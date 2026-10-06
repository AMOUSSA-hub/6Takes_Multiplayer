// routes/gameRoutes.js
const express = require('express');
const router = express.Router();
const { health, getGame, getOpenRooms } = require('../controllers/gameController');

router.get('/', health);
router.get('/room/:roomId', getGame);
router.get('/rooms', getOpenRooms);

module.exports = router;
