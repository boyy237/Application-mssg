
// Pool de connexions PostgreSQL partagé par toute l'application.
const { Pool } = require('pg');
const config = require('./config');

const pool = process.env.DATABASE_URL
  ? new Pool({
      connectionString: process.env.DATABASE_URL,
      ssl: { rejectUnauthorized: false }
    })
  : new Pool({
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
