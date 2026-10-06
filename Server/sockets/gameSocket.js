// sockets/gameSocket.js
// Gestion des connexions WebSocket : routage des messages vers les salons.

let nextId = 1;

module.exports = (wss, roomManager) => {
  wss.on('connection', (ws) => {
    const peer = {
      id: `p${nextId++}`,
      send: (text) => {
        if (ws.readyState === ws.OPEN) ws.send(text);
      },
    };
    let room = null;
    ws.isAlive = true;
    ws.on('pong', () => { ws.isAlive = true; });
    console.log(`✅ Client connecté (${peer.id})`);

    ws.on('message', (data) => {
      let msg;
      try {
        msg = JSON.parse(data.toString());
      } catch {
        return peer.send(JSON.stringify({ type: 'error', message: 'Message invalide' }));
      }

      switch (msg.type) {
        case 'rooms':
          return peer.send(JSON.stringify({ type: 'rooms', rooms: roomManager.getOpenRooms() }));

        case 'create':
          if (room) room.disconnected(peer);
          room = roomManager.createRoom();
          return room.handle(peer, msg);

        case 'join': {
          const target = roomManager.getRoom(msg.roomId);
          if (!target) {
            return peer.send(JSON.stringify({ type: 'error', message: `Salon ${msg.roomId} introuvable` }));
          }
          if (room && room !== target) room.disconnected(peer);
          room = target;
          return room.handle(peer, msg);
        }

        default:
          if (!room) {
            return peer.send(JSON.stringify({ type: 'error', message: "Vous n'êtes dans aucun salon" }));
          }
          room.handle(peer, msg);
          if (msg.type === 'leave') room = null;
      }
    });

    ws.on('close', () => {
      console.log(`🔌 Client déconnecté (${peer.id})`);
      if (room) room.disconnected(peer);
      room = null;
    });

    ws.on('error', (err) => {
      console.error('⚠️ Erreur WebSocket :', err.message);
    });
  });

  // Ping régulier : détecte les clients disparus et garde les connexions
  // ouvertes derrière les proxys des hébergeurs.
  const interval = setInterval(() => {
    for (const ws of wss.clients) {
      if (!ws.isAlive) {
        ws.terminate();
        continue;
      }
      ws.isAlive = false;
      ws.ping();
    }
  }, 30000);
  wss.on('close', () => clearInterval(interval));
};
