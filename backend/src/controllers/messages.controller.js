const { query } = require('../db');
const { assertParticipant } = require('./conversations.controller');
const { broadcastToConversation } = require('../ws/socketServer');
const { sendPushNotification } = require('../utils/fcm');

function toDto(row) {
  return {
    id: row.id,
    conversationId: row.conversation_id,
    senderId: row.sender_id,
    cipherText: row.cipher_text,
    cipherA: row.cipher_a,
    cipherB: row.cipher_b,
    isDeleted: row.is_deleted,
    createdAt: row.created_at,
  };
}

async function getHistory(req, res, next) {
  try {
    const conversationId = parseInt(req.params.id, 10);
    if (!(await assertParticipant(conversationId, req.user.id))) {
      return res.status(403).json({ error: 'Vous ne participez pas à cette conversation.' });
    }

    const limit = Math.min(parseInt(req.query.limit, 10) || 50, 100);
    const before = req.query.before ? parseInt(req.query.before, 10) : null;

    const result = await query(
      `SELECT * FROM messages
       WHERE conversation_id = $1 AND ($2::int IS NULL OR id < $2::int)
       ORDER BY id DESC LIMIT $3`,
      [conversationId, before, limit]
    );

    return res.json({ messages: result.rows.map(toDto).reverse() });
  } catch (err) {
    return next(err);
  }
}

async function create(req, res, next) {
  try {
    const conversationId = parseInt(req.params.id, 10);
    if (!(await assertParticipant(conversationId, req.user.id))) {
      return res.status(403).json({ error: 'Vous ne participez pas à cette conversation.' });
    }

    const { cipherText } = req.body;
    if (!cipherText || !cipherText.trim()) {
      return res.status(400).json({ error: 'cipherText est requis.' });
    }

    const convResult = await query(
      'SELECT cipher_a, cipher_b FROM conversations WHERE id = $1',
      [conversationId]
    );
    if (!convResult.rows[0]) {
      return res.status(404).json({ error: 'Conversation introuvable.' });
    }
    const { cipher_a: cipherA, cipher_b: cipherB } = convResult.rows[0];

    const result = await query(
      `INSERT INTO messages (conversation_id, sender_id, cipher_text, cipher_a, cipher_b)
       VALUES ($1, $2, $3, $4, $5) RETURNING *`,
      [conversationId, req.user.id, cipherText, cipherA, cipherB]
    );

    const message = toDto(result.rows[0]);

    broadcastToConversation(conversationId, req.user.id, {
      type: 'message:new',
      message,
    });

    const senderResult = await query(
      'SELECT username FROM users WHERE id = $1',
      [req.user.id]
    );
    const senderName = senderResult.rows[0]?.username || 'Quelqu\'un';

    const participantsResult = await query(
      `SELECT u.id, u.fcm_token
       FROM users u
       JOIN conversation_participants cp ON cp.user_id = u.id
       WHERE cp.conversation_id = $1 AND u.id != $2`,
      [conversationId, req.user.id]
    );

    for (const participant of participantsResult.rows) {
      if (participant.fcm_token) {
        await sendPushNotification(
          participant.fcm_token,
          senderName,
          'Vous avez reçu un nouveau message',
          {
            conversationId: conversationId.toString(),
            senderId: req.user.id.toString(),
            senderName,
          }
        );
      }
    }

    return res.status(201).json({ message });
  } catch (err) {
    return next(err);
  }
}

async function remove(req, res, next) {
  try {
    const messageId = parseInt(req.params.messageId, 10);
    const msgResult = await query('SELECT * FROM messages WHERE id = $1', [messageId]);
    const message = msgResult.rows[0];

    if (!message) return res.status(404).json({ error: 'Message introuvable.' });
    if (message.sender_id !== req.user.id) {
      return res.status(403).json({ error: "Vous ne pouvez supprimer que vos propres messages." });
    }

    await query('UPDATE messages SET is_deleted = TRUE WHERE id = $1', [messageId]);

    broadcastToConversation(message.conversation_id, req.user.id, {
      type: 'message:deleted',
      conversationId: message.conversation_id,
      messageId,
    });

    return res.json({ ok: true });
  } catch (err) {
    return next(err);
  }
}

module.exports = { getHistory, create, remove };
