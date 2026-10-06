package amoussa.sixtakes.Multijoueur.Server;

import java.io.IOException;

import amoussa.sixtakes.Multijoueur.Net.LanDiscovery;
import amoussa.sixtakes.Multijoueur.Net.MiniWebSocketServer;

/**
 * Serveur de partie en réseau local, lancé dans l'application de l'hôte.
 * Il héberge un unique salon et s'annonce sur le réseau (découverte UDP).
 */
public class LanServer {

    public static final int DEFAULT_PORT = 3000;

    private final MiniWebSocketServer ws;
    private final GameRoom room;
    private final LanDiscovery.Beacon beacon;

    public LanServer(int port, String hostName) {
        this.room = new GameRoom("LAN");
        this.ws = new MiniWebSocketServer(port, new MiniWebSocketServer.Handler() {
            @Override
            public void onOpen(MiniWebSocketServer.Connection c) {
            }

            @Override
            public void onMessage(MiniWebSocketServer.Connection c, String message) {
                room.handle(peer(c), message);
            }

            @Override
            public void onClose(MiniWebSocketServer.Connection c) {
                room.disconnected(peer(c));
            }
        });
        this.beacon = new LanDiscovery.Beacon(hostName, port, () -> room.getPhase().equals("lobby"));
    }

    private static GameRoom.Peer peer(MiniWebSocketServer.Connection c) {
        return new GameRoom.Peer() {
            @Override
            public String getId() {
                return c.getId();
            }

            @Override
            public void send(String json) {
                c.send(json);
            }
        };
    }

    /**
     * Démarrer le serveur et l'annonce réseau.
     *
     * @throws IOException si le port est déjà utilisé
     */
    public void start() throws IOException {
        ws.start();
        beacon.start();
        System.out.println("Serveur LAN démarré sur le port " + ws.getPort());
    }

    /**
     * Arrêter le serveur.
     */
    public void stop() {
        beacon.stop();
        ws.stop();
        room.shutdown();
    }
}
