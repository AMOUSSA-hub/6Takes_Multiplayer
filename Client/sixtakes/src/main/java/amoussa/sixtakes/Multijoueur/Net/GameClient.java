package amoussa.sixtakes.Multijoueur.Net;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.swing.SwingUtilities;

/**
 * Client WebSocket vers un serveur de partie (LAN ou online).
 * Les messages reçus sont transmis sur le thread Swing.
 */
public class GameClient {

    /**
     * Réception des événements réseau (appelée sur le thread Swing).
     */
    public interface Listener {
        void onMessage(Map<String, Object> msg);

        void onDisconnected(String reason);
    }

    private final WebSocket ws;
    private volatile Listener listener;
    private CompletableFuture<?> sendChain = CompletableFuture.completedFuture(null);
    private volatile boolean closedByUser;

    private GameClient(WebSocket ws) {
        this.ws = ws;
    }

    /**
     * Se connecter à un serveur.
     *
     * @param url  ex : ws://192.168.1.10:3000 ou wss://mon-serveur.onrender.com
     * @param listener
     * @return
     * @throws Exception si la connexion échoue
     */
    public static GameClient connect(String url, Listener listener) throws Exception {
        Receiver receiver = new Receiver();
        WebSocket ws = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build()
                .newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .buildAsync(URI.create(normalize(url)), receiver)
                .get();
        GameClient c = new GameClient(ws);
        c.listener = listener;
        receiver.client = c;
        return c;
    }

    /**
     * Compléter une adresse saisie par l'utilisateur (ajout du schéma et du port).
     *
     * @param url
     * @return
     */
    public static String normalize(String url) {
        String u = url.trim();
        if (!u.startsWith("ws://") && !u.startsWith("wss://")) {
            if (u.startsWith("http://")) {
                u = "ws://" + u.substring(7);
            } else if (u.startsWith("https://")) {
                u = "wss://" + u.substring(8);
            } else {
                u = "ws://" + u;
                // pas de port précisé : port par défaut du jeu
                if (!u.substring(5).contains(":")) {
                    u += ":3000";
                }
            }
        }
        return u;
    }

    public void setListener(Listener l) {
        this.listener = l;
    }

    /**
     * Envoyer un message au serveur.
     *
     * @param msg
     */
    public synchronized void send(Map<String, Object> msg) {
        String text = Json.stringify(msg);
        sendChain = sendChain.handle((v, e) -> null).thenCompose(v -> ws.sendText(text, true));
    }

    /**
     * Fermer la connexion.
     */
    public void close() {
        closedByUser = true;
        try {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
        } catch (Exception ignored) {
        }
        ws.abort();
    }

    private void dispatch(Map<String, Object> msg) {
        SwingUtilities.invokeLater(() -> {
            Listener l = listener;
            if (l != null) {
                l.onMessage(msg);
            }
        });
    }

    private void disconnected(String reason) {
        if (closedByUser) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            Listener l = listener;
            if (l != null) {
                l.onDisconnected(reason);
            }
        });
    }

    private static class Receiver implements WebSocket.Listener {
        volatile GameClient client;
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                String text = buffer.toString();
                buffer.setLength(0);
                try {
                    if (client != null) {
                        client.dispatch(Json.parseObject(text));
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("Message illisible : " + text);
                }
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            if (client != null) {
                client.disconnected("Le serveur a fermé la connexion");
            }
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            if (client != null) {
                client.disconnected("Connexion perdue : " + error.getMessage());
            }
        }
    }
}
