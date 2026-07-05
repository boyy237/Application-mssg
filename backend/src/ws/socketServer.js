const { WebSocketServer } = require('ws');
const url = require('url');
const { verifyToken } = require('../utils/jwt');
const { query } = require('../db');

// userId (number) -> Set<WebSocket>   (un utilisateur peut être connecté depuis
// plusieurs appareils en même temps : téléphone + tablette par exemple)
const connections = new Map();

function addConnection(userId, socket) {
  if (!connections.has(userId)) connections.set(userId, new Set());
  connections.get(userId).add(socket);
}

function removeConnection(userId, socket) {
  const set = connections.get(userId);
  if (!set) return;
  set.delete(socket);
  if (set.size === 0) connections.delete(userId);
}

function sendToUser(userId, payload) {
  const set = connections.get(userId);
  if (!set) return;
  const data = JSON.stringify(payload);
  for (const socket of set) {
    if (socket.readyState === socket.OPEN) socket.send(data);
  }
}

// Envoie un événement à tous les participants d'une conversation donnée
// (y compris l'auteur, pour synchroniser ses autres appareils éventuels).
async function broadcastToConversation(conversationId, actorUserId, payload) {
  try {
    const result = await query(
      'SELECT user_id FROM conversation_participants WHERE conversation_id = $1',
      [conversationId]
    );
    for (const row of result.rows) {
      sendToUser(row.user_id, payload);
    }
  } catch (err) {
    console.error('Erreur de diffusion WebSocket :', err);
  }
}

async function getConversationIdsForUser(userId) {
  const result = await query(
    'SELECT conversation_id FROM conversation_participants WHERE user_id = $1',
    [userId]
  );
  return result.rows.map((r) => r.conversation_id);
}

async function setOnlineStatus(userId, isOnline) {
  await query('UPDATE users SET is_online = $1, last_seen = NOW() WHERE id = $2', [isOnline, userId]);
  const conversationIds = await getConversationIdsForUser(userId);
  for (const conversationId of conversationIds) {
    broadcastToConversation(conversationId, userId, {
      type: 'presence',
      userId,
      isOnline,
    });
  }
}

function attachWebSocketServer(httpServer) {
  const wss = new WebSocketServer({ server: httpServer, path: '/ws' });

  wss.on('connection', (socket, req) => {
    const { query: qs } = url.parse(req.url, true);
    const token = qs.token;

    let userId;
    try {
      const payload = verifyToken(token);
      userId = payload.sub;
    } catch (err) {
      socket.close(4001, 'Token invalide');
      return;
    }

    addConnection(userId, socket);
    setOnlineStatus(userId, true);

    socket.on('message', async (raw) => {
      let data;
      try {
        data = JSON.parse(raw.toString());
      } catch (err) {
        return;
      }

      // Relaie un indicateur "est en train d'écrire..." aux autres participants.
      if (data.type === 'typing' && data.conversationId) {
        broadcastToConversation(data.conversationId, userId, {
          type: 'typing',
          conversationId: data.conversationId,
          userId,
          isTyping: !!data.isTyping,
        });
      }
    });

    socket.on('close', () => {
      removeConnection(userId, socket);
      if (!connections.has(userId)) {
        setOnlineStatus(userId, false);
      }
    });

    socket.send(JSON.stringify({ type: 'connected', userId }));
  });

  return wss;
}

module.exports = { attachWebSocketServer, broadcastToConversation, sendToUser };
