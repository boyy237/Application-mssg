const { query } = require('../db');
const { hashPassword, comparePassword } = require('../utils/password');
const { signToken } = require('../utils/jwt');

const PALETTE = ['#4af0c4', '#a78bfa', '#f06060', '#f0c14a', '#60a5fa', '#f0609e'];

function publicUser(row) {
  return {
    id: row.id,
    username: row.username,
    email: row.email,
    avatarColor: row.avatar_color,
    isOnline: row.is_online,
    lastSeen: row.last_seen,
  };
}

async function register(req, res, next) {
  try {
    const { username, email, password } = req.body;

    if (!username || !email || !password) {
      return res.status(400).json({ error: 'username, email et password sont requis.' });
    }
    if (username.length < 3) {
      return res.status(400).json({ error: "Le nom d'utilisateur doit contenir au moins 3 caractères." });
    }
    if (password.length < 6) {
      return res.status(400).json({ error: 'Le mot de passe doit contenir au moins 6 caractères.' });
    }

    const existing = await query(
      'SELECT id FROM users WHERE username = $1 OR email = $2',
      [username, email]
    );
    if (existing.rows.length > 0) {
      return res.status(409).json({ error: "Ce nom d'utilisateur ou cet email est déjà utilisé." });
    }

    const passwordHash = await hashPassword(password);
    const avatarColor = PALETTE[Math.floor(Math.random() * PALETTE.length)];

    const result = await query(
      `INSERT INTO users (username, email, password_hash, avatar_color)
       VALUES ($1, $2, $3, $4) RETURNING *`,
      [username, email, passwordHash, avatarColor]
    );

    const user = result.rows[0];
    const token = signToken(user);
    return res.status(201).json({ token, user: publicUser(user) });
  } catch (err) {
    return next(err);
  }
}

async function login(req, res, next) {
  try {
    const { username, password } = req.body;
    if (!username || !password) {
      return res.status(400).json({ error: 'username et password sont requis.' });
    }

    const result = await query(
      'SELECT * FROM users WHERE username = $1 OR email = $1',
      [username]
    );
    const user = result.rows[0];
    if (!user) {
      return res.status(401).json({ error: 'Identifiants invalides.' });
    }

    const ok = await comparePassword(password, user.password_hash);
    if (!ok) {
      return res.status(401).json({ error: 'Identifiants invalides.' });
    }

    await query('UPDATE users SET is_online = TRUE, last_seen = NOW() WHERE id = $1', [user.id]);

    const token = signToken(user);
    return res.json({ token, user: publicUser(user) });
  } catch (err) {
    return next(err);
  }
}

async function me(req, res, next) {
  try {
    const result = await query('SELECT * FROM users WHERE id = $1', [req.user.id]);
    if (!result.rows[0]) return res.status(404).json({ error: 'Utilisateur introuvable.' });
    return res.json({ user: publicUser(result.rows[0]) });
  } catch (err) {
    return next(err);
  }
}

module.exports = { register, login, me, publicUser };
