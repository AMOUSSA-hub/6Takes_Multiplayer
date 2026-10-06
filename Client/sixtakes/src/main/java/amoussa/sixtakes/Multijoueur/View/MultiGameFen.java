package amoussa.sixtakes.Multijoueur.View;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.LineBorder;

import amoussa.sixtakes.Multijoueur.MultiSession;
import amoussa.sixtakes.Multijoueur.Net.Json;
import amoussa.sixtakes.Multijoueur.Server.GameRoom;
import amoussa.sixtakes.Solo.View.Card;
import amoussa.sixtakes.Solo.View.MyJLabel;
import amoussa.sixtakes.Utils.Icone;

/**
 * Fenêtre de partie multijoueur : affiche l'état envoyé par le serveur.
 */
public class MultiGameFen extends JFrame {

    private static final Color GREEN = new Color(55, 131, 65);
    private static final Color DARK_GREEN = new Color(40, 100, 50);

    private final MultiSession session;
    private final MyJLabel status = new MyJLabel("");
    private final MyJLabel log = new MyJLabel("");
    private final JPanel rowsPan = new JPanel(new GridLayout(4, 1, 5, 5));
    private final JPanel playersPan = new JPanel();
    private final JPanel playsPan = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
    private final JPanel handPan = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
    private final Timer countdown;

    private Map<String, Object> state;
    private int timeLeft;
    private JDialog resultDialog;

