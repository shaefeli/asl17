package ch.ethz.asltest;

import java.io.IOException;
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
            if(Config.verboseLevel>0){
                System.err.println("Failed to create initial Server Socket");
                e.printStackTrace();
            }

        }
        Config.mcAdresses = mcAddresses;
        queueHandler = new QueueHandler(numThreadsPTP);

        //Initialize every thread
        Request request = new Request(3);
        for(int i = 0; i<numThreadsPTP;i++){
            QueueHandler.putToQueue(request,new Socket());
        }
    }
    public void run(){
        while (Config.middlewareOn) {
            try {
                //System.out.println("Waiting for client on port " +welcomeSocket.getLocalPort() + "...");

                //Wait for a client to create a TCP connection
                Socket clientSocket = welcomeSocket.accept();
                System.out.println(clientSocket.toString());
                //System.out.println("Just connected to " + clientSocket.getRemoteSocketAddress());

                //Give socket to handler
                new Thread(new SocketHandler(clientSocket)).start();


            }catch (IOException e1) {
                if(Config.verboseLevel>0){
                    System.err.println("Connection to client impossible");
                }

            }
        }
        this.teardown();
    }

    public void teardown() {
        if (welcomeSocket != null) {
            try {
                welcomeSocket.close();
            }catch(Exception e){
                if(Config.verboseLevel>0){
                    e.printStackTrace();
                }
            }
            welcomeSocket = null;
        }
    }

}
