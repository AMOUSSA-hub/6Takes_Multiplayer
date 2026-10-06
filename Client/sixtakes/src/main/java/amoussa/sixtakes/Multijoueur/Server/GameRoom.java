package amoussa.sixtakes.Multijoueur.Server;

import java.util.*;
import java.util.concurrent.*;

import amoussa.sixtakes.Multijoueur.Net.Json;

/**
 * Salon de jeu faisant autorité (logique côté serveur).
 * Même protocole et mêmes règles que le serveur Node (Server/game/GameRoom.js).
 * Toutes les méthodes publiques s'exécutent sur un unique thread dédié.
 */
public class GameRoom {

    public static final int MAX_PLAYERS = 10;
    public static final int TOTAL_ROUNDS = 10;
    public static final int TURN_SECONDS = 30;
    public static final int CHOOSE_ROW_SECONDS = 15;
    private static final int REVEAL_DELAY_MS = 2000;
    private static final int PLACE_DELAY_MS = 1200;

    /**
     * Un client connecté au salon.
     */
    public interface Peer {
        String getId();

        void send(String json);
    }

    private static class PlayerState {
        final String id;
        final String name;
        Peer peer;
        final List<Integer> hand = new ArrayList<>();
        int score;
        Integer chosen;
        boolean connected = true;

        PlayerState(Peer peer, String name) {
            this.peer = peer;
            this.id = peer.getId();
            this.name = name;
        }
    }

    private static class Play {
        final PlayerState player;
        final int card;

        Play(PlayerState p, int c) {
            this.player = p;
            this.card = c;
        }
    }

