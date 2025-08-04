package amoussa.sixtakes.Solo.Model;

import java.util.*;

import amoussa.sixtakes.Solo.View.Card;

/**
 * Une Pile ( du point de vue du modèle).
 */
public class FoldModel extends LinkedList<Card> {
    /**
     * Vider une pile.
     * 
     * @return
     */
    public int clearStack() {
        int malus = getSumMalus();
        this.clear();
        return malus;
    }

    /**
     * Getter pour le malus total d'une pile.
     * 
     * @return
     */
    public int getSumMalus() {
        int sum = 0;
        for (Card c : this) {
            sum += c.getMalus();
        }
        return sum;
    }

    /**
     * Ajouter une carte à la pile.
     */
    @Override
    public boolean add(Card e) {
        return super.add(e);
    }

}
