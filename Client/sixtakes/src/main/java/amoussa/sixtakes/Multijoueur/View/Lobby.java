package amoussa.sixtakes.Multijoueur.View;

import javax.swing.*;



import java.awt.BorderLayout;
/**
 * Salon de connexion multijoueur.
 */
public class Lobby extends JDialog {



    public Lobby(){
        System.out.println();
        setSize(200,200);
        
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
         
        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList playerList = new JList<>(listModel);
        listModel.addElement("Joueur 1");
        JButton confirm = new JButton("Lancer la partie");
        setResizable(false);
    
    

        //confirm.addActionListener(e ->{new GameFen((Integer)nbr_player.getValue());this.dispose();});
        add(playerList,BorderLayout.NORTH);
        add(confirm,BorderLayout.SOUTH);

       setLocationRelativeTo(null);
        setVisible(true);

        

        
    }

    
    
    
    
}
