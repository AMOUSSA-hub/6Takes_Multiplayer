package amoussa.sixtakes.Multijoueur.View;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.*;

import amoussa.sixtakes.Multijoueur.MultiSession;
import amoussa.sixtakes.Multijoueur.Net.GameClient;
import amoussa.sixtakes.Multijoueur.Net.Json;
import amoussa.sixtakes.Multijoueur.Net.LanDiscovery;
import amoussa.sixtakes.Multijoueur.Server.LanServer;

/**
 * Menu multijoueur : héberger / rejoindre en réseau local, ou jouer en ligne.
 */
public class MultiMenu extends JDialog {

    /** Serveur online par défaut (modifiable avec -Dsixtakes.server=... ou SIXTAKES_SERVER). */
    private static final String DEFAULT_ONLINE_SERVER = System.getProperty("sixtakes.server",
            System.getenv().getOrDefault("SIXTAKES_SERVER", "ws://localhost:3000"));

    private final JFrame home;
    private final JTextField pseudo = new JTextField(System.getProperty("user.name", "Joueur"), 15);

    // Réseau local
    private final DefaultListModel<LanDiscovery.HostInfo> lanHosts = new DefaultListModel<>();
    private final JList<LanDiscovery.HostInfo> lanList = new JList<>(lanHosts);
    private final JTextField lanAddress = new JTextField(15);

    // En ligne
    private final JTextField serverUrl = new JTextField(DEFAULT_ONLINE_SERVER, 22);
    private final JTextField roomCode = new JTextField(6);
    private final DefaultListModel<String> rooms = new DefaultListModel<>();
    private final JList<String> roomList = new JList<>(rooms);

    private final JLabel status = new JLabel(" ");
    private boolean busy;

    public MultiMenu(JFrame home) {
        super(home, "Multijoueur", true);
        this.home = home;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Votre pseudo :"));
        top.add(pseudo);
        main.add(top, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Réseau local", lanTab());
        tabs.addTab("En ligne", onlineTab());
        main.add(tabs, BorderLayout.CENTER);

        status.setForeground(Color.DARK_GRAY);
        main.add(status, BorderLayout.SOUTH);

        add(main);
        pack();
        setResizable(false);
        setLocationRelativeTo(home);
        SwingUtilities.invokeLater(this::searchLan);
        setVisible(true);
    }

    // ===================== Onglets =====================

    private JPanel lanTab() {
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton host = new JButton("Héberger une partie");
        host.addActionListener(e -> hostLan());
        JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT));
        north.add(host);
        north.add(new JLabel("(les autres joueurs doivent être sur le même réseau)"));
        p.add(north, BorderLayout.NORTH);

        lanList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lanList.addListSelectionListener(e -> {
            LanDiscovery.HostInfo h = lanList.getSelectedValue();
            if (h != null) {
                lanAddress.setText(h.address + ":" + h.port);
            }
        });
        JScrollPane scroll = new JScrollPane(lanList);
        scroll.setPreferredSize(new Dimension(420, 120));
        scroll.setBorder(BorderFactory.createTitledBorder("Parties trouvées sur le réseau"));
        p.add(scroll, BorderLayout.CENTER);

