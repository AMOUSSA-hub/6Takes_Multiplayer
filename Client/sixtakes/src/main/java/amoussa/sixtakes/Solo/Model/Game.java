package amoussa.sixtakes.Solo.Model;

import java.util.*;
import java.util.concurrent.*;

import javax.swing.JFrame;

import amoussa.sixtakes.Solo.Controller.FoldListener;
import amoussa.sixtakes.Solo.View.Card;
import amoussa.sixtakes.Solo.View.GameBoard;
import amoussa.sixtakes.Solo.View.ResultFrame;

/**
 * Représentation de l'état actuel de la partie (classe principale du modèle).
 */
public class Game extends Timer {

  private List<Player> allPlayers;
  private ArrayList<FoldModel> allFolds;
  private GameBoard gm;
  private int roundRemaining;

  public Game(int nbr_player, GameBoard view) {
    this.gm = view;
    this.allFolds = new ArrayList<>();
    this.allPlayers = new ArrayList<>();
    this.roundRemaining = 10;

    for (int i = 0; i < 4; i++) {
      this.allFolds.add(new FoldModel());
    }

    for (int a = 0; a < nbr_player; a++) {
      this.allPlayers.add(new Player());
    }

    this.Deal();

    view.renderDeckLocalPlayer(allPlayers.get(0).getHand());
    view.renderScores(allPlayers);

    startRound();

  }

  /**
   * Distribution des cartes.
   */
  public void Deal() {
    List<Card> deck = new LinkedList<Card>();
    for (int i = 1; i <= 104; i++) {
      Card c = new Card(i);
      deck.add(c);
    }

    Random r = new Random();
    for (int i = 0; i <= allFolds.size() - 1; i++) {
      Card c = deck.remove(r.nextInt(deck.size()));
      allFolds.get(i).add(c);
      this.gm.getAllFolds().get(i).add(c);
    }

    for (Player p : allPlayers) {

      while (p.getHand().size() != 10) {
        Card c = deck.remove(new Random().nextInt(deck.size()));
        c.setOwner(p);
        p.addCardToHand(c);
      }
      p.printHand();

    }

  }

  /**
   * Getter pour l'ensemble des joueurs de la partie.
   * 
   * @return
   */
  public List<Player> getAllPlayers() {
    return this.allPlayers;
  }

  /**
   * Getter pour toutes les piles(d'un point de vue modèle) de la partie.
   * 
   * @return
   */
  public List<FoldModel> getAllFolds() {
    return this.allFolds;
  }

  /**
   * Getter permettant d'obtenir les cartes sélectionnées par tous les joueurs.
   */
  public void getAllPlays() {
    List<Card> allPlays = new ArrayList<>();
    for (Player p : this.allPlayers) {
      Card c = p.getSelectedCard();

      if (c == null) {
        p.SelectRandomCard();
        c = p.getSelectedCard();
      }

      System.out.println("Le joueur " + (allPlayers.indexOf(p) + 1) + " a joué la carte " + c.getValue());
      allPlays.add(c);
      p.resetCardSelection();

    }

    gm.renderPlays(allPlays);
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    this.makePlays(allPlays);
    this.gm.renderScores(allPlayers);

  }

  /**
   * Commencer un tour.
   */
  public void startRound() {
    schedule(new Round(10, this), 1000, 1000);
  }

  /**
   * Getter pour la vue associée à ce modèle.
   * 
   * @return
   */
  public GameBoard getView() {
    return this.gm;
  }

  /**
   * Gérer les cartes jouées.
   * 
   * @param plays
   */
  public void makePlays(List<Card> plays) {

    while (plays.size() != 0) {

      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      Card c = Collections.min(plays, Comparator.comparingInt(Card::getValue));
      System.out.println(c.getValue());
      this.dispactchCardInFolds(c);
      plays.remove(c);
      this.gm.renderPlays(plays);

    }

    this.roundRemaining--;

    if (roundRemaining != 0) {
      startRound();

    } else {
      System.out.println("la partie est finie");
      this.gm.renderScores(allPlayers);
      new ResultFrame(allPlayers, (JFrame) gm.getTopLevelAncestor());

    }

  }

  /**
   * Placer les cartes dans les piles.
   * 
   * @param c
   * @return
   */
  public boolean dispactchCardInFolds(Card c) {

    List<FoldModel> copyallFolds = new ArrayList<>(this.allFolds);
    int nbrFoldTested = 0;
    boolean cardPlaced = false;
    while (nbrFoldTested != 4 && !cardPlaced) {
      FoldModel f = Collections.max(copyallFolds, Comparator.comparingInt(element -> element.getLast().getValue()));

      if (c.getValue() > f.getLast().getValue()) {

        System.out.println(
            "la carte " + c.getValue() + " qui a été jouée par le joueur " + (getAllPlayers().indexOf(c.getOwner()) + 1)
                + " va aller dans la " + (allFolds.indexOf(f) + 1) + "eme pile");

        if (f.size() == 5) {
          c.getOwner().addMalus(allFolds.get(getAllFolds().indexOf(f)).clearStack());
          this.gm.getAllFolds().get(getAllFolds().indexOf(f)).clearStack();

        }

        allFolds.get(allFolds.indexOf(f)).add(c);
        this.gm.getAllFolds().get(allFolds.indexOf(f)).add(c);
        cardPlaced = true;

      }
      nbrFoldTested++;
      copyallFolds.remove(f);
    }

    if (!cardPlaced) {

      if (this.allPlayers.indexOf(c.getOwner()) == 0) {

        System.out.println("Vous devez faire un choix");
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new FoldSelection(5, this), 0, 1, TimeUnit.SECONDS);
        try {
          Thread.sleep(5500);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        scheduler.shutdown();

        int rankFoldSelected;
        if (FoldListener.getFoldSelected() != null) {

          rankFoldSelected = this.gm.getAllFolds().indexOf(FoldListener.getFoldSelected());
        } else {
          rankFoldSelected = new Random().nextInt(3);
        }

        System.out.println("Vous avez sélectionné le " + (rankFoldSelected + 1)
            + "eme plie et vous récolter donc un malus de " + allFolds.get(rankFoldSelected).getSumMalus() + " points");

        c.getOwner().addMalus(allFolds.get(rankFoldSelected).clearStack());
        this.gm.getAllFolds().get(rankFoldSelected).clearStack();

        allFolds.get(rankFoldSelected).add(c);
        this.gm.getAllFolds().get(rankFoldSelected).add(c);

      } else {

        FoldModel f = Collections.min(allFolds, Comparator.comparingInt(element -> element.getSumMalus()));

        int rankFoldSelected = allFolds.indexOf(f);

        System.out.println(
            "Le joueur " + (allPlayers.indexOf(c.getOwner()) + 1) + " a pris la pile " + (rankFoldSelected + 1));

        c.getOwner().addMalus(f.clearStack());
        this.gm.getAllFolds().get(rankFoldSelected).clearStack();
        allFolds.get(rankFoldSelected).add(c);
        this.gm.getAllFolds().get(rankFoldSelected).add(c);

      }

    }

    return true;
  }

}
