const express = require('express');
const http = require('http');
const cors = require('cors');
const WebSocket = require('ws');

const RoomManager = require('./RoomManager');
const gameRoutes = require('./routes/gameRoutes');
const gameSocket = require('./sockets/gameSocket');

const app = express();
const server = http.createServer(app);
const roomManager = new RoomManager();

app.use(cors());
app.use(express.json());
app.set('roomManager', roomManager);

// Routes REST (consultation des salons)
app.use('/', gameRoutes);

// Jeu en temps réel (WebSocket, même port que l'API REST)
const wss = new WebSocket.Server({ server });
gameSocket(wss, roomManager);

// Les hébergeurs (Render, Railway, Fly...) fournissent le port via PORT.
const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
  console.log(`🚀 Serveur 6Takes lancé sur http://localhost:${PORT} (WebSocket : ws://localhost:${PORT})`);
});
