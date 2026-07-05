const { verifyToken } = require('../utils/jwt');

/**
 * Vérifie le JWT présent dans l'en-tête "Authorization: Bearer <token>"
 * et attache l'utilisateur authentifié (req.user = { id, username }) à la requête.
 */
function requireAuth(req, res, next) {
  const header = req.headers.authorization || '';
  const [scheme, token] = header.split(' ');

  if (scheme !== 'Bearer' || !token) {
    return res.status(401).json({ error: 'Authentification requise.' });
  }

  try {
    const payload = verifyToken(token);
    req.user = { id: payload.sub, username: payload.username };
    return next();
  } catch (err) {
    return res.status(401).json({ error: 'Token invalide ou expiré.' });
  }
}

module.exports = { requireAuth };
