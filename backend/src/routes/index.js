
/**
 * routes/index.js — VERSION MISE À JOUR avec route FCM
 
 */

const express = require('express');
const router = express.Router();

router.use('/auth', require('./auth.routes'));
router.use('/users', require('./users.routes'));
router.use('/conversations', require('./conversations.routes'));
router.use('/fcm', require('./fcm.routes'));

module.exports = router;

