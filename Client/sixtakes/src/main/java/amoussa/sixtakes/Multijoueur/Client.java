package amoussa.sixtakes.Multijoueur;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
    private Socket sock;
    private String pseudo;


    public Client(String address,int port, String pseudo){

        this.pseudo = pseudo;
         try {
             sock = new Socket(address,port);

            System.out.println("La connexion est établie");
            System.out.println(sock.toString());

            
                           
                    Thread t =new Thread(new Receiver(this));
                    t.start();
                    sendMessage(FormattedMessage.serverConnexion(pseudo));

                     
                    
                

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {

            e.printStackTrace();
        }
        

    }

    public void sendMessage(String msg) throws IOException{

        OutputStream output = this.sock.getOutputStream();

            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(output)); 


                            bw.write(this.pseudo+": "+msg);
                            bw.newLine();
                            bw.flush();
                            bw.close();
                            output.close();


    }

    public static void main(String[] args) {
        new Client(args[0], Integer.parseInt(args[1]),args[2]);
    }
    
}
class Receiver implements Runnable{
    private Client c;

    public Receiver(Client c){
        this.c = c;
    }


    @Override
    public void run() {
        


        try {
         
            ServerSocket ss = new ServerSocket(2023);
           


            String line ="";
            while(true){
                Socket sock = ss.accept();
                  BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                        while((line=br.readLine())!= null){
                            if(line != null){System.out.println(line);}

                        }
                        System.out.println("connexion fermé");
                    
                    br.close();
                    sock.close();
            }
        } catch (IOException e) {
            
           System.out.println("Vous avez été déconnecté du serveur !");
        }
        finally{
            System.out.println("blabla");
        }
    
    }
}