package amoussa.sixtakes.Solo.Model;

import java.util.*;
import amoussa.sixtakes.Solo.Controller.FoldListener;
import amoussa.sixtakes.Solo.View.Card;

/**
 * Tâche de sélection du pli à prendre (limite de temps).
 */
public class FoldSelection extends TimerTask {

  public Card c;
  public int time;
  public Game g;

  FoldSelection(int t, Game g) {
    this.time = t;
    this.g = g;
    FoldListener.setSelectable(true);

  }

  @Override
  public void run() {

    // if (!g.isPaused()) {
    this.g.getView().renderChrono("<html>Choisissez une pile <br>" + this.time + "</html>");
    if (this.time == 0) {
      this.g.getView().renderChrono("");
      FoldListener.setSelectable(false);
      cancel();
    }

    this.time--;
    // }

  }
}
