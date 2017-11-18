package ch.ethz.asltest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Simon on 06.10.17.
 */
//One request handler object represents one worker thread
public class RequestHandler implements Runnable{
    //variables for statistics
    public static long[] perThreadServiceTimes = new long[Params.nrThreads];
    public static int[] perThreadServiceTimesCount = new int[Params.nrThreads];
    public static AtomicInteger timeInQueueCount = new AtomicInteger(0);
    public static AtomicLong timeInQueue = new AtomicLong(0);

    public static AtomicInteger serviceTimeCount = new AtomicInteger(0);
    public static AtomicLong serviceTime = new AtomicLong(0);

    public static AtomicInteger timeInGetCount = new AtomicInteger(0);
    public static AtomicLong timeInGet = new AtomicLong(0);

    public static AtomicInteger timeInSetCount = new AtomicInteger(0);
    public static AtomicLong timeInSet = new AtomicLong(0);

    public static AtomicInteger timeInMGetCount = new AtomicInteger(0);
    public static AtomicLong timeInMGet = new AtomicLong(0);

    public static AtomicInteger timeInMGetMemCount = new AtomicInteger(0);
    public static AtomicLong timeInMGetMem = new AtomicLong(0);

    public static AtomicInteger parseTimeCount = new AtomicInteger(0);
    public static AtomicLong parseTime = new AtomicLong(0);

    public static AtomicInteger nrGets = new AtomicInteger(0);
    public static AtomicInteger nrSets = new AtomicInteger(0);
    public static AtomicInteger nrMGets = new AtomicInteger(0);

    public static AtomicInteger nrMissesGet = new AtomicInteger(0);

    //varaiables for executing thread
    private PreRequest prerequest;
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
                    System.err.println("Failed to reach "+hostname+" , did you put the correct ip, and start up the servers?");
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
    public RequestHandler(PreRequest prerequest, SocketChannel clientSocket){
        this.request = new Request(RequestType.UNKNOWN);
        this.prerequest = prerequest;
        this.clientSocket = clientSocket;
    }

    //Used to treat a mock init request, where we don't need to send anything back to the client
    public RequestHandler(Request request){
        this.request = request;
    }

    public void run(){
        if(request.requestType != RequestType.INIT){
            long endTimeInqueue = System.nanoTime()-prerequest.startingTimeQueue;
            long startParsingTime = System.nanoTime();
            request = new Request(prerequest.toParse);
            long endParsingTime = System.nanoTime()-startParsingTime;
            parseTime.addAndGet(endParsingTime);
            parseTimeCount.getAndIncrement();
            timeInQueue.getAndAdd(endTimeInqueue);
            timeInQueueCount.getAndIncrement();
        }

       if(Params.verbose){
           System.out.println("Thread number "+Thread.currentThread().getId()+ " handles request :"+request.toString());
       }
        if(request.requestType == RequestType.SET){
            nrSets.getAndIncrement();
            handleSet(request);
            long endServiceTime = System.nanoTime()-request.startServiceTime;
            serviceTime.getAndAdd(endServiceTime);
            serviceTimeCount.getAndIncrement();
            perThreadServiceTimes[(int)Thread.currentThread().getId()%Params.nrThreads] += endServiceTime;
            perThreadServiceTimesCount[(int)Thread.currentThread().getId()%Params.nrThreads] += 1;
        }

        else if(request.requestType == RequestType.GET){
            nrGets.getAndIncrement();
            handleGet(request);
            long endServiceTime = System.nanoTime()-request.startServiceTime;
            serviceTime.getAndAdd(endServiceTime);
            serviceTimeCount.getAndIncrement();
            perThreadServiceTimes[(int)Thread.currentThread().getId()%Params.nrThreads] += endServiceTime;
            perThreadServiceTimesCount[(int)Thread.currentThread().getId()%Params.nrThreads] += 1;
        }
        else if (request.requestType == RequestType.INIT){
            getSockets();   //This is done so that the ThreadLocal gets initialized;
            //Do nothing, it was only to initialize the threads
        }
        else if(request.requestType == RequestType.MGET){
            nrMGets.getAndIncrement();
            handleMultiGet(request);
            long endServiceTime = System.nanoTime()-request.startServiceTime;
            serviceTime.getAndAdd(endServiceTime);
            serviceTimeCount.getAndIncrement();
            perThreadServiceTimes[(int)Thread.currentThread().getId()%Params.nrThreads] += endServiceTime;
            perThreadServiceTimesCount[(int)Thread.currentThread().getId()%Params.nrThreads] += 1;
        }

        //Unknown request
        else if(request.requestType == RequestType.UNKNOWN){
            try{
                System.err.println("Error unknown request, "+request.toString());
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
        long startTimeInSet = System.nanoTime();
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
        long endTimeInSet = System.nanoTime()-startTimeInSet;
        timeInSet.getAndAdd(endTimeInSet);
        timeInSetCount.getAndIncrement();
    }

    private void handleGet(Request request){
        long startTimeInGet = System.nanoTime();
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

            //In this case we are in a miss
            if(newLine.equals("END")){
                nrMissesGet.getAndIncrement();
            }
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
        long endTimeInGet = System.nanoTime()-startTimeInGet;
        timeInGet.addAndGet(endTimeInGet);
        timeInGetCount.getAndIncrement();
    }
    private void handleMultiGet(Request request){
        long startTimeInMGet = System.nanoTime();
        if(!Params.shardedRead){
            long startTimeMemcachedMGet = System.nanoTime();
            handleGet(request);
            long endTimeMemcachedMGet = System.nanoTime()-startTimeMemcachedMGet;
            timeInMGetMem.addAndGet(endTimeMemcachedMGet);
            timeInMGetMemCount.getAndIncrement();
        }
        else {

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

            long startTimeMemcachedMGet = System.nanoTime();
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
            long endTimeMemcachedMGet = System.nanoTime()-startTimeMemcachedMGet;
            timeInMGetMem.addAndGet(endTimeMemcachedMGet);
            timeInMGetMemCount.getAndIncrement();
            result.append("END" +"\r\n");
            try{
                if (Params.verbose) {
                    System.out.println("Sent back to client " + clientSocket.toString() + " from thread " + Thread.currentThread().getId() + " is : " + result.toString());
                }
                byte[] msgToClientNext = result.toString().getBytes();
                ByteBuffer bufferNext = ByteBuffer.wrap(msgToClientNext);
                clientSocket.write(bufferNext);
                bufferNext.clear();
            }catch(Exception e){
                System.err.println("Impossible to send back to client");
                e.printStackTrace();
            }
        }
        long endTimeInMGet = System.nanoTime()-startTimeInMGet;
        timeInMGet.addAndGet(endTimeInMGet);
        timeInMGetCount.getAndIncrement();
    }
    private static List<Socket> getSockets(){
        return initializedServerSockets.get();
    }
}