    public MultiGameFen(MultiSession session) {
        this.session = session;
        setTitle("6Takes - Multijoueur");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int c = JOptionPane.showConfirmDialog(MultiGameFen.this,
                        session.isHosting() ? "Vous êtes l'hôte : quitter arrêtera la partie pour tout le monde."
                                : "Quitter la partie ?",
                        "Quitter", JOptionPane.YES_NO_OPTION);
                if (c == JOptionPane.YES_OPTION) {
                    session.close();
                }
            }
        });

        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBackground(GREEN);
        main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Haut : manche, minuteur et journal
        JPanel top = new JPanel(new GridLayout(2, 1));
        top.setOpaque(false);
        status.setFont(new Font("Serif", Font.BOLD, 24));
        status.setHorizontalAlignment(SwingConstants.CENTER);
        log.setFont(new Font("SansSerif", Font.PLAIN, 16));
        log.setHorizontalAlignment(SwingConstants.CENTER);
        top.add(status);
        top.add(log);
        main.add(top, BorderLayout.NORTH);

        // Centre : les 4 piles
        rowsPan.setOpaque(false);
        main.add(rowsPan, BorderLayout.CENTER);

        // Droite : joueurs et cartes révélées
        JPanel east = new JPanel(new BorderLayout(5, 5));
        east.setOpaque(false);
        east.setPreferredSize(new Dimension(300, 0));
        playersPan.setLayout(new BoxLayout(playersPan, BoxLayout.Y_AXIS));
        playersPan.setBackground(DARK_GREEN);
        playersPan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JScrollPane playersScroll = new JScrollPane(playersPan);
        playersScroll.setBorder(null);
        playersScroll.getViewport().setBackground(DARK_GREEN);
        east.add(playersScroll, BorderLayout.CENTER);
        playsPan.setBackground(DARK_GREEN);
        playsPan.setPreferredSize(new Dimension(300, 200));
        east.add(playsPan, BorderLayout.SOUTH);
        main.add(east, BorderLayout.EAST);

        // Bas : la main du joueur
        handPan.setBackground(DARK_GREEN);
        handPan.setPreferredSize(new Dimension(0, 95));
        main.add(handPan, BorderLayout.SOUTH);

        add(main);

        countdown = new Timer(1000, e -> {
            if (timeLeft > 0) {
                timeLeft--;
                updateStatus();
            }
        });
        countdown.start();

        int h = (int) Icone.tailleEcran.getHeight();
        int w = (int) Icone.tailleEcran.getWidth();
        setSize(Math.max(1000, (int) (w * 0.7)), Math.max(700, (int) (h * 0.75)));
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Mettre à jour l'affichage à partir d'un message "state".
     *
     * @param msg
     */
    public void render(Map<String, Object> msg) {
        this.state = msg;
        this.timeLeft = Json.integer(msg, "timeLeft", 0);
        String phase = Json.str(msg, "phase");

        updateStatus();
        log.setText(Json.str(msg, "log"));
        renderRows(phase);
        renderPlayers();
        renderPlays();
        renderHand(phase);

        if ("end".equals(phase)) {
            showResults();
        } else if (resultDialog != null) {
            resultDialog.dispose();
            resultDialog = null;
        }

        getContentPane().revalidate();
        getContentPane().repaint();
    }

    private boolean isMe(String id) {
        return id != null && id.equals(session.getMyId());
    }

    private void updateStatus() {
        if (state == null) {
            return;
        }
        String phase = Json.str(state, "phase");
        String round = "Manche " + Json.integer(state, "round", 0) + "/" + Json.integer(state, "totalRounds", 10);
        String text;
        switch (phase) {
            case "choosing":
                text = (state.get("chosen") == null ? "Choisissez une carte" : "En attente des autres joueurs")
                        + " - " + timeLeft + " s";
                break;
            case "chooseRow":
                text = isMe(Json.str(state, "chooserId"))
                        ? "Votre carte est trop petite : cliquez sur une pile à ramasser - " + timeLeft + " s"
                        : "Un joueur choisit une pile... " + timeLeft + " s";
                break;
            case "reveal":
                text = "Placement des cartes...";
                break;
            case "end":
                text = "Partie terminée";
                break;
            default:
                text = "";
        }
        status.setText(round + "  |  " + text);
    }

    private void renderRows(String phase) {
        rowsPan.removeAll();
        boolean canChoose = "chooseRow".equals(phase) && isMe(Json.str(state, "chooserId"));
        List<Object> rows = Json.list(state, "rows");
        for (int i = 0; i < rows.size(); i++) {
            List<Integer> cards = Json.ints(rows.get(i));
            int malus = cards.stream().mapToInt(GameRoom::malus).sum();

            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
            row.setOpaque(false);
            MyJLabel label = new MyJLabel("Pile " + (i + 1) + " (" + malus + " têtes)");
            label.setPreferredSize(new Dimension(120, 70));
            row.add(label);
            for (int v : cards) {
                row.add(new Card(v));
            }

            if (canChoose) {
                final int index = i;
                row.setBorder(new LineBorder(Color.YELLOW, 3, true));
                row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                MouseAdapter click = new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        session.send(Json.obj("type", "chooseRow", "row", index));
                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        row.setBorder(new LineBorder(Color.BLACK, 5, true));
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        row.setBorder(new LineBorder(Color.YELLOW, 3, true));
                    }
                };
                row.addMouseListener(click);
                for (Component c : row.getComponents()) {
                    c.addMouseListener(click);
                }
            }
            rowsPan.add(row);
        }
    }

    private void renderPlayers() {
        playersPan.removeAll();
        MyJLabel title = new MyJLabel("Joueurs");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        playersPan.add(title);
        playersPan.add(Box.createVerticalStrut(5));

        String phase = Json.str(state, "phase");
        String hostId = Json.str(state, "hostId");
        for (Map<String, Object> p : Json.objects(state, "players")) {
            String id = Json.str(p, "id");
            StringBuilder sb = new StringBuilder("<html>");
            sb.append(isMe(id) ? "<b>" + Json.str(p, "name") + " (vous)</b>" : Json.str(p, "name"));
            if (id.equals(hostId)) {
                sb.append(" ★");
            }
            sb.append(" - ").append(Json.integer(p, "score", 0)).append(" têtes");
            if (!Json.bool(p, "connected")) {
                sb.append(" <i>(déconnecté)</i>");
            } else if ("choosing".equals(phase)) {
                sb.append(Json.bool(p, "ready") ? " ✔" : " …");
            }
            sb.append("</html>");
            MyJLabel l = new MyJLabel(sb.toString());
            l.setIcon(new ImageIcon(Icone.player));
            playersPan.add(l);
            playersPan.add(Box.createVerticalStrut(4));
        }
    }

    private void renderPlays() {
        playsPan.removeAll();
        List<Map<String, Object>> plays = Json.objects(state, "plays");
        if (plays.isEmpty()) {
            return;
        }
        MyJLabel title = new MyJLabel("Cartes à placer :");
        title.setPreferredSize(new Dimension(280, 20));
        playsPan.add(title);
        for (Map<String, Object> p : plays) {
            JPanel cell = new JPanel(new BorderLayout());
            cell.setOpaque(false);
            cell.add(new Card(Json.integer(p, "card", 0)), BorderLayout.CENTER);
            String name = Json.str(p, "name");
            MyJLabel n = new MyJLabel(name.length() > 8 ? name.substring(0, 8) : name);
            n.setFont(new Font("SansSerif", Font.PLAIN, 10));
            n.setHorizontalAlignment(SwingConstants.CENTER);
            cell.add(n, BorderLayout.SOUTH);
            playsPan.add(cell);
        }
    }

    private void renderHand(String phase) {
        handPan.removeAll();
        Object chosenObj = state.get("chosen");
        Integer chosen = chosenObj instanceof Number ? ((Number) chosenObj).intValue() : null;
        boolean canPlay = "choosing".equals(phase) && chosen == null;

        for (int v : Json.ints(state.get("hand"))) {
            Card c = new Card(v);
            if (chosen != null && chosen == v) {
                c.setHover(true);
            }
            if (canPlay) {
                c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                c.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        session.send(Json.obj("type", "play", "card", v));
                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        c.setHover(true);
                        c.repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        c.setHover(false);
                        c.repaint();
                    }
                });
            }
            handPan.add(c);
        }
    }

    private void showResults() {
        if (resultDialog != null) {
            return;
        }
        List<Map<String, Object>> ranking = new ArrayList<>(Json.objects(state, "players"));
        ranking.sort(Comparator.comparingInt(p -> Json.integer(p, "score", 0)));

        resultDialog = new JDialog(this, "Résultats", false);
        resultDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        JPanel pan = new JPanel(new GridLayout(ranking.size() + 1, 1, 10, 10));
        pan.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JLabel title = new JLabel("Résultats finaux", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        pan.add(title);

        int rank = 0;
        int previous = -1;
        for (int i = 0; i < ranking.size(); i++) {
            Map<String, Object> p = ranking.get(i);
            int score = Json.integer(p, "score", 0);
            if (score != previous) {
                rank = i + 1;
                previous = score;
            }
            String name = Json.str(p, "name") + (isMe(Json.str(p, "id")) ? " (vous)" : "");
            JLabel l = new JLabel(String.format("%d%s : %s - %d têtes", rank, rank == 1 ? "er" : "e", name, score),
                    SwingConstants.CENTER);
            if (rank == 1) {
                l.setForeground(Color.ORANGE);
            } else if (rank == 2) {
                l.setForeground(Color.GRAY);
            } else if (rank == 3) {
                l.setForeground(new Color(205, 127, 50));
            }
            pan.add(l);
        }

        JPanel bottom = new JPanel(new FlowLayout());
        if (isMe(Json.str(state, "hostId"))) {
            JButton again = new JButton("Rejouer");
            again.addActionListener(e -> session.send(Json.obj("type", "start")));
            bottom.add(again);
        } else {
            bottom.add(new JLabel("L'hôte peut relancer une partie."));
        }
        JButton quit = new JButton("Quitter");
        quit.addActionListener(e -> session.close());
        bottom.add(quit);

        resultDialog.add(pan, BorderLayout.CENTER);
        resultDialog.add(bottom, BorderLayout.SOUTH);
        resultDialog.pack();
        resultDialog.setLocationRelativeTo(this);
        resultDialog.setVisible(true);
    }

    @Override
    public void dispose() {
        countdown.stop();
        if (resultDialog != null) {
            resultDialog.dispose();
        }
        super.dispose();
    }
}
