package amoussa.sixtakes.Solo.Controller;
import java.awt.event.*;
import javax.swing.*;

import amoussa.sixtakes.Multijoueur.View.MultiMenu;
import amoussa.sixtakes.Solo.View.*;
/**
 * Listener gérant les boutons de la fenêtre d'accueil.
 */
public class AccueilListener implements ActionListener {

    private JFrame fen ;
    private JSpinner nbr_player;
    private JDialog jdial;

    public AccueilListener(JFrame f){
        this.fen = f;
    }
    public AccueilListener(JSpinner n, JDialog jd){
        this.nbr_player = n;
        this.jdial = jd;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if( e.getActionCommand() =="Partie Solo"){
            this.fen.dispose();
            new FormNumberPlayer();
           
        }

        if( e.getActionCommand() =="Multijoueur"){
            new MultiMenu(this.fen);
        }
    }   
}
