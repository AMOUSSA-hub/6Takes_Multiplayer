package amoussa.sixtakes.Solo.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;
import java.awt.*;

import amoussa.Home;
import amoussa.sixtakes.Solo.Model.Game;
import amoussa.sixtakes.Solo.Model.Player;

/**
 * Fenêtre d'affichage du résultat final.
 */
public class ResultFrame extends JDialog {

    // Constructeur de la classe ResultFrame
    public ResultFrame(List<Player> joueurs, JFrame gameFen) {
        super(gameFen, true);
        // Création d'une copie de la liste des joueurs
        List<Player> joueursTriés = new ArrayList<>(joueurs);

        // Tri des joueurs en fonction du malus
        Collections.sort(joueursTriés, Comparator.comparingInt(Player::getMalus));

        // Configuration de la boîte de dialogue
        setTitle("Résultats");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        // Création d'un panneau pour une meilleure mise en page
        JPanel panneauPrincipal = new JPanel(new GridLayout(joueurs.size() + 1, 1, 20, 20));
        panneauPrincipal.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JButton goHome = new JButton("retour au menu");
        JButton quit = new JButton("quitter");

        goHome.addActionListener(e -> {
            gameFen.dispose();
            new Home();
        });
        quit.addActionListener(e -> System.exit(0));

        JPanel botPanel = new JPanel(new FlowLayout());

        botPanel.add(goHome);
        botPanel.add(quit);

        // Ajout d'une étiquette de titre
        JLabel étiquetteTitre = new JLabel("Résultats Finaux");
        étiquetteTitre.setHorizontalAlignment(SwingConstants.CENTER);
        étiquetteTitre.setFont(new Font("Arial", Font.BOLD, 18));
        panneauPrincipal.add(étiquetteTitre);

        int rank = 1;
        // Ajout des résultats des joueurs
        for (int i = 0; i < joueurs.size(); i++) {
            Player joueur = joueursTriés.get(i);

            if (i != 0 && joueursTriés.get(i).getMalus() > joueursTriés.get(i - 1).getMalus()) {
                rank++;
                System.out.println();
            }

            String texteRésultat = String.format("%der : Joueur %d - %d points", rank, joueurs.indexOf(joueur) + 1,
                    joueur.getMalus());
            JLabel étiquetteRésultat = new JLabel(texteRésultat);
            étiquetteRésultat.setHorizontalAlignment(SwingConstants.CENTER);

            // Attribution des couleurs en fonction de la position
            if (rank == 1) {
                étiquetteRésultat.setForeground(Color.ORANGE); // Or pour le premier joueur
            } else if (rank == 2) {
                étiquetteRésultat.setForeground(Color.LIGHT_GRAY); // Argent pour le deuxième joueur
            } else if (rank == 3) {
                étiquetteRésultat.setForeground(new Color(205, 127, 50)); // Bronze pour le troisième joueur
            }

            panneauPrincipal.add(étiquetteRésultat);
        }

        // Ajout du panneau principal à la boîte de dialogue
        add(panneauPrincipal, BorderLayout.CENTER);
        add(botPanel, BorderLayout.SOUTH);

        // Configuration de la taille et de la visibilité
        pack();
        setLocationRelativeTo(gameFen);
        setVisible(true);

    }
}
