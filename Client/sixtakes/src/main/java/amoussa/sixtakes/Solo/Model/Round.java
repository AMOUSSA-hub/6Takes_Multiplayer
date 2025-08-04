package amoussa.sixtakes.Solo.Model;

import java.util.*;
import amoussa.sixtakes.Solo.Controller.FoldListener;
import amoussa.sixtakes.Solo.View.Card;

/**
 * Tâche de sélection de la carte à jouer (limité dans le temps)
 */
public class Round extends TimerTask {

  public Card c;
  public int time;
  public Game g;

  Round(int t, Game g) {
    this.time = t;
    this.g = g;
    System.out.println("Commencement nouveau round");

  }

  @Override
  public void run() {

    if (!g.getView().isPaused()) {
      this.g.getView().renderChrono("Il vous reste " + this.time + " secondes pour jouer");
      if (this.time == 0) {
        this.g.getView().renderChrono("");
        this.g.getAllPlays();
        cancel();
      }
      this.time--;

    }

  }
}
