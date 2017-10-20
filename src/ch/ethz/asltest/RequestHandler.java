package ch.ethz.asltest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Simon on 06.10.17.
 */
//The request handler is one working thread
public class RequestHandler implements Runnable{
    private Request request;
    private SocketChannel clientSocket;
    private static int serverToSendGet = 0;
    private int nrServers = Config.nrServers;
    private static final ThreadLocal<List<Socket>> initializedServerSockets = new ThreadLocal<List<Socket>>(){
        @Override
        public List<Socket> initialValue(){
            List<Socket> serverSockets = new ArrayList<>();
            for(String mcAdress: Config.mcAdresses){
                int port = getPort(mcAdress);
                String hostname = getHostname(mcAdress);
                try{
                    Socket serverSocket = new Socket(hostname, port);
                    serverSockets.add(serverSocket);
                    if(Config.verbose){
                        System.out.println("Just opened a socket to the server "+serverSocket.toString()+ "from thread number "+ Thread.currentThread().getId());
                    }
                }catch(Exception e){
                    System.err.println("Failed to reach "+hostname+" , did you put the ip ? ");
                    e.printStackTrace();
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


    public RequestHandler(Request request, SocketChannel clientSocket){
        this.request = request;
        this.clientSocket = clientSocket;
    }
    public RequestHandler(Request request){
        this.request = request;
    }

    public void run(){
       if(Config.verbose){
           System.out.println("Thread number "+Thread.currentThread().getId()+ " handles request :"+request.toString());
       }
        getSockets();
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
                if(Config.verbose){
                    System.out.println("Error unknown request");
                }

                String lineToSend = "UNKNOWN";
                byte[] messageToClient = lineToSend.getBytes();
                ByteBuffer buffer = ByteBuffer.wrap(messageToClient);
                clientSocket.write(buffer);
                buffer.clear();
            }catch(Exception e){
                System.err.println("Impossible to get outputstream");
                e.printStackTrace();
            }

        }
        //If request is init (3) do nothing


    }
    private void handleSet(Request request){
        String resultForAll = "STORED";
        List<Socket> serverSockets = getSockets();
        for(int i=0; i<nrServers;i++) {
            Socket serverSocket = serverSockets.get(i);
            try {
                PrintWriter out = new PrintWriter(serverSocket.getOutputStream(), true);
                String toSend = request.toString()+"\r";
                out.println(toSend);

            }catch(Exception e){
                System.err.println("Impossible to write to server socket in set");
            }
        }
        for(int i=0; i<nrServers;i++) {
            try{
                Socket serverSocket = serverSockets.get(i);
                BufferedReader din = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
                String serverInput = "";
                //Loop until you get an answer
                while ((serverInput = din.readLine()) == null){this.wait();}
                if(!serverInput.equals("STORED")){
                    resultForAll = serverInput;
                }

            }catch(Exception e){
                System.err.println("Impossible to read from server socket in set");
            }
        }

        try {
            if(Config.verbose){
                System.out.println("Sent back to client "+clientSocket.toString()+ " from thread "+Thread.currentThread().getId()+ " is : "+resultForAll);
            }
            resultForAll+="\r\n";
            byte[] messageToClient = resultForAll.getBytes();
            ByteBuffer buffer = ByteBuffer.wrap(messageToClient);
            clientSocket.write(buffer);
            buffer.clear();

        }catch(Exception e){
            System.err.println("Failed to send result back to client ");
            e.printStackTrace();
        }
    }
    private void handleGet(Request request){
        Socket serverSocket = getSockets().get(serverToSendGet);
        serverToSendGet = serverToSendGet%nrServers;
        try{
            PrintWriter out = new PrintWriter(serverSocket.getOutputStream(), true);
            out.println(request.toString());

            BufferedReader din = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
            String newLine = "";
            StringBuilder totalToSendBack = new StringBuilder();
            //Loop until you get an answer
            while ((newLine = din.readLine()) == null){this.wait();}

            totalToSendBack.append(newLine+"\r\n");
            while(!newLine.equals("END")){
                newLine = din.readLine();

                totalToSendBack.append(newLine+"\r\n");
            }
            if(Config.verbose){
                System.out.println("Sent back to client "+clientSocket.toString()+ " from thread "+Thread.currentThread().getId()+ " is : "+totalToSendBack.toString());
            }
            byte[] msgToClientNext = totalToSendBack.toString().getBytes();
            ByteBuffer bufferNext = ByteBuffer.wrap(msgToClientNext);
            clientSocket.write(bufferNext);
            bufferNext.clear();

        }catch(Exception e){
            System.err.println("Failed to send result back to client ");
            e.printStackTrace();
        }

    }
    private void handleMultiGet(Request request){
        if(!Config.shardedRead){
            handleGet(request);
        }
        else {
            //Share the requests

            int nrKeysPerServer = request.keys.size()/Config.nrServers;
            Request[] requests = new Request[Config.nrServers];
            int start = 0;
            int end = nrKeysPerServer;
            for(int i=0; i<Config.nrServers; i++){
                if(i == Config.nrServers-1){
                    end = request.keys.size();
                }
                requests[i] = new Request(4,request.keys.subList(start,end));
                start = end;
                end+=nrKeysPerServer;
            }

            //Send to all servers
            List<Socket> serverSockets = getSockets();
            for(int i=0; i<nrServers;i++) {
                Socket serverSocket = serverSockets.get(i);
                try {
                    PrintWriter out = new PrintWriter(serverSocket.getOutputStream(), true);
                    out.println(requests[i].toString());

                }catch(Exception e){
                    System.err.println("Impossible to write to server socket in multi get");
                }
            }



            //Read from all servers and reassemble in result
            StringBuilder result = new StringBuilder();
            for(int i=0; i<nrServers;i++) {
                Socket serverSocket = serverSockets.get(i);
                try{
                    BufferedReader din = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
                    String newLine = "";
                    StringBuilder ToSendBackOneServer = new StringBuilder();
                    //Loop until you get an answer
                    while ((newLine = din.readLine()) == null){this.wait();}

                    if(Config.verbose){
                        System.out.println("Sent back to client "+clientSocket.toString()+ " from thread "+Thread.currentThread().getId()+ " is : "+newLine);
                    }
                    ToSendBackOneServer.append(newLine+"\r\n");
                    while(!newLine.equals("END")){
                        newLine = din.readLine();
                        if(!newLine.equals("END")) {
                            if (Config.verbose) {
                                System.out.println("Sent back to client " + clientSocket.toString() + " from thread " + Thread.currentThread().getId() + " is : " + newLine);
                            }
                            ToSendBackOneServer.append(newLine + "\r\n");
                        }
                    }
                    result.append(ToSendBackOneServer.toString());
                }catch(Exception e){
                    System.err.println("Impossible to read from server socket in set");
                }
            }
            result.append("END+\r\n");
            try{
                byte[] msgToClientNext = result.toString().getBytes();
                ByteBuffer bufferNext = ByteBuffer.wrap(msgToClientNext);
                clientSocket.write(bufferNext);
                bufferNext.clear();
            }catch(Exception e){
                System.err.println("Impossible to send back to client");
                e.printStackTrace();
            }



        }
    }
    private static List<Socket> getSockets(){
        return initializedServerSockets.get();
    }
}