        JButton refresh = new JButton("Rechercher");
        refresh.addActionListener(e -> searchLan());
        JButton join = new JButton("Rejoindre");
        join.addActionListener(e -> joinLan());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT));
        south.add(refresh);
        south.add(new JLabel("Adresse de l'hôte :"));
        south.add(lanAddress);
        south.add(join);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    private JPanel onlineTab() {
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT));
        north.add(new JLabel("Serveur :"));
        north.add(serverUrl);
        JButton create = new JButton("Créer un salon");
        create.addActionListener(e -> createOnline());
        north.add(create);
        p.add(north, BorderLayout.NORTH);

        roomList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roomList.addListSelectionListener(e -> {
            String r = roomList.getSelectedValue();
            if (r != null) {
                roomCode.setText(r.substring(0, r.indexOf(' ')));
            }
        });
        JScrollPane scroll = new JScrollPane(roomList);
        scroll.setPreferredSize(new Dimension(420, 120));
        scroll.setBorder(BorderFactory.createTitledBorder("Salons ouverts"));
        p.add(scroll, BorderLayout.CENTER);

        JButton refresh = new JButton("Actualiser");
        refresh.addActionListener(e -> listRooms());
        JButton join = new JButton("Rejoindre");
        join.addActionListener(e -> joinOnline());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT));
        south.add(refresh);
        south.add(new JLabel("Code du salon :"));
        south.add(roomCode);
        south.add(join);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    // ===================== Actions =====================

    private String name() {
        String n = pseudo.getText().trim();
        return n.isEmpty() ? "Joueur" : n;
    }

    private void hostLan() {
        String name = name();
        runAsync("Démarrage du serveur...", () -> {
            LanServer server = new LanServer(LanServer.DEFAULT_PORT, name);
            try {
                server.start();
            } catch (Exception e) {
                throw new Exception("Impossible d'ouvrir le port " + LanServer.DEFAULT_PORT
                        + " (une partie est peut-être déjà hébergée sur ce PC).");
            }
            MultiSession s = new MultiSession(MultiSession.Mode.LAN, "hôte");
            s.setLanServer(server);
            try {
                s.connect("ws://127.0.0.1:" + LanServer.DEFAULT_PORT);
            } catch (Exception e) {
                server.stop();
                throw e;
            }
            return () -> {
                s.send(Json.obj("type", "create", "name", name));
                closeAll();
            };
        });
    }

    private void joinLan() {
        String addr = lanAddress.getText().trim();
        if (addr.isEmpty()) {
            status.setText("Choisissez une partie dans la liste ou saisissez l'adresse IP de l'hôte.");
            return;
        }
        connectAndJoin(MultiSession.Mode.LAN, addr, Json.obj("type", "join", "name", name(), "roomId", "LAN"));
    }

    private void createOnline() {
        connectAndJoin(MultiSession.Mode.ONLINE, serverUrl.getText(), Json.obj("type", "create", "name", name()));
    }

    private void joinOnline() {
        String code = roomCode.getText().trim().toUpperCase();
        if (code.isEmpty()) {
            status.setText("Saisissez le code du salon (ou choisissez-en un dans la liste).");
            return;
        }
        connectAndJoin(MultiSession.Mode.ONLINE, serverUrl.getText(),
                Json.obj("type", "join", "name", name(), "roomId", code));
    }

    private void connectAndJoin(MultiSession.Mode mode, String url, Map<String, Object> joinMsg) {
        runAsync("Connexion à " + url + "...", () -> {
            MultiSession s = new MultiSession(mode, GameClient.normalize(url));
            s.connect(url);
            return () -> {
                s.send(joinMsg);
                closeAll();
            };
        });
    }

    private void searchLan() {
        runAsync("Recherche de parties sur le réseau local...", () -> {
            List<LanDiscovery.HostInfo> found = LanDiscovery.search(2000);
            return () -> {
                lanHosts.clear();
                found.forEach(lanHosts::addElement);
                status.setText(found.isEmpty()
                        ? "Aucune partie trouvée : saisissez l'adresse IP de l'hôte si besoin."
                        : found.size() + " partie(s) trouvée(s).");
            };
        }, false);
    }

    private void listRooms() {
        String url = serverUrl.getText();
        runAsync("Récupération des salons...", () -> {
            CompletableFuture<Map<String, Object>> reply = new CompletableFuture<>();
            GameClient c = GameClient.connect(url, new GameClient.Listener() {
                @Override
                public void onMessage(Map<String, Object> msg) {
                    if ("rooms".equals(Json.str(msg, "type"))) {
                        reply.complete(msg);
                    }
                }

                @Override
                public void onDisconnected(String reason) {
                    reply.completeExceptionally(new Exception(reason));
                }
            });
            c.send(Json.obj("type", "rooms"));
            Map<String, Object> msg;
            try {
                msg = reply.get(5, TimeUnit.SECONDS);
            } finally {
                c.close();
            }
            return () -> {
                rooms.clear();
                for (Map<String, Object> r : Json.objects(msg, "rooms")) {
                    rooms.addElement(Json.str(r, "id") + " - partie de " + Json.str(r, "host") + " ("
                            + Json.integer(r, "players", 0) + "/10)");
                }
                status.setText(rooms.isEmpty() ? "Aucun salon ouvert : créez-en un !" : rooms.size() + " salon(s).");
            };
        });
    }

    private void closeAll() {
        dispose();
        home.dispose();
    }

    // ===================== Tâches de fond =====================

    private interface Task {
        Runnable run() throws Exception;
    }

    /**
     * Exécuter une tâche réseau hors du thread Swing, puis appliquer son résultat.
     */
    private void runAsync(String message, Task task) {
        runAsync(message, task, true);
    }

    /**
     * @param exclusive si vrai, aucune autre action ne peut être lancée en parallèle
     */
    private void runAsync(String message, Task task, boolean exclusive) {
        if (exclusive && busy) {
            return;
        }
        if (exclusive) {
            busy = true;
        }
        status.setText(message);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<Runnable, Void>() {
            @Override
            protected Runnable doInBackground() throws Exception {
                return task.run();
            }

            @Override
            protected void done() {
                if (exclusive) {
                    busy = false;
                }
                setCursor(Cursor.getDefaultCursor());
                try {
                    status.setText(" ");
                    get().run();
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    while (cause.getCause() != null) {
                        cause = cause.getCause();
                    }
                    String msg = cause.getMessage();
                    status.setText("Erreur : " + (msg == null ? cause.getClass().getSimpleName() : msg));
                }
            }
        }.execute();
    }
}
