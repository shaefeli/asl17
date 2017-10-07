package ch.ethz.asltest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Created by simon on 07.10.17.
 */
public class SocketHandler implements Runnable{
    private Socket clientSocket;
    public SocketHandler(Socket clientSocket){
        this.clientSocket = clientSocket;
    }
    public void run(){
        while(Config.middlewareOn){
            String userInput="";
            int i=0;
            try{
                BufferedReader din = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                while ((userInput = din.readLine()) != null) {
                    System.out.println(userInput);
                    QueueHandler.putToQueue(userInput,clientSocket);
                }
            }catch(Exception e){

            }



        }

    }
}
