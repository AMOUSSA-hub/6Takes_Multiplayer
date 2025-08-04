package amoussa.sixtakes.Solo.View;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import amoussa.sixtakes.Solo.Controller.FoldListener;

/**
 * -
 * Une pile de carte (representation graphique).
 */
public class Fold extends JPanel {

    private boolean hover = false;
    private GridLayout gl;

    Fold() {
        this.setOpaque(false);
        this.addMouseListener(new FoldListener(this));
        this.gl = new GridLayout(1, 1, 0, 5);
        this.setLayout(gl);
    }

    /**
     * Setter pour la position de la souris par rapport à la pile (dessus ou non).
     * 
     * @param hover
     */
    public void setHover(boolean hover) {
        this.hover = hover;
    }

    /**
     * Vider la pile.
     */
    public void clearStack() {
        this.removeAll();
        this.gl.setRows(1);
    }

    /**
     * Ajouter une carte à la pile.
     */
    @Override
    public Component add(Component comp) {

        Card c = (Card) comp;
        this.gl.setRows(this.getComponentCount() + 1);
        return super.add(c);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (hover) {
            this.setBorder(new LineBorder(Color.BLACK, 5, true));
        } else {
            this.setBorder(null);
        }
    }

}
