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
//One request handler object represents one worker thread
public class RequestHandler implements Runnable{
    private Request request;
    private SocketChannel clientSocket;
    private static int serverToSendGet = 0;    //The round-robin for handling the get requests
    private int nrServers = Params.nrServers;

    //This initialization is done only the first time the thread is called
    private static final ThreadLocal<List<Socket>> initializedServerSockets = new ThreadLocal<List<Socket>>(){
        @Override
        public List<Socket> initialValue(){
            List<Socket> serverSockets = new ArrayList<>();
            for(String mcAdress: Params.mcAdresses){
                int port = getPort(mcAdress);
                String hostname = getHostname(mcAdress);
                try{
                    Socket serverSocket = new Socket(hostname, port);
                    serverSockets.add(serverSocket);
                    if(Params.verbose){
                        System.out.println("Just opened a socket to the server "+serverSocket.toString()+ "from thread number "+ Thread.currentThread().getId());
                    }
                }catch(Exception e){
                    System.err.println("Failed to reach "+hostname+" , did you put the correct ip, and start uo the servers?");
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

    //Used to treat normal requests
    public RequestHandler(Request request, SocketChannel clientSocket){
        this.request = request;
        this.clientSocket = clientSocket;
    }

    //Used to treat a mock init request, where we don't need to send anything back to the client
    public RequestHandler(Request request){
        this.request = request;
    }

    public void run(){
       if(Params.verbose){
           System.out.println("Thread number "+Thread.currentThread().getId()+ " handles request :"+request.toString());
       }

        if(request.requestType == RequestType.SET){
            handleSet(request);
        }

        else if(request.requestType == RequestType.GET){
            handleGet(request);
        }
        else if (request.requestType == RequestType.INIT){
            getSockets();   //This is done so that the ThreadLocal gets initialized;
            //Do nothing, it was only to initialize the threads
        }
        else if(request.requestType == RequestType.MGET){
            handleMultiGet(request);
        }

        //Unknown request
        else if(request.requestType == RequestType.UNKNOWN){
            try{
                if(Params.verbose){
                    System.out.println("Error unknown request, "+request.toString());
                }
                String lineToSend = "UNKNOWN";
                byte[] messageToClient = lineToSend.getBytes();
                ByteBuffer buffer = ByteBuffer.wrap(messageToClient);
                clientSocket.write(buffer);
                buffer.clear();
            }catch(Exception e){
                System.err.println("Impossible send back to client");
                e.printStackTrace();
            }

        }
    }
    private void handleSet(Request request){
        String resultForAll = "STORED";     //one result to send for all servers
        List<Socket> serverSockets = getSockets();

        //Send set to every server
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

        //Receive answer from every server
        for(int i=0; i<nrServers;i++) {
            try{
                Socket serverSocket = serverSockets.get(i);
                BufferedReader din = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
                String serverInput;

                while ((serverInput = din.readLine()) == null){this.wait();}
                //If there is one error for one server, the outputed result for all the servers is this error
                if(!serverInput.equals("STORED")){
                    resultForAll = serverInput;
                }

            }catch(Exception e){
                System.err.println("Impossible to read from server socket in set request");
            }
        }

        //Send answer back to client
        try {
            if(Params.verbose){
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
        serverToSendGet = (serverToSendGet+1)%nrServers;    //This is the round robin (globally for every thread)
        try{

            //Send the request
            PrintWriter out = new PrintWriter(serverSocket.getOutputStream(), true);
            out.println(request.toString());

            //Get the answer back
            BufferedReader din = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
            String newLine;
            StringBuilder totalToSendBack = new StringBuilder();

            while ((newLine = din.readLine()) == null){this.wait();}

            totalToSendBack.append(newLine);
            totalToSendBack.append("\r\n");
            while(!newLine.equals("END")){
                newLine = din.readLine();
                totalToSendBack.append(newLine);
                totalToSendBack.append("\r\n");
            }
            if(Params.verbose){
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
        if(!Params.shardedRead){
            handleGet(request);
        }
        else {
            System.out.println("Welcome to multi get");

            //Share the requests
            int nrKeysPerServer = request.keys.size()/ Params.nrServers;
            int leftKeysToShare = request.keys.size()-nrKeysPerServer* Params.nrServers;
            Request[] requests = new Request[Params.nrServers];
            int start = 0;
            int end = nrKeysPerServer;
            for(int i = 0; i< Params.nrServers; i++){
                if(i == Params.nrServers-1){
                    end = request.keys.size();
                }
                if(leftKeysToShare > 0){
                    end += 1;
                    requests[i] = new Request(4,request.keys.subList(start,end));
                    leftKeysToShare--;
                }
                else{
                    requests[i] = new Request(4,request.keys.subList(start,end));
                }

                start = end;
                end+=nrKeysPerServer;
            }

            //Send to all servers
            List<Socket> serverSockets = getSockets();
            for(int i=0; i<nrServers;i++) {
                Socket serverSocket = serverSockets.get(i);
                try {

                    System.out.println(requests[i].toString());
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
                    String newLine;
                    StringBuilder ToSendBackOneServer = new StringBuilder();
                    //Loop until you get an answer
                    while ((newLine = din.readLine()) == null){this.wait();}
                    if(!newLine.equals("END")) {
                        ToSendBackOneServer.append(newLine);
                        ToSendBackOneServer.append("\r\n");
                    }
                    while(!newLine.equals("END")){
                        newLine = din.readLine();
                        if(!newLine.equals("END")) {
                            ToSendBackOneServer.append(newLine);
                            ToSendBackOneServer.append("\r\n");
                        }
                    }
                    result.append(ToSendBackOneServer.toString());
                }catch(Exception e){
                    System.err.println("Impossible to read from server socket in set");
                }
            }
            result.append("END" +"\r\n");
            try{
                if (Params.verbose) {
                    System.out.println("Sent back to client " + clientSocket.toString() + " from thread " + Thread.currentThread().getId() + " is : " + result.toString());
                }
                byte[] msgToClientNext = result.toString().getBytes();
                ByteBuffer bufferNext = ByteBuffer.wrap(msgToClientNext);
                System.out.println(bufferNext.position());
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
