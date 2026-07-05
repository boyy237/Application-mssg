const express = require('express');
const router = express.Router();
const conversationsController = require('../controllers/conversations.controller');
const messagesController = require('../controllers/messages.controller');
const { requireAuth } = require('../middleware/auth.middleware');

router.use(requireAuth);

router.get('/', conversationsController.list);
router.post('/', conversationsController.createOrGet);
router.get('/:id', conversationsController.getById);
router.patch('/:id/key', conversationsController.updateKey);
router.post('/:id/read', conversationsController.markRead);

router.get('/:id/messages', messagesController.getHistory);
router.post('/:id/messages', messagesController.create);
router.delete('/:id/messages/:messageId', messagesController.remove);

module.exports = router;
