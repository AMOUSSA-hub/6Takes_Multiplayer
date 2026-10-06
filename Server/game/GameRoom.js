// game/GameRoom.js
// Salon de jeu faisant autorité. Mêmes règles et même protocole que le serveur LAN Java
// (Client/.../Multijoueur/Server/GameRoom.java).

const MAX_PLAYERS = 10;
const TOTAL_ROUNDS = 10;
const TURN_SECONDS = 30;
const CHOOSE_ROW_SECONDS = 15;
const REVEAL_DELAY_MS = 2000;
const PLACE_DELAY_MS = 1200;

/** Nombre de têtes de boeuf d'une carte. */
function malus(v) {
  if (v % 11 === 0) return v === 55 ? 7 : 5;
  if (v % 10 === 0) return 3;
  if (v % 5 === 0) return 2;
  return 1;
}

function shuffle(arr) {
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [arr[i], arr[j]] = [arr[j], arr[i]];
  }
  return arr;
}

class GameRoom {
  /**
   * @param {string} roomId
   * @param {() => void} onEmpty appelé quand plus aucun joueur n'est connecté
   */
  constructor(roomId, onEmpty = () => {}) {
    this.roomId = roomId;
    this.onEmpty = onEmpty;
    this.players = []; // { id, name, peer, hand, score, chosen, connected }
    this.rows = [];
    this.pending = []; // { player, card }
    this.hostId = null;
    this.phase = 'lobby';
    this.round = 0;
    this.chooserId = null;
    this.deadline = 0;
    this.timer = null;
    this.placeTimer = null;
    this.log = '';
  }

  // ===================== Entrées =====================

  /** Traiter un message (déjà décodé) d'un client. peer = { id, send(str) } */
  handle(peer, msg) {
    switch (msg.type) {
      case 'create':
      case 'join':
        return this.join(peer, msg.name);
      case 'start':
        return this.start(peer);
      case 'play':
        return this.play(peer, Number(msg.card));
      case 'chooseRow':
        return this.chooseRow(peer, Number(msg.row));
      case 'leave':
        return this.leave(peer);
      default:
        return this.error(peer, `Message inconnu : ${msg.type}`);
    }
  }

  disconnected(peer) {
    this.leave(peer);
  }

  isOpen() {
    return this.phase === 'lobby' && this.players.length < MAX_PLAYERS;
  }

  summary() {
    const host = this.players.find(p => p.id === this.hostId);
    return {
      id: this.roomId,
      host: host ? host.name : '',
      players: this.players.length,
      phase: this.phase,
    };
  }

  shutdown() {
    this.cancelTimer();
    clearTimeout(this.placeTimer);
  }

  // ===================== Logique =====================

  join(peer, name) {
    if (this.find(peer)) return;
    if (this.phase !== 'lobby') return this.error(peer, 'La partie a déjà commencé');
    if (this.players.length >= MAX_PLAYERS) {
      return this.error(peer, `Le salon est plein (${MAX_PLAYERS} joueurs max)`);
    }
    name = typeof name === 'string' && name.trim() ? name.trim().slice(0, 20) : `Joueur ${this.players.length + 1}`;
    const p = { id: peer.id, name, peer, hand: [], score: 0, chosen: null, connected: true };
    this.players.push(p);
    if (!this.hostId) this.hostId = p.id;
    this.send(p, { type: 'joined', roomId: this.roomId, playerId: p.id });
    this.broadcastLobby();
  }

  leave(peer) {
    const p = this.find(peer);
    if (!p) return;

    if (this.phase === 'lobby') {
      this.players = this.players.filter(x => x !== p);
      if (p.id === this.hostId) this.hostId = this.players.length ? this.players[0].id : null;
      this.broadcastLobby();
      this.checkEmpty();
      return;
    }

    // En partie : le joueur est remplacé par un automate.
    p.connected = false;
    p.peer = null;
    if (p.id === this.hostId) {
      const next = this.players.find(x => x.connected);
      this.hostId = next ? next.id : null;
    }
    this.log = `${p.name} s'est déconnecté (joué automatiquement)`;
    if (this.checkEmpty()) return;

    if (this.phase === 'choosing' && p.chosen === null) {
      p.chosen = this.randomCard(p);
      this.checkAllPlayed();
      return;
    }
    if (this.phase === 'chooseRow' && p.id === this.chooserId) {
      this.applyRowChoice(this.minMalusRow());
      return;
    }
    this.broadcastState();
  }

