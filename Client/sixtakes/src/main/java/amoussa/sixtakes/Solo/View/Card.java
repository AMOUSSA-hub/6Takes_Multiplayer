package amoussa.sixtakes.Solo.View;

import javax.swing.*;
import java.awt.*;

import amoussa.sixtakes.Solo.Model.Player;
import amoussa.sixtakes.Utils.Icone;

/**
 * une carte (représentation graphique)
 */
public class Card extends JComponent {

    private int malus;
    private int value;
    private boolean hover = false;
    private Player owner;

    public Card(int v) {
        this.value = v;
        setMinimumSize(new Dimension(50, 70));
        setPreferredSize(new Dimension(50, 70));

        if (v % 11 == 0) {
            if (v == 55) {
                this.malus = 7;
            } else {
                this.malus = 5;
            }
        }

        else if (v % 10 == 0) {
            this.malus = 3;
        }

        else if (v % 5 == 0) {
            this.malus = 2;
        }

        else {
            this.malus = 1;
        }

    }

    /**
     * Getter pour le malus associé à cette carte.
     * 
     * @return
     */
    public int getMalus() {
        return malus;
    }

    /**
     * Getter pour la valeur de la carte.
     * 
     * @return
     */
    public int getValue() {
        return value;
    }

    /**
     * Setter pour le propriétaire de la carte.
     * 
     * @param p
     */
    public void setOwner(Player p) {
        this.owner = p;
    }

    /**
     * Setter pour le propriétaire de la carte.
     * 
     * @return
     */
    public Player getOwner() {
        return this.owner;
    }

    @Override
    protected void paintComponent(Graphics g) {
        
        Graphics p = g.create();
        Graphics2D g2d = (Graphics2D) g;
        if (this.isOpaque()) {
            p.setColor(this.getBackground());
            p.fillRect(0, 0, this.getWidth(), this.getHeight());
        }

        p.setColor(Color.white);
        p.fillRoundRect(0, 0, (int) Math.round(this.getWidth()), (int) Math.round(this.getHeight()), 15, 15);

        p.setColor(Color.BLACK);
        p.drawString(this.value + "", (int) Math.round(this.getWidth() * 0.05) + 5,
                (int) Math.round(this.getHeight() * 0.1) + 5);
        for (int a = 0; a < this.malus; a++) {
            p.drawImage(Icone.bull, (int) Math.round(this.getWidth() * 0.01 + a * 10) - 5,
                    (int) Math.round((this.getHeight() * 0.1) + 10), 20, 20, null);
        }

        if (this.hover) {
            g2d.setStroke(new BasicStroke(5.0f));
            g2d.setColor(Color.BLACK);
            g2d.drawRoundRect(0, 0, (int) Math.round(this.getWidth()), (int) Math.round(this.getHeight()), 15, 15);
        }
    }

    /**
     * Setter pour la position de la souris par rapport à la carte (dessus ou non).
     * 
     * @param b
     */
    public void setHover(boolean b) {
        this.hover = b;

    }

    public static boolean switchApplied;

    /**
     * Algorithme de tri croissant des cartes
     * 
     * @param cards
     * @param i
     * @return
     */
    public static Card[] bubbleSort(Card[] cards, int i) {

        System.out.println("appel recursif");
        if (i == 0) {
            switchApplied = false;
        }

        if (cards[i].getValue() > cards[i + 1].getValue()) {
            Card stock = cards[i];
            cards[i] = cards[i + 1];
            cards[i + 1] = stock;

            if (!switchApplied) {
                switchApplied = true;
            }
        }

        if (i < cards.length - 2) {
            return bubbleSort(cards, i + 1);
        } else {

            if (switchApplied) {
                return bubbleSort(cards, 0);
            }
        }

        if (switchApplied) {
            switchApplied = false;
        }
        return cards;
    }
}
