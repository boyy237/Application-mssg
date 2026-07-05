require('dotenv').config();

module.exports = {
  port: process.env.PORT || 4000,
  db: {
    host: process.env.PGHOST || 'localhost',
    port: parseInt(process.env.PGPORT || '5432', 10),
    database: process.env.PGDATABASE || 'ciphertalk',
    user: process.env.PGUSER || 'postgres',
    password: process.env.PGPASSWORD || 'perovc',
  },
  jwt: {
    secret: process.env.JWT_SECRET || 'dev-secret-change-me',
    expiresIn: process.env.JWT_EXPIRES_IN || '1y',
  },
  defaultCipher: {
    a: parseInt(process.env.DEFAULT_CIPHER_A || '7', 10),
    b: parseInt(process.env.DEFAULT_CIPHER_B || '3', 10),
  },
};
