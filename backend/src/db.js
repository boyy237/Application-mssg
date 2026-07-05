const { Pool } = require('pg');
const config = require('./config');

// Pool de connexions PostgreSQL partagé par toute l'application.
const pool = new Pool({
  host: config.db.host,
  port: config.db.port,
  database: config.db.database,
  user: config.db.user,
  password: config.db.password,
});

pool.on('error', (err) => {
  console.error('Erreur inattendue sur le pool PostgreSQL :', err);
});

async function query(text, params) {
  return pool.query(text, params);
}

module.exports = { pool, query };