  checkEmpty() {
    if (!this.players.some(p => p.connected)) {
      this.shutdown();
      this.onEmpty();
      return true;
    }
    return false;
  }

  start(peer) {
    const p = this.find(peer);
    if (!p || p.id !== this.hostId) return this.error(peer, "Seul l'hôte peut lancer la partie");
    if (this.phase !== 'lobby' && this.phase !== 'end') return;

    this.players = this.players.filter(x => x.connected);
    if (this.players.length < 2) {
      this.phase = 'lobby';
      this.error(peer, 'Il faut au moins 2 joueurs');
      this.broadcastLobby();
      return;
    }
    this.deal();
    this.round = 1;
    this.log = 'La partie commence !';
    this.startChoosing();
  }

  deal() {
    const deck = shuffle(Array.from({ length: 104 }, (_, i) => i + 1));
    this.rows = [0, 1, 2, 3].map(() => [deck.shift()]);
    for (const p of this.players) {
      p.hand = deck.splice(0, TOTAL_ROUNDS).sort((a, b) => a - b);
      p.score = 0;
      p.chosen = null;
    }
  }

  startChoosing() {
    this.phase = 'choosing';
    this.pending = [];
    this.chooserId = null;
    for (const p of this.players) {
      p.chosen = p.connected ? null : this.randomCard(p);
    }
    this.startTimer(TURN_SECONDS, () => this.resolveRound());
    this.broadcastState();
  }

  play(peer, card) {
    const p = this.find(peer);
    if (!p || this.phase !== 'choosing' || p.chosen !== null) return;
    if (!p.hand.includes(card)) return this.error(peer, "Cette carte n'est pas dans votre main");
    p.chosen = card;
    this.checkAllPlayed();
  }

  checkAllPlayed() {
    if (this.players.every(x => x.chosen !== null)) this.resolveRound();
    else this.broadcastState();
  }

  resolveRound() {
    if (this.phase !== 'choosing') return;
    this.cancelTimer();
    this.pending = [];
    for (const p of this.players) {
      if (p.chosen === null) p.chosen = this.randomCard(p);
      p.hand = p.hand.filter(c => c !== p.chosen);
      this.pending.push({ player: p, card: p.chosen });
      p.chosen = null;
    }
    this.pending.sort((a, b) => a.card - b.card);
    this.phase = 'reveal';
    this.log = 'Cartes révélées !';
    this.broadcastState();
    this.schedulePlace(REVEAL_DELAY_MS);
  }

  schedulePlace(ms) {
    clearTimeout(this.placeTimer);
    this.placeTimer = setTimeout(() => this.placeNext(), ms);
  }

  placeNext() {
    if (this.pending.length === 0) return this.endRound();

    const play = this.pending[0];
    let target = -1;
    this.rows.forEach((row, i) => {
      const last = row[row.length - 1];
      if (last < play.card && (target === -1 || last > this.lastOf(target))) target = i;
    });

    if (target === -1) {
      // Carte plus petite que toutes les piles : le joueur doit en ramasser une.
      if (play.player.connected) {
        this.phase = 'chooseRow';
        this.chooserId = play.player.id;
        this.log = `${play.player.name} doit choisir une pile à ramasser`;
        this.startTimer(CHOOSE_ROW_SECONDS, () => this.applyRowChoice(this.minMalusRow()));
        this.broadcastState();
      } else {
        this.applyRowChoice(this.minMalusRow());
      }
      return;
    }

    if (this.rows[target].length >= 5) {
      const m = this.takeRow(play.player, target);
      this.log = `${play.player.name} pose le ${play.card} en 6e carte et ramasse ${m} tête(s)`;
    } else {
      this.log = `${play.player.name} pose le ${play.card} sur la pile ${target + 1}`;
    }
    this.rows[target].push(play.card);
    this.pending.shift();
    this.phase = 'reveal';
    this.broadcastState();
    this.schedulePlace(PLACE_DELAY_MS);
  }

