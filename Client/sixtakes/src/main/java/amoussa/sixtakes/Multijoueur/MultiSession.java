package amoussa.sixtakes.Multijoueur;

import java.util.Map;

import javax.swing.JOptionPane;

import amoussa.Home;
import amoussa.sixtakes.Multijoueur.Net.GameClient;
import amoussa.sixtakes.Multijoueur.Net.Json;
import amoussa.sixtakes.Multijoueur.Server.LanServer;
import amoussa.sixtakes.Multijoueur.View.Lobby;
import amoussa.sixtakes.Multijoueur.View.MultiGameFen;

/**
 * Session multijoueur : relie la connexion réseau aux fenêtres (salon, partie).
 */
public class MultiSession implements GameClient.Listener {

    /**
     * Mode de connexion.
     */
    public enum Mode {
        LAN, ONLINE
    }

    private final Mode mode;
    private final String serverLabel;
    private GameClient client;
    private LanServer lanServer;
    private String myId;
    private Lobby lobby;
    private MultiGameFen gameFen;
    private boolean closed;

    public MultiSession(Mode mode, String serverLabel) {
        this.mode = mode;
        this.serverLabel = serverLabel;
    }

    /**
     * Ouvrir la connexion (à appeler hors du thread Swing).
     *
     * @param url
     * @throws Exception
     */
    public void connect(String url) throws Exception {
        this.client = GameClient.connect(url, this);
    }

    /**
     * Associer le serveur LAN hébergé par ce joueur (arrêté avec la session).
     *
     * @param s
     */
    public void setLanServer(LanServer s) {
        this.lanServer = s;
    }

    public void send(Map<String, Object> msg) {
        if (client != null) {
            client.send(msg);
        }
    }

    public String getMyId() {
        return myId;
    }

    public Mode getMode() {
        return mode;
    }

    public String getServerLabel() {
        return serverLabel;
    }

    public boolean isHosting() {
        return lanServer != null;
    }

    @Override
    public void onMessage(Map<String, Object> msg) {
        if (closed) {
            return;
        }
        String type = Json.str(msg, "type");
        if (type == null) {
            return;
        }
        switch (type) {
            case "joined":
                myId = Json.str(msg, "playerId");
                break;
            case "lobby":
                if (gameFen != null) {
                    gameFen.dispose();
                    gameFen = null;
                }
                if (lobby == null) {
                    lobby = new Lobby(this);
                }
                lobby.render(msg);
                break;
            case "state":
                if (lobby != null) {
                    lobby.dispose();
                    lobby = null;
                }
                if (gameFen == null) {
                    gameFen = new MultiGameFen(this);
                }
                gameFen.render(msg);
                break;
            case "error":
                JOptionPane.showMessageDialog(gameFen != null ? gameFen : lobby,
                        Json.str(msg, "message"), "Multijoueur", JOptionPane.WARNING_MESSAGE);
                if (lobby == null && gameFen == null) {
                    // refus à l'entrée du salon (plein, partie en cours, code inconnu...)
                    close();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onDisconnected(String reason) {
        if (closed) {
            return;
        }
        JOptionPane.showMessageDialog(gameFen != null ? gameFen : lobby, reason, "Déconnexion",
                JOptionPane.ERROR_MESSAGE);
        close();
    }

    /**
     * Quitter la session et revenir à l'accueil.
     */
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (client != null) {
            client.send(Json.obj("type", "leave"));
            client.close();
        }
        if (lanServer != null) {
            lanServer.stop();
        }
        if (lobby != null) {
            lobby.dispose();
        }
        if (gameFen != null) {
            gameFen.dispose();
        }
        new Home();
    }
}
