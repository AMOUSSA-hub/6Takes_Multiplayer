// controllers/gameController.js

function health(req, res) {
  res.json({ name: '6Takes server', status: 'ok' });
}

function getGame(req, res) {
  const room = req.app.get('roomManager').getRoom(req.params.roomId);
  if (!room) return res.status(404).json({ message: 'Not found' });
  res.json(room.summary());
}

function getOpenRooms(req, res) {
  res.json(req.app.get('roomManager').getOpenRooms());
}

module.exports = { health, getGame, getOpenRooms };
