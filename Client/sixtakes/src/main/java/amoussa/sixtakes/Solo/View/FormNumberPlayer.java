package amoussa.sixtakes.Solo.View;

import javax.swing.*;
import java.awt.*;

/**
 * Fenetre permettant de choisir le nombre de joueur.
 */
public class FormNumberPlayer extends JDialog {

    private static final int DEFAULT_WIDTH = 300;
    private static final int DEFAULT_HEIGHT = 220;

    public FormNumberPlayer() {
        setLayout(new GridLayout(3, 1));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JSpinner nbrPlayerSpinner = new JSpinner(new SpinnerNumberModel(2, 2, 10, 1));
        JButton confirmButton = new JButton("Ok");

        JPanel midPanel = new JPanel(new FlowLayout());

        add(new JLabel(" Sélectionner le nombre de joueurs (de 2 à 10 joueurs) "));
        add(midPanel);
        midPanel.add(nbrPlayerSpinner);
        midPanel.add(confirmButton);

        confirmButton.addActionListener(e -> {
            new GameFen((Integer) nbrPlayerSpinner.getValue());
            this.dispose();
        });

        pack();
        setResizable(false);
        setLocationRelativeTo(null);
        setVisible(true);
    }
}
