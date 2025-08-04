package amoussa.sixtakes.Solo.Model;

import java.util.*;

import amoussa.sixtakes.Solo.Controller.DeckListener;
import amoussa.sixtakes.Solo.View.*;

/**
 * Un joueur.
 */
public class Player {

  private List<Card> hand;
  private int malus;
  private Card selectedCard;

  public Player() {
    this.malus = 0;
    this.hand = new ArrayList<Card>();
  }

  /**
   * Ajouter une carte à la main du joueur.
   * 
   * @param c
   */
  public void addCardToHand(Card c) {
    c.addMouseListener(new DeckListener(c, this));
    this.hand.add(c);
  }

  /**
   * Enlever une carte de la main du joueur.
   * 
   * @param c
   * @return
   */
  public Card removeCardFromHand(Card c) {

    this.hand.remove(c);

    return c;
  }

  /**
   * Afficher la main du joueur (dans le terminal).
   */
  public void printHand() {
    System.out.printf("[ ");
    for (Card c : this.hand) {

      System.out.printf("%d ,", c.getValue());

    }
    System.out.println("] ");
  }

  /**
   * Getter pour l'ensemble des cartes d'un joueur.
   * 
   * @return
   */
  public List<Card> getHand() {
    return this.hand;
  }

  /**
   * Setter pour la carte que le joueur souhaite jouer.
   * 
   * @param sc
   */
  public void setSelectedCard(Card sc) {
    this.selectedCard = sc;
  }

  /**
   * Getter pour la carte que le joueur à jouée.
   * 
   * @return
   */
  public Card getSelectedCard() {
    Card c = this.selectedCard;
    return removeCardFromHand(c);
  }

  /**
   * Sélectionner une carte au hasard dans la main du joueur.
   */
  public void SelectRandomCard() {
    Random r = new Random();

    if (hand.size() != 1) {
      setSelectedCard(hand.get(r.nextInt(hand.size() - 1)));
    } else {
      setSelectedCard(hand.get(0));
    }

  }

  /**
   * Getter pour le score du joueur.
   * 
   * @return
   */
  public int getMalus() {
    return this.malus;
  }

  /**
   * Setter pour le score du joueur.
   * 
   * @param m
   */
  public void addMalus(int m) {
    this.malus += m;
  }

  /**
   * réinitialiser la sélection du joueur.
   */
  public void resetCardSelection() {
    this.selectedCard.setHover(false);
    this.selectedCard.repaint();
    this.selectedCard.revalidate();
    this.selectedCard = null;
  }

}