  chooseRow(peer, row) {
    const p = this.find(peer);
    if (!p || this.phase !== 'chooseRow' || p.id !== this.chooserId) return;
    if (!Number.isInteger(row) || row < 0 || row >= this.rows.length) return this.error(peer, 'Pile invalide');
    this.applyRowChoice(row);
  }

  applyRowChoice(row) {
    if (this.pending.length === 0) return;
    this.cancelTimer();
    const play = this.pending.shift();
    const m = this.takeRow(play.player, row);
    this.rows[row].push(play.card);
    this.chooserId = null;
    this.phase = 'reveal';
    this.log = `${play.player.name} ramasse la pile ${row + 1} (${m} tête(s))`;
    this.broadcastState();
    this.schedulePlace(PLACE_DELAY_MS);
  }

  endRound() {
    if (this.round >= TOTAL_ROUNDS) {
      this.phase = 'end';
      this.log = 'Partie terminée !';
      this.broadcastState();
      return;
    }
    this.round++;
    this.startChoosing();
  }

  // ===================== Utilitaires =====================

  lastOf(i) {
    const r = this.rows[i];
    return r[r.length - 1];
  }

  rowMalus(i) {
    return this.rows[i].reduce((s, c) => s + malus(c), 0);
  }

  minMalusRow() {
    let best = 0;
    for (let i = 1; i < this.rows.length; i++) {
      if (this.rowMalus(i) < this.rowMalus(best)) best = i;
    }
    return best;
  }

  takeRow(p, i) {
    const m = this.rowMalus(i);
    p.score += m;
    this.rows[i] = [];
    return m;
  }

  randomCard(p) {
    return p.hand[Math.floor(Math.random() * p.hand.length)];
  }

  find(peer) {
    return this.players.find(p => p.id === peer.id);
  }

  startTimer(seconds, onTimeout) {
    this.cancelTimer();
    this.deadline = Date.now() + seconds * 1000;
    this.timer = setTimeout(onTimeout, seconds * 1000);
  }

  cancelTimer() {
    clearTimeout(this.timer);
    this.timer = null;
    this.deadline = 0;
  }

  // ===================== Envoi =====================

  send(p, msg) {
    if (p.connected && p.peer) p.peer.send(JSON.stringify(msg));
  }

  error(peer, message) {
    peer.send(JSON.stringify({ type: 'error', message }));
  }

  broadcastLobby() {
    const msg = {
      type: 'lobby',
      roomId: this.roomId,
      hostId: this.hostId,
      players: this.players.map(p => ({ id: p.id, name: p.name })),
    };
    for (const p of this.players) this.send(p, msg);
  }

  broadcastState() {
    const players = this.players.map(p => ({
      id: p.id,
      name: p.name,
      score: p.score,
      ready: p.chosen !== null,
      connected: p.connected,
      cards: p.hand.length,
    }));
    const plays = this.pending.map(pl => ({ playerId: pl.player.id, name: pl.player.name, card: pl.card }));
    const timeLeft = this.deadline ? Math.max(0, Math.ceil((this.deadline - Date.now()) / 1000)) : 0;

    for (const p of this.players) {
      this.send(p, {
        type: 'state',
        roomId: this.roomId,
        phase: this.phase,
        round: this.round,
        totalRounds: TOTAL_ROUNDS,
        hostId: this.hostId,
        rows: this.rows,
        players,
        hand: p.hand,
        chosen: p.chosen,
        plays,
        chooserId: this.chooserId,
        timeLeft,
        log: this.log,
      });
    }
  }
}

module.exports = { GameRoom, MAX_PLAYERS, malus };
