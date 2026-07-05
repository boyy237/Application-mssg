const http = require('http');
const express = require('express');
const cors = require('cors');
const morgan = require('morgan');

const config = require('./config');
const { pool } = require('./db');
const routes = require('./routes');
const { notFound, errorHandler } = require('./middleware/error.middleware');
const { attachWebSocketServer } = require('./ws/socketServer');

const app = express();

app.use(cors());
app.use(express.json({ limit: '2mb' }));
app.use(morgan('dev'));

app.get('/api/health', async (req, res) => {
  try {
    await pool.query('SELECT 1');
    res.json({ status: 'ok', db: 'connected' });
  } catch (err) {
    res.status(500).json({ status: 'error', db: 'unreachable', error: err.message });
  }
});

app.use('/api', routes);

app.use(notFound);
app.use(errorHandler);

const server = http.createServer(app);

// Attache le serveur WebSocket (présence + diffusion temps réel des messages)
attachWebSocketServer(server);

server.listen(config.port, () => {
  console.log(`🔐 CipherTalk backend démarré sur le port ${config.port}`);
  console.log(`   API REST  : http://localhost:${config.port}/api`);
  console.log(`   WebSocket : ws://localhost:${config.port}/ws?token=VOTRE_JWT`);
});
