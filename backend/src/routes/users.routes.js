const express = require('express');
const router = express.Router();
const usersController = require('../controllers/users.controller');
const { requireAuth } = require('../middleware/auth.middleware');

router.get('/search', requireAuth, usersController.search);

module.exports = router;
