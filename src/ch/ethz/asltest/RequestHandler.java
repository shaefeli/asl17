package ch.ethz.asltest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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
    private int nrServers = Config.nrServers;
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
        //long threadId = Thread.currentThread().getId();
        //System.out.println("Thread id:"+threadId);
        getSockets();
        //System.out.println(request.toString());
        if(request.requestType == 4){
            handleMultiGet(request);
        }

        else if(request.requestType == 1){
            handleSet(request);
        }
        else if(request.requestType == 2){
            handleGet(request);
        }
        else if(request.requestType == 0){
            try{
                PrintWriter outToClient = new PrintWriter(clientSocket.getOutputStream(), true);
                outToClient.println("UNKNOWN");
            }catch(Exception e){
                System.out.println("Impossible to get outputstream");
            }

        }
        //If request is init (3) do nothing



    }
    private void handleSet(Request request){
        for(int i=0; i<nrServers;i++) {
            Socket serverSocket = getSockets().get(i);
            try {
                PrintWriter out = new PrintWriter(serverSocket.getOutputStream(), true);
                System.out.println(request.toString());
                out.println(request.toString());
                BufferedReader din = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
                String serverInput = "";
                //Loop until you get an answer
                while ((serverInput = din.readLine()) == null);
                PrintWriter outToClient = new PrintWriter(clientSocket.getOutputStream(), true);
                outToClient.println(serverInput);

            }catch(Exception e){
                System.out.println("Impossible to write in socket");
            }
            System.out.println("this is i:"+i);
            System.out.println("nr of servers: "+nrServers);
        }
    }
    private void handleGet(Request request){
        Socket serverSocket = getSockets().get(0);
        try{
            PrintWriter out = new PrintWriter(serverSocket.getOutputStream(), true);
            System.out.println(request.toString());
            out.println(request.toString());
            BufferedReader din = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
            String serverInput = "";
            //Loop until you get an answer
            while ((serverInput = din.readLine()) == null);
            PrintWriter outToClient = new PrintWriter(clientSocket.getOutputStream(), true);
            outToClient.println(serverInput);
        }catch(Exception e){
            System.out.println("Impossible to open outputstream");
        }

    }
    private void handleMultiGet(Request request){
        Socket serverSocket = getSockets().get(0);
        try{
            PrintWriter out = new PrintWriter(serverSocket.getOutputStream(), true);
            System.out.println(request.toString());
            out.println(request.toString());
            BufferedReader din = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
            String serverInput = "";
            //Loop until you get an answer
            while ((serverInput = din.readLine()) == null);
            PrintWriter outToClient = new PrintWriter(clientSocket.getOutputStream(), true);
            outToClient.println(serverInput);
        }catch(Exception e){
            System.out.println("Impossible to open outputstream");
        }
    }
    private static List<Socket> getSockets(){
        return initializedServerSockets.get();
    }
}
