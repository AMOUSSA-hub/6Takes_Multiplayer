package amoussa.sixtakes.Solo.View;
import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JButton;

public class HomeButton extends JButton{

    private  String text;
    public HomeButton(String text){
        super(text);
        this.text = text;


    }



    


@Override
protected void paintComponent(Graphics g) {
    // TODO Auto-generated method stub
    g.setColor(new Color(80,0,171));
    g.drawRoundRect(0, 0, this.getWidth(), this.getHeight(), 20,20);
    g.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 20,20);
    g.setColor(Color.white);
    g.drawString(this.text,this.getWidth()/5,this.getHeight()*3/4);


}


}