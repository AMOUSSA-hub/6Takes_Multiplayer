package amoussa.sixtakes.Solo.Controller;

import java.awt.event.*;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.text.PlainDocument;

import amoussa.sixtakes.Solo.Model.Player;
import amoussa.sixtakes.Solo.View.*;
/**
 * Listener gérant le choix de la carte sélectionnée pour le joueur local.
 */
public class DeckListener implements MouseListener {

    private Card ca;
    private Player p;
    private static Card last_clicked;

    public DeckListener(Card c, Player p) {
        this.ca = c;
        this.p = p;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (last_clicked != null) {
            last_clicked.setHover(false);
            last_clicked.repaint();
            last_clicked.revalidate();
        }
        System.out.println(ca.getValue());
        this.p.setSelectedCard(ca);
        last_clicked = ca;
        ca.setHover(true);
        ca.repaint();
        ca.revalidate();

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {
        ca.setHover(true);
        ca.repaint();
        ca.revalidate();

    }

    @Override
    public void mouseExited(MouseEvent e) {

        if (this.p.getSelectedCard() != ca) {
            ca.setHover(false);
            ca.repaint();
            ca.revalidate();
        }

    }
}
