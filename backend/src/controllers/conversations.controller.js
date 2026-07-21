const { query } = require('../db');
const config = require('../config');
const { isValidKey } = require('../utils/affineCipher');
const { publicUser } = require('./auth.controller');
const { broadcastToConversation } = require('../ws/socketServer');

// Vérifie que l'utilisateur courant participe bien à la conversation demandée.
async function assertParticipant(conversationId, userId) {
  const result = await query(
    'SELECT 1 FROM conversation_participants WHERE conversation_id = $1 AND user_id = $2',
    [conversationId, userId]
  );
  return result.rows.length > 0;
}

async function getOtherParticipants(conversationId, userId) {
  const result = await query(
    `SELECT u.* FROM users u
     JOIN conversation_participants cp ON cp.user_id = u.id
     WHERE cp.conversation_id = $1 AND u.id != $2`,
    [conversationId, userId]
  );
  return result.rows.map(publicUser);
}

// Liste des conversations de l'utilisateur courant, avec aperçu du dernier message
// (texte chiffré + clé) et compteur de messages non lus.
async function list(req, res, next) {
  try {
    const convResult = await query(
      `SELECT c.id, c.is_group, c.name, c.cipher_a, c.cipher_b, cp.last_read_message_id
       FROM conversations c
       JOIN conversation_participants cp ON cp.conversation_id = c.id
       WHERE cp.user_id = $1`,
      [req.user.id]
    );

    const conversations = [];
    for (const conv of convResult.rows) {
      const lastMsgResult = await query(
        `SELECT id, sender_id, cipher_text, cipher_a, cipher_b, created_at FROM messages
         WHERE conversation_id = $1 AND is_deleted = FALSE
         ORDER BY created_at DESC LIMIT 1`,
        [conv.id]
      );
      const lastMessage = lastMsgResult.rows[0] || null;

      const unreadResult = await query(
        `SELECT COUNT(*)::int AS count FROM messages
         WHERE conversation_id = $1 AND sender_id != $2 AND is_deleted = FALSE
           AND ($3::int IS NULL OR id > $3::int)`,
        [conv.id, req.user.id, conv.last_read_message_id]
      );

      const others = await getOtherParticipants(conv.id, req.user.id);

      conversations.push({
        id: conv.id,
        isGroup: conv.is_group,
        name: conv.name,
        cipherA: conv.cipher_a,
        cipherB: conv.cipher_b,
        participants: others,
        lastMessage: lastMessage && {
          id: lastMessage.id,
          senderId: lastMessage.sender_id,
          cipherText: lastMessage.cipher_text,
          cipherA: lastMessage.cipher_a,
          cipherB: lastMessage.cipher_b,
          createdAt: lastMessage.created_at,
        },
        unreadCount: unreadResult.rows[0].count,
      });
    }

    conversations.sort((a, b) => {
      const ta = a.lastMessage ? new Date(a.lastMessage.createdAt).getTime() : 0;
      const tb = b.lastMessage ? new Date(b.lastMessage.createdAt).getTime() : 0;
      return tb - ta;
    });

    return res.json({ conversations });
  } catch (err) {
    return next(err);
  }
}

