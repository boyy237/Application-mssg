/**
 * Route pour enregistrer/mettre à jour le token FCM d'un utilisateur.
 * L'app Android appelle cette route au démarrage pour que le serveur
 * sache où envoyer les notifications de cet utilisateur.
 */

const express = require('express');
const router = express.Router();
const { requireAuth } = require('../middleware/auth.middleware');
const { query } = require('../db');

// POST /api/fcm/token
// Body : { token: "eFcM_ToKeN_iCi..." }
router.post('/token', requireAuth, async (req, res, next) => {
  try {
    const { token } = req.body;
    if (!token) {
      return res.status(400).json({ error: 'token FCM requis.' });
    }

    await query(
      'UPDATE users SET fcm_token = $1 WHERE id = $2',
      [token, req.user.id]
    );

    return res.json({ ok: true });
  } catch (err) {
    return next(err);
  }
});

// DELETE /api/fcm/token  (appelé à la déconnexion)
router.delete('/token', requireAuth, async (req, res, next) => {
  try {
    await query(
      'UPDATE users SET fcm_token = NULL WHERE id = $1',
      [req.user.id]
    );
    return res.json({ ok: true });
  } catch (err) {
    return next(err);
  }
});

module.exports = router;
