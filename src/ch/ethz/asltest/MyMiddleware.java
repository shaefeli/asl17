package ch.ethz.asltest;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

/**
 * Created by Simon on 29.09.17.
 */
public class MyMiddleware implements Runnable{
    private ServerSocket welcomeSocket;
    private QueueHandler queueHandler;
    public MyMiddleware(String ip, int port, List<String> mcAddresses, int numThreadsPTP, boolean readSharded){

        try{
            welcomeSocket = new ServerSocket(port);
        } catch(Exception e) {
            System.out.println("Failed to create initial Server Socket");
            e.printStackTrace();
        }

        queueHandler = new QueueHandler(numThreadsPTP);
    }
    public void run(){
        while (Config.middlewareOn) {
            try {
                System.out.println("Waiting for client on port " +
                        welcomeSocket.getLocalPort() + "...");

                //Wait for a client to create a TCP connection
                Socket clientSocket = welcomeSocket.accept();
                System.out.println("Just connected to " + clientSocket.getRemoteSocketAddress());

                // Receive TCP packet and give it to the queue Handler
                BufferedReader din = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String message = din.readLine();
                System.out.println("Received: " + message);
                queueHandler.putToQueue(message,clientSocket);


            }catch (IOException e1) {
                System.out.println("Connection to client impossible");
            }
        }
        this.teardown();
    }


    public void teardown() {
        if (welcomeSocket != null) {
            try {
                welcomeSocket.close();
            }catch(Exception e){
                e.printStackTrace();
            }
            welcomeSocket = null;
        }
    }

}
