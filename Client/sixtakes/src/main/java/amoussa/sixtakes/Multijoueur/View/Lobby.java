package amoussa.sixtakes.Multijoueur.View;

import javax.swing.*;

import amoussa.sixtakes.Multijoueur.MultiSession;
import amoussa.sixtakes.Multijoueur.Net.Json;
import amoussa.sixtakes.Multijoueur.Net.LanDiscovery;
import amoussa.sixtakes.Multijoueur.Server.LanServer;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Map;

/**
 * Salon d'attente multijoueur (avant le lancement de la partie).
 */
public class Lobby extends JFrame {

    private final MultiSession session;
    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JLabel info = new JLabel();
    private final JButton start = new JButton("Lancer la partie");

    public Lobby(MultiSession session) {
        this.session = session;
        setTitle("6Takes - Salon");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                session.close();
            }
        });

        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        info.setHorizontalAlignment(SwingConstants.CENTER);
        main.add(info, BorderLayout.NORTH);

        JList<String> playerList = new JList<>(listModel);
        JScrollPane scroll = new JScrollPane(playerList);
        scroll.setPreferredSize(new Dimension(320, 200));
        scroll.setBorder(BorderFactory.createTitledBorder("Joueurs (2 à 10)"));
        main.add(scroll, BorderLayout.CENTER);

        JButton quit = new JButton("Quitter");
        JPanel bottom = new JPanel(new FlowLayout());
        bottom.add(start);
        bottom.add(quit);
        main.add(bottom, BorderLayout.SOUTH);

        start.addActionListener(e -> session.send(Json.obj("type", "start")));
        quit.addActionListener(e -> session.close());

        add(main);
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Mettre à jour l'affichage à partir d'un message "lobby".
     *
     * @param msg
     */
    public void render(Map<String, Object> msg) {
        String hostId = Json.str(msg, "hostId");
        List<Map<String, Object>> players = Json.objects(msg, "players");

        listModel.clear();
        for (Map<String, Object> p : players) {
            String id = Json.str(p, "id");
            String label = Json.str(p, "name");
            if (id.equals(hostId)) {
                label += "  (hôte)";
            }
            if (id.equals(session.getMyId())) {
                label += "  ← vous";
            }
            listModel.addElement(label);
        }

        boolean amHost = session.getMyId() != null && session.getMyId().equals(hostId);
        start.setVisible(amHost);
        start.setEnabled(amHost && players.size() >= 2);

        String where;
        if (session.getMode() == MultiSession.Mode.LAN) {
            where = session.isHosting()
                    ? "Réseau local - les autres joueurs rejoignent : <b>" + LanDiscovery.localAddress() + ":"
                            + LanServer.DEFAULT_PORT + "</b>"
                    : "Réseau local - " + session.getServerLabel();
        } else {
            where = "En ligne - code du salon : <b>" + Json.str(msg, "roomId") + "</b>";
        }
        String waiting = amHost ? "Lancez la partie quand tout le monde est là."
                : "En attente du lancement par l'hôte...";
        info.setText("<html><center>" + where + "<br>" + waiting + "</center></html>");
        pack();
    }
}