// Crée une conversation 1-à-1 avec "participantId", ou renvoie celle qui existe déjà.
async function createOrGet(req, res, next) {
  try {
    const { participantId } = req.body;
    if (!participantId || participantId === req.user.id) {
      return res.status(400).json({ error: 'participantId valide requis.' });
    }

    const targetExists = await query('SELECT id FROM users WHERE id = $1', [participantId]);
    if (!targetExists.rows[0]) {
      return res.status(404).json({ error: 'Utilisateur introuvable.' });
    }

    const existing = await query(
      `SELECT c.id FROM conversations c
       JOIN conversation_participants p1 ON p1.conversation_id = c.id AND p1.user_id = $1
       JOIN conversation_participants p2 ON p2.conversation_id = c.id AND p2.user_id = $2
       WHERE c.is_group = FALSE
       LIMIT 1`,
      [req.user.id, participantId]
    );

    let conversationId;
    if (existing.rows[0]) {
      conversationId = existing.rows[0].id;
    } else {
      const created = await query(
        `INSERT INTO conversations (is_group, cipher_a, cipher_b) VALUES (FALSE, $1, $2) RETURNING id`,
        [config.defaultCipher.a, config.defaultCipher.b]
      );
      conversationId = created.rows[0].id;
      await query(
        `INSERT INTO conversation_participants (conversation_id, user_id) VALUES ($1, $2), ($1, $3)`,
        [conversationId, req.user.id, participantId]
      );
    }

    const convResult = await query('SELECT * FROM conversations WHERE id = $1', [conversationId]);
    const others = await getOtherParticipants(conversationId, req.user.id);

    return res.status(201).json({
      conversation: {
        id: convResult.rows[0].id,
        isGroup: convResult.rows[0].is_group,
        cipherA: convResult.rows[0].cipher_a,
        cipherB: convResult.rows[0].cipher_b,
        participants: others,
      },
    });
  } catch (err) {
    return next(err);
  }
}

async function getById(req, res, next) {
  try {
    const conversationId = parseInt(req.params.id, 10);
    if (!(await assertParticipant(conversationId, req.user.id))) {
      return res.status(403).json({ error: "Vous ne participez pas à cette conversation." });
    }

    const convResult = await query('SELECT * FROM conversations WHERE id = $1', [conversationId]);
    if (!convResult.rows[0]) return res.status(404).json({ error: 'Conversation introuvable.' });

    const others = await getOtherParticipants(conversationId, req.user.id);

    return res.json({
      conversation: {
        id: convResult.rows[0].id,
        isGroup: convResult.rows[0].is_group,
        name: convResult.rows[0].name,
        cipherA: convResult.rows[0].cipher_a,
        cipherB: convResult.rows[0].cipher_b,
        participants: others,
      },
    });
  } catch (err) {
    return next(err);
  }
}

// Met à jour la clé partagée (a, b) du chiffrement affine pour cette conversation.
async function updateKey(req, res, next) {
  try {
    const conversationId = parseInt(req.params.id, 10);
    if (!(await assertParticipant(conversationId, req.user.id))) {
      return res.status(403).json({ error: "Vous ne participez pas à cette conversation." });
    }

    const a = parseInt(req.body.a, 10);
    const b = parseInt(req.body.b, 10);
    if (!isValidKey(a, b)) {
      return res.status(400).json({ error: 'Clé invalide : pgcd(a, 26) doit valoir 1 et b doit être dans [0, 25].' });
    }

    await query('UPDATE conversations SET cipher_a = $1, cipher_b = $2 WHERE id = $3', [a, b, conversationId]);

    broadcastToConversation(conversationId, req.user.id, {
      type: 'key:updated',
      conversationId,
      cipherA: a,
      cipherB: b,
    });

    return res.json({ cipherA: a, cipherB: b });
  } catch (err) {
    return next(err);
  }
}

// Marque la conversation comme lue jusqu'au dernier message reçu.
async function markRead(req, res, next) {
  try {
    const conversationId = parseInt(req.params.id, 10);
    if (!(await assertParticipant(conversationId, req.user.id))) {
      return res.status(403).json({ error: "Vous ne participez pas à cette conversation." });
    }

    const lastMsg = await query(
      `SELECT id FROM messages WHERE conversation_id = $1 ORDER BY created_at DESC LIMIT 1`,
      [conversationId]
    );
    const lastId = lastMsg.rows[0] ? lastMsg.rows[0].id : null;

    if (lastId !== null) {
      await query(
        `UPDATE conversation_participants SET last_read_message_id = $1
         WHERE conversation_id = $2 AND user_id = $3`,
        [lastId, conversationId, req.user.id]
      );
      broadcastToConversation(conversationId, req.user.id, {
        type: 'message:read',
        conversationId,
        readerId: req.user.id,
        upToMessageId: lastId,
      });
    }

    return res.json({ ok: true });
  } catch (err) {
    return next(err);
  }
}

module.exports = { list, createOrGet, getById, updateKey, markRead, assertParticipant };
