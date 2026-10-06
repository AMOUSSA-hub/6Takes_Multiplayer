const { GameRoom } = require('./game/GameRoom');

// Lettres sans ambiguïté (pas de I, O, 0, 1) pour des codes faciles à dicter.
const CODE_CHARS = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';

class RoomManager {
  constructor() {
    this.rooms = new Map();
  }

  generateCode() {
    let code;
    do {
      code = Array.from({ length: 4 }, () => CODE_CHARS[Math.floor(Math.random() * CODE_CHARS.length)]).join('');
    } while (this.rooms.has(code));
    return code;
  }

  createRoom() {
    const code = this.generateCode();
    const room = new GameRoom(code, () => {
      this.rooms.delete(code);
      console.log(`🗑️  Salon ${code} supprimé (vide)`);
    });
    this.rooms.set(code, room);
    console.log(`🎲 Salon ${code} créé`);
    return room;
  }

  getRoom(roomId) {
    return roomId ? this.rooms.get(String(roomId).toUpperCase()) : undefined;
  }

  /** Salons en attente de joueurs. */
  getOpenRooms() {
    return [...this.rooms.values()].filter(r => r.isOpen()).map(r => r.summary());
  }

  getAllRooms() {
    return [...this.rooms.values()].map(r => r.summary());
  }
}

module.exports = RoomManager;
