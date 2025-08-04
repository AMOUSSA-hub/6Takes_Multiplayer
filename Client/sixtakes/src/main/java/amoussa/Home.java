package amoussa;

import amoussa.sixtakes.Solo.Controller.*;
import amoussa.sixtakes.Solo.View.HomeButton;

import javax.swing.*;

import amoussa.sixtakes.Utils.Icone;

import java.awt.*;
/**
 * Fenêtre d'accueil
 */
public class Home extends JFrame {

    public Home() {
        setTitle("6Takes");
        this.setBackground(Color.WHITE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(900, 500));
        setResizable(false);
        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        JLabel imageLabel = new JLabel(new ImageIcon(Icone.home_Image));
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        HomeButton solo = new HomeButton("Partie Solo");
        solo.setAlignmentX(Component.CENTER_ALIGNMENT);
        HomeButton multi = new HomeButton("Multijoueur");
        multi.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(imageLabel);
        centerPanel.add(Box.createVerticalStrut(10)); 
        centerPanel.add(solo);
        centerPanel.add(multi);
        centerPanel.add(Box.createVerticalGlue());

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton quit = new HomeButton("Quitter");
        footerPanel.add(quit);
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        solo.addActionListener(new AccueilListener(this));
        multi.addActionListener(new AccueilListener(this));
        quit.addActionListener(e -> this.dispose());


        centerPanel.setBackground(Color.white);
        footerPanel.setBackground(Color.white);
        mainPanel.setBackground(Color.white);

        add(mainPanel);
        pack();
        setResizable(false);
        setLocationRelativeTo(null); // Centrer la fenÃªtre
        setVisible(true);
    }

 
}