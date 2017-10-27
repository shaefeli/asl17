package ch.ethz.asltest;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Simon on 29.09.17.
 */
public class MyMiddleware implements Runnable{
    //For statistics
    public static ConcurrentHashMap forSetParse = new ConcurrentHashMap<>();
    public static Set<Long> parseTime = forSetParse.newKeySet();

    private QueueHandler queueHandler;
    private Selector connectionSelector;
    private ServerSocketChannel welcomeSocket;

    //Starts the middleware
    public MyMiddleware(String ip, int port, List<String> mcAddresses, int numThreadsPTP, boolean readSharded){

        //Shutdown hook for the middleware
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    welcomeSocket.close();
                    //Here we print all statistics
                    printAllStatistics();
                } catch(Exception e){
                    System.err.println("Error while shutting down");
                }
            }
        });

        Params.nrServers = mcAddresses.size();
        Params.shardedRead = readSharded;
        Params.mcAdresses = mcAddresses;

        //Set up the welcome socket
        try{
            connectionSelector = Selector.open();
            welcomeSocket = ServerSocketChannel.open();
            InetSocketAddress serverAddress = new InetSocketAddress(ip,port);
            welcomeSocket.bind(serverAddress);
            welcomeSocket.configureBlocking(false);
            int ops = welcomeSocket.validOps();
            welcomeSocket.register(connectionSelector,ops,null);

        }catch(Exception e){
            System.err.println("Failed to create initial Server welcome Socket");
            e.printStackTrace();
        }

        //create a queue, and initialize every worker thread with a mock init request
        //Note that we prefer doing it this way than using the ThreadPoolExecutor method
        //prestartAllCoreThreads(), since there is going to be idle waiting after starting them up
        queueHandler =new QueueHandler(numThreadsPTP);

        Request request = new Request(3);
        for(int i = 0; i<numThreadsPTP;i++){
            queueHandler.putToQueueInit(request);
        }

        //Statistics related
        Statistics.timeInQueue = new HashSet<>();
        Statistics.nrGets = new HashSet<>();
        Statistics.nrSets = new HashSet<>();
        Statistics.nrMGets = new HashSet<>();
        Statistics.queueLength = new HashSet<>();
        Statistics.parsingTime = new HashSet<>();
        Statistics.getTime = new HashSet<>();
        Statistics.setTime = new HashSet<>();
        Statistics.mgetTime = new HashSet<>();
        Statistics.mgetMemTime = new HashSet<>();
        Timer timer = new Timer();
        timer.schedule(new StatisticsAggregator(),0,Statistics.timeWindowStat*1000);
    }

    //allocate buffer is 1100, because a key is maximum 5 bytes, there are maximum 10 of them
    //and a value is maximum 1024B => total = 1024*10+5*10 = 102450 + flags and all: 11000.
    public void run(){
        while (Params.middlewareOn) {
            try {
                connectionSelector.select();
                Set<SelectionKey> connections = connectionSelector.selectedKeys();
                Iterator<SelectionKey> connectionIterator = connections.iterator();
                while (connectionIterator.hasNext()) {
                    SelectionKey connection = connectionIterator.next();
                    if (connection.isAcceptable()) {
                        SocketChannel clientSocket = welcomeSocket.accept();

                        // Adjusts this channel's blocking mode to false
                        clientSocket.configureBlocking(false);

                        // Operation-set bit for read operations
                        clientSocket.register(connectionSelector, SelectionKey.OP_READ);
                        if (Params.verbose) {
                            System.out.println("Connection accepted " + clientSocket.getLocalAddress());
                        }
                    } else if (connection.isReadable()) {
                        SocketChannel clientSocket = (SocketChannel) connection.channel();
                        ByteBuffer bufferFromClient = ByteBuffer.allocate(2000);
                        int nrBytes = clientSocket.read(bufferFromClient);
                        if(nrBytes<0){
                            clientSocket.close();
                            connection.cancel();
                        }
                        else {
                            String readFromClient = new String(bufferFromClient.array()).trim();
                            //measure time to parse a request
                            long startParsingTime = System.nanoTime();
                            Request r = new Request(readFromClient);
                            long endParsingTime = System.nanoTime()-startParsingTime;
                            parseTime.add(endParsingTime);
                            queueHandler.putToQueue(r, clientSocket);
                        }
                    }
                    connectionIterator.remove();
                }
            } catch (Exception e) {
                System.err.println("Failed to read from a client socket");
                e.printStackTrace();
            }
        }
    }

    private void printAllStatistics(){
        BufferedWriter out = null;
        try
        {
            FileWriter fstream = new FileWriter(Statistics.fileName, true);
            out = new BufferedWriter(fstream);
            out.write("Configuration :\nnrThreads: toadd to fields of class\n...\n");
            out.write("Times in queue "+Statistics.timeInQueue+"\n");
            out.write("Parsing times "+Statistics.parsingTime+"\n");
            out.write("Times in get "+Statistics.getTime+"\n");
            out.write("Times in set "+Statistics.setTime+"\n");
            out.write("Times in mget "+Statistics.mgetTime+"\n");
            out.write("Times in mget only memcached part "+Statistics.mgetMemTime+"\n");
            out.write("Queue lenghts "+Statistics.queueLength+"\n");
            out.write("Number of gets "+Statistics.nrGets+"\n");
            out.write("Number of sets"+Statistics.nrSets+"\n");
            out.write("Number of mgets"+Statistics.nrMGets+"\n");
            out.write("\n\n\n\n\n");


        }
        catch (IOException e)
        {
            System.err.println("Error: " + e.getMessage());
        }
        finally
        {
            if(out != null) {
                try{
                    out.close();
                }catch(Exception e){
                    System.err.println("Impossible to write to file");
                }

            }
        }
    }


}
