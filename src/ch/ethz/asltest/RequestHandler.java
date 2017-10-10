package ch.ethz.asltest;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Simon on 06.10.17.
 */
//The request handler is one working thread
public class RequestHandler implements Runnable{
    private String message;
    private Socket clientSocket;
    private static final ThreadLocal<List<Socket>> initializedServerSockets = new ThreadLocal<List<Socket>>(){
        @Override
        public List<Socket> initialValue(){
            List<Socket> serverSockets = new ArrayList<>();

            //for(String mcAdress: Config.mcAdresses){
            List<String> mock = new ArrayList<>();
            mock.add(" fdsa");
            for(String mcAdress: mock){
                int port = getPort(mcAdress);
                String hostname = getHostname(mcAdress);
                try{
                    Socket serverSocket = new Socket(hostname, port);
                    serverSockets.add(serverSocket);
                    System.out.println("okay");
                }catch(Exception e){
                    System.out.println("Failed to reach "+hostname+" , did you put the ip ? ");
                }

            }
            return serverSockets;
        }
        private int getPort (String mcAdress){
            return 3000;
        }
        private String getHostname(String mcAdress){
            return "127.0.0.1";
        }
    };


    public RequestHandler(String message, Socket clientSocket){
        this.message = message;
        this.clientSocket = clientSocket;
    }

    public void run(){
        long threadId = Thread.currentThread().getId();
        System.out.println("Thread id:"+threadId);
        getSockets();

        Request request = new Request(message.toString());
        System.out.println(request.toString());

        //if multi get => disperse it correctly (other class)
        //Send request to servers
        //Wait for answer
        //Interpret the answer
        //Send back to client via the socket
    }
    private static List<Socket> getSockets(){
        return initializedServerSockets.get();
    }
}
