const { query } = require('../db');
const { publicUser } = require('./auth.controller');

// Recherche d'utilisateurs par nom d'utilisateur, pour démarrer une nouvelle conversation.
async function search(req, res, next) {
  try {
    const q = (req.query.q || '').trim();
    if (q.length < 1) return res.json({ users: [] });

    const result = await query(
      `SELECT * FROM users
       WHERE username ILIKE $1 AND id != $2
       ORDER BY username ASC
       LIMIT 20`,
      [`%${q}%`, req.user.id]
    );
    return res.json({ users: result.rows.map(publicUser) });
  } catch (err) {
    return next(err);
  }
}

module.exports = { search };
