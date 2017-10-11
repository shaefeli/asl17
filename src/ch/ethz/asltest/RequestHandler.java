package ch.ethz.asltest;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Simon on 06.10.17.
 */
//The request handler is one working thread
public class RequestHandler implements Runnable{
    private Request request;
    private Socket clientSocket;
    private static final ThreadLocal<List<Socket>> initializedServerSockets = new ThreadLocal<List<Socket>>(){
        @Override
        public List<Socket> initialValue(){
            List<Socket> serverSockets = new ArrayList<>();


            for(String mcAdress: Config.mcAdresses){
                int port = getPort(mcAdress);
                String hostname = getHostname(mcAdress);
                //System.out.println(port);
                //System.out.println(hostname);
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
            char readChar;
            StringBuilder port = new StringBuilder();
            boolean passedSemiColumm = false;

            for(int i =0; i<mcAdress.length();i++){
                readChar = mcAdress.charAt(i);
                if(passedSemiColumm){
                    port.append(readChar);
                }
                if(readChar == ':'){
                    passedSemiColumm = true;
                }
            }

            int portN = 0;
            try{
                portN =Integer.parseInt(port.toString());
            }
            catch(NumberFormatException e){
                System.out.println("Error converting port to a number "+ port);
            }
            return portN;

        }
        private String getHostname(String mcAdress){


            char readChar;
            StringBuilder hostname = new StringBuilder();
            boolean passedSemiColumm = false;
            for(int i=0; i<mcAdress.length();i++){
                readChar = mcAdress.charAt(i);
                if(readChar == ':'){
                    passedSemiColumm = true;
                }
                if(!passedSemiColumm){
                    hostname.append(readChar);
                }
            }

            return hostname.toString();
        }
    };


    public RequestHandler(Request request, Socket clientSocket){
        this.request = request;
        this.clientSocket = clientSocket;
    }

    public void run(){
        long threadId = Thread.currentThread().getId();
        //System.out.println("Thread id:"+threadId);
        getSockets();
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
