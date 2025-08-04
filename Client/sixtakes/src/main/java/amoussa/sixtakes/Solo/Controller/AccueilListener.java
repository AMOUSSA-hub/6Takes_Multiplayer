package amoussa.sixtakes.Solo.Controller;
import java.awt.event.*;
import javax.swing.*;

import amoussa.sixtakes.Multijoueur.View.Lobby;
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
            String[] option = {"Créer une partie","Rejoindre une partie"};
            //JOptionPane.showMessageDialog(fen, "Cette section n'est pas encore disponible", "à venir", 0);
             int c = JOptionPane.showOptionDialog(fen, "Choisissez :", "Multijoueur", JOptionPane.DEFAULT_OPTION, 0, null,option, e);
            
             if(c== 0){
                 new Lobby();
             }
        } 
    }   
}
