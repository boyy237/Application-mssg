const { query } = require('../db');

function toConversationDto(row) {
  return {
    id: row.id,
    isGroup: row.is_group,
    name: row.name,
    cipherA: row.cipher_a,
    cipherB: row.cipher_b,
    createdAt: row.created_at,
    lastReadMessageId: row.last_read_message_id,
  };
}

async function assertParticipant(conversationId, userId) {
  const result = await query(
    'SELECT 1 FROM conversation_participants WHERE conversation_id = $1 AND user_id = $2',
    [conversationId, userId]
  );

  return result.rowCount > 0;
}

async function list(req, res, next) {
  try {
    const result = await query(
      `SELECT c.id, c.is_group, c.name, c.cipher_a, c.cipher_b, c.created_at, cp.last_read_message_id
       FROM conversations c
       JOIN conversation_participants cp ON cp.conversation_id = c.id
       WHERE cp.user_id = $1
       ORDER BY c.created_at DESC`,
      [req.user.id]
    );

    return res.json({ conversations: result.rows.map(toConversationDto) });
  } catch (err) {
    return next(err);
  }
}

async function createOrGet(req, res, next) {
  try {
    const explicitUserId = req.body?.userId ?? req.body?.participantId;
    const participantIds = Array.isArray(req.body?.participantIds)
      ? req.body.participantIds
      : explicitUserId
        ? [explicitUserId]
        : [];

    const targetUserIds = participantIds
      .map((id) => Number(id))
      .filter((id) => Number.isInteger(id) && id > 0 && id !== Number(req.user.id));

    if (targetUserIds.length === 0) {
      return res.status(400).json({ error: 'Au moins un autre participant est requis.' });
    }

    const isGroup = targetUserIds.length > 1;

    if (!isGroup) {
      const existing = await query(
        `SELECT c.id, c.is_group, c.name, c.cipher_a, c.cipher_b, c.created_at
         FROM conversations c
         JOIN conversation_participants cp1 ON cp1.conversation_id = c.id AND cp1.user_id = $1
         JOIN conversation_participants cp2 ON cp2.conversation_id = c.id AND cp2.user_id = $2
         WHERE c.is_group = FALSE
         LIMIT 1`,
        [req.user.id, targetUserIds[0]]
      );

      if (existing.rows[0]) {
        return res.json({ conversation: toConversationDto(existing.rows[0]) });
      }
    }

    const conversationResult = await query(
      `INSERT INTO conversations (is_group, name)
       VALUES ($1, $2)
       RETURNING id, is_group, name, cipher_a, cipher_b, created_at`,
      [isGroup, isGroup ? null : null]
    );

    const conversation = conversationResult.rows[0];
    const participantUserIds = [req.user.id, ...targetUserIds];

    for (const userId of participantUserIds) {
      await query(
        `INSERT INTO conversation_participants (conversation_id, user_id)
         VALUES ($1, $2)
         ON CONFLICT (conversation_id, user_id) DO NOTHING`,
        [conversation.id, userId]
      );
    }

    return res.status(201).json({ conversation: toConversationDto(conversation) });
  } catch (err) {
    return next(err);
  }
}

async function getById(req, res, next) {
  try {
    const conversationId = parseInt(req.params.id, 10);
    if (!(await assertParticipant(conversationId, req.user.id))) {
      return res.status(403).json({ error: 'Vous ne participez pas à cette conversation.' });
    }

    const result = await query(
      `SELECT c.id, c.is_group, c.name, c.cipher_a, c.cipher_b, c.created_at, cp.last_read_message_id
       FROM conversations c
       JOIN conversation_participants cp ON cp.conversation_id = c.id AND cp.user_id = $2
       WHERE c.id = $1`,
      [conversationId, req.user.id]
    );

    if (!result.rows[0]) {
      return res.status(404).json({ error: 'Conversation introuvable.' });
    }

    return res.json({ conversation: toConversationDto(result.rows[0]) });
  } catch (err) {
    return next(err);
  }
}

async function updateKey(req, res, next) {
  try {
    const conversationId = parseInt(req.params.id, 10);
    const { cipherA, cipherB } = req.body;

    if (!(await assertParticipant(conversationId, req.user.id))) {
      return res.status(403).json({ error: 'Vous ne participez pas à cette conversation.' });
    }

    await query(
      'UPDATE conversations SET cipher_a = $2, cipher_b = $3 WHERE id = $1',
      [conversationId, cipherA, cipherB]
    );

    return res.json({ ok: true });
  } catch (err) {
    return next(err);
  }
}

async function markRead(req, res, next) {
  try {
    const conversationId = parseInt(req.params.id, 10);
    const { messageId } = req.body;

    if (!(await assertParticipant(conversationId, req.user.id))) {
      return res.status(403).json({ error: 'Vous ne participez pas à cette conversation.' });
    }

    const result = await query(
      'UPDATE conversation_participants SET last_read_message_id = $3 WHERE conversation_id = $1 AND user_id = $2',
      [conversationId, req.user.id, messageId]
    );

    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'Conversation introuvable.' });
    }

    return res.json({ ok: true });
  } catch (err) {
    return next(err);
  }
}

module.exports = { list, createOrGet, getById, updateKey, markRead, assertParticipant };