    private final String roomId;
    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "game-room");
        t.setDaemon(true);
        return t;
    });
    private final Random random = new Random();

    private final List<PlayerState> players = new ArrayList<>();
    private final List<List<Integer>> rows = new ArrayList<>();
    private final List<Play> pending = new ArrayList<>();
    private String hostId;
    private String phase = "lobby";
    private int round;
    private String chooserId;
    private long deadline;
    private ScheduledFuture<?> timer;
    private String log = "";

    public GameRoom(String roomId) {
        this.roomId = roomId;
    }

    // ===================== Entrées (thread-safe) =====================

    /**
     * Traiter un message JSON reçu d'un client.
     *
     * @param peer
     * @param raw
     */
    public void handle(Peer peer, String raw) {
        exec.execute(() -> {
            try {
                Map<String, Object> msg = Json.parseObject(raw);
                String type = Json.str(msg, "type");
                if (type == null) {
                    return;
                }
                switch (type) {
                    case "create":
                    case "join":
                        join(peer, Json.str(msg, "name"));
                        break;
                    case "start":
                        start(peer);
                        break;
                    case "play":
                        play(peer, Json.integer(msg, "card", -1));
                        break;
                    case "chooseRow":
                        chooseRow(peer, Json.integer(msg, "row", -1));
                        break;
                    case "leave":
                        leave(peer);
                        break;
                    default:
                        error(peer, "Message inconnu : " + type);
                }
            } catch (Exception e) {
                e.printStackTrace();
                error(peer, "Message invalide");
            }
        });
    }

    /**
     * Signaler la déconnexion d'un client.
     *
     * @param peer
     */
    public void disconnected(Peer peer) {
        exec.execute(() -> leave(peer));
    }

    /**
     * Arrêter le salon.
     */
    public void shutdown() {
        exec.shutdownNow();
    }

    public String getPhase() {
        return phase;
    }

    // ===================== Logique =====================

    private void join(Peer peer, String name) {
        if (find(peer) != null) {
            return;
        }
        if (!phase.equals("lobby")) {
            error(peer, "La partie a déjà commencé");
            return;
        }
        if (players.size() >= MAX_PLAYERS) {
            error(peer, "Le salon est plein (" + MAX_PLAYERS + " joueurs max)");
            return;
        }
        name = (name == null || name.isBlank()) ? "Joueur " + (players.size() + 1) : name.trim();
        if (name.length() > 20) {
            name = name.substring(0, 20);
        }
        PlayerState p = new PlayerState(peer, name);
        players.add(p);
        if (hostId == null) {
            hostId = p.id;
        }
        send(p, Json.obj("type", "joined", "roomId", roomId, "playerId", p.id));
        broadcastLobby();
    }

    private void leave(Peer peer) {
        PlayerState p = find(peer);
        if (p == null) {
            return;
        }
        if (phase.equals("lobby")) {
            players.remove(p);
            if (p.id.equals(hostId)) {
                hostId = players.isEmpty() ? null : players.get(0).id;
            }
            broadcastLobby();
            return;
        }
        // En partie : le joueur est remplacé par un automate.
        p.connected = false;
        p.peer = null;
        if (p.id.equals(hostId)) {
            PlayerState next = players.stream().filter(x -> x.connected).findFirst().orElse(null);
            hostId = next == null ? null : next.id;
        }
        log = p.name + " s'est déconnecté (joué automatiquement)";
        if (phase.equals("choosing") && p.chosen == null) {
            p.chosen = randomCard(p);
            checkAllPlayed();
        } else if (phase.equals("chooseRow") && p.id.equals(chooserId)) {
            applyRowChoice(minMalusRow());
            return;
        }
        broadcastState();
    }

    private void start(Peer peer) {
        PlayerState p = find(peer);
        if (p == null || !p.id.equals(hostId)) {
            error(peer, "Seul l'hôte peut lancer la partie");
            return;
        }
        if (!phase.equals("lobby") && !phase.equals("end")) {
            return;
        }
        players.removeIf(x -> !x.connected);
        if (players.size() < 2) {
            error(peer, "Il faut au moins 2 joueurs");
            phase = "lobby";
            broadcastLobby();
            return;
        }
        deal();
        round = 1;
        log = "La partie commence !";
        startChoosing();
    }

    private void deal() {
        List<Integer> deck = new ArrayList<>();
        for (int i = 1; i <= 104; i++) {
            deck.add(i);
        }
        Collections.shuffle(deck, random);
        rows.clear();
        for (int i = 0; i < 4; i++) {
            rows.add(new ArrayList<>(List.of(deck.remove(0))));
        }
        for (PlayerState p : players) {
            p.hand.clear();
            p.score = 0;
            p.chosen = null;
            for (int i = 0; i < TOTAL_ROUNDS; i++) {
                p.hand.add(deck.remove(0));
            }
            Collections.sort(p.hand);
        }
    }

    private void startChoosing() {
        phase = "choosing";
        pending.clear();
        chooserId = null;
        for (PlayerState p : players) {
            p.chosen = p.connected ? null : randomCard(p);
        }
        startTimer(TURN_SECONDS, this::resolveRound);
        broadcastState();
    }

    private void play(Peer peer, int card) {
        PlayerState p = find(peer);
        if (p == null || !phase.equals("choosing") || p.chosen != null) {
            return;
        }
        if (!p.hand.contains(card)) {
            error(peer, "Cette carte n'est pas dans votre main");
            return;
        }
        p.chosen = card;
        checkAllPlayed();
    }

    private void checkAllPlayed() {
        if (players.stream().allMatch(x -> x.chosen != null)) {
            resolveRound();
        } else {
            broadcastState();
        }
    }

    private void resolveRound() {
        if (!phase.equals("choosing")) {
            return;
        }
        cancelTimer();
        pending.clear();
        for (PlayerState p : players) {
            if (p.chosen == null) {
                p.chosen = randomCard(p);
            }
            p.hand.remove(p.chosen);
            pending.add(new Play(p, p.chosen));
            p.chosen = null;
        }
        pending.sort(Comparator.comparingInt(x -> x.card));
        phase = "reveal";
        log = "Cartes révélées !";
        broadcastState();
        exec.schedule(this::placeNext, REVEAL_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private void placeNext() {
        if (pending.isEmpty()) {
            endRound();
            return;
        }
        Play play = pending.get(0);
        int target = -1;
        for (int i = 0; i < rows.size(); i++) {
            int last = lastOf(i);
            if (last < play.card && (target == -1 || last > lastOf(target))) {
                target = i;
            }
        }

        if (target == -1) {
            // Carte plus petite que toutes les piles : le joueur doit en ramasser une.
            if (play.player.connected) {
                phase = "chooseRow";
                chooserId = play.player.id;
                log = play.player.name + " doit choisir une pile à ramasser";
                startTimer(CHOOSE_ROW_SECONDS, () -> applyRowChoice(minMalusRow()));
                broadcastState();
            } else {
                applyRowChoice(minMalusRow());
            }
            return;
        }

        List<Integer> row = rows.get(target);
        if (row.size() >= 5) {
            int malus = takeRow(play.player, target);
            log = play.player.name + " pose le " + play.card + " en 6e carte et ramasse " + malus + " tête(s)";
        } else {
            log = play.player.name + " pose le " + play.card + " sur la pile " + (target + 1);
        }
        rows.get(target).add(play.card);
        pending.remove(0);
        phase = "reveal";
        broadcastState();
        exec.schedule(this::placeNext, PLACE_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private void chooseRow(Peer peer, int row) {
        PlayerState p = find(peer);
        if (p == null || !phase.equals("chooseRow") || !p.id.equals(chooserId)) {
            return;
        }
        if (row < 0 || row >= rows.size()) {
            error(peer, "Pile invalide");
            return;
        }
        applyRowChoice(row);
    }

    private void applyRowChoice(int row) {
        if (pending.isEmpty()) {
            return;
        }
        cancelTimer();
        Play play = pending.remove(0);
        int malus = takeRow(play.player, row);
        rows.get(row).add(play.card);
        chooserId = null;
        phase = "reveal";
        log = play.player.name + " ramasse la pile " + (row + 1) + " (" + malus + " tête(s))";
        broadcastState();
        exec.schedule(this::placeNext, PLACE_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private void endRound() {
        if (round >= TOTAL_ROUNDS) {
            phase = "end";
            log = "Partie terminée !";
            broadcastState();
            return;
        }
        round++;
        startChoosing();
    }

    // ===================== Utilitaires =====================

    /**
     * Nombre de têtes de boeuf d'une carte.
     *
     * @param v
     * @return
     */
    public static int malus(int v) {
        if (v % 11 == 0) {
            return v == 55 ? 7 : 5;
        }
        if (v % 10 == 0) {
            return 3;
        }
        if (v % 5 == 0) {
            return 2;
        }
        return 1;
    }

    private int lastOf(int row) {
        List<Integer> r = rows.get(row);
        return r.get(r.size() - 1);
    }

    private int rowMalus(int row) {
        return rows.get(row).stream().mapToInt(GameRoom::malus).sum();
    }

    private int minMalusRow() {
        int best = 0;
        for (int i = 1; i < rows.size(); i++) {
            if (rowMalus(i) < rowMalus(best)) {
                best = i;
            }
        }
        return best;
    }

    private int takeRow(PlayerState p, int row) {
        int m = rowMalus(row);
        p.score += m;
        rows.get(row).clear();
        return m;
    }

    private int randomCard(PlayerState p) {
        return p.hand.get(random.nextInt(p.hand.size()));
    }

    private PlayerState find(Peer peer) {
        for (PlayerState p : players) {
            if (p.id.equals(peer.getId())) {
                return p;
            }
        }
        return null;
    }

    private void startTimer(int seconds, Runnable onTimeout) {
        cancelTimer();
        deadline = System.currentTimeMillis() + seconds * 1000L;
        timer = exec.schedule(onTimeout, seconds, TimeUnit.SECONDS);
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel(false);
            timer = null;
        }
        deadline = 0;
    }

    // ===================== Envoi =====================

    private void send(PlayerState p, Map<String, Object> msg) {
        if (p.connected && p.peer != null) {
            p.peer.send(Json.stringify(msg));
        }
    }

    private void error(Peer peer, String message) {
        peer.send(Json.stringify(Json.obj("type", "error", "message", message)));
    }

    private void broadcastLobby() {
        List<Object> list = new ArrayList<>();
        for (PlayerState p : players) {
            list.add(Json.obj("id", p.id, "name", p.name));
        }
        Map<String, Object> msg = Json.obj("type", "lobby", "roomId", roomId, "hostId", hostId, "players", list);
        for (PlayerState p : players) {
            send(p, msg);
        }
    }

    private void broadcastState() {
        List<Object> pubPlayers = new ArrayList<>();
        for (PlayerState p : players) {
            pubPlayers.add(Json.obj(
                    "id", p.id,
                    "name", p.name,
                    "score", p.score,
                    "ready", p.chosen != null,
                    "connected", p.connected,
                    "cards", p.hand.size()));
        }
        List<Object> plays = new ArrayList<>();
        for (Play pl : pending) {
            plays.add(Json.obj("playerId", pl.player.id, "name", pl.player.name, "card", pl.card));
        }
        int timeLeft = deadline == 0 ? 0 : (int) Math.max(0, Math.ceil((deadline - System.currentTimeMillis()) / 1000.0));

        for (PlayerState p : players) {
            send(p, Json.obj(
                    "type", "state",
                    "roomId", roomId,
                    "phase", phase,
                    "round", round,
                    "totalRounds", TOTAL_ROUNDS,
                    "hostId", hostId,
                    "rows", rows,
                    "players", pubPlayers,
                    "hand", p.hand,
                    "chosen", p.chosen,
                    "plays", plays,
                    "chooserId", chooserId,
                    "timeLeft", timeLeft,
                    "log", log));
        }
    }
}
