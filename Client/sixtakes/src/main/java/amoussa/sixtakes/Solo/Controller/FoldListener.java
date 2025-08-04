package amoussa.sixtakes.Solo.Controller;

import java.awt.event.*;
import java.util.*;

import amoussa.sixtakes.Solo.View.*;
/**
 * Listener gérant la sélection des 4 piles.
 */
public class FoldListener implements MouseListener{
   
    private static boolean selectable = false;
    private Fold f;
    private static Fold foldSelected;
    private static List <Fold> folds = new ArrayList<>();

    public FoldListener( Fold f){
        this.f = f;

       folds.add(f);

    }

    @Override
    public void mouseClicked(MouseEvent e) {


        
    }

    @Override
    public void mousePressed(MouseEvent e) {

                if(selectable){
        
            if(foldSelected != null){
            foldSelected.setHover(false);
            foldSelected.repaint();
            foldSelected.revalidate();
            }
            foldSelected= this.f;
            foldSelected.setHover(true);
            foldSelected.repaint();
            foldSelected.revalidate();
        }
        
       
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        if(selectable){
            f.setHover(true);
            f.repaint();
            f.revalidate();
        }
       
    }

    @Override
    public void mouseExited(MouseEvent e) {
         if(selectable){
            if(this.f != foldSelected){
                f.setHover(false);
                f.repaint();
                f.revalidate();
            }
         }


    }

    public static void setSelectable(boolean s){
        selectable = s;
        if(!s){
            
            for(Fold fold : folds){
            fold.setHover(false);
            fold.repaint();
            fold.revalidate();
        
            }
        }
    }

    public static Fold getFoldSelected() {
        return foldSelected;
    }
    
    
}
