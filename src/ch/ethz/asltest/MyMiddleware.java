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

/**
 * Created by Simon on 29.09.17.
 */
public class MyMiddleware implements Runnable{

    private int numThreads;
    private boolean readSharded;

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

        this.numThreads=numThreadsPTP;
        this.readSharded=readSharded;
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
        Statistics.timeInQueue = new ArrayList<>();
        Statistics.serviceTime = new ArrayList<>();
        Statistics.nrGets = new ArrayList<>();
        Statistics.nrSets = new ArrayList<>();
        Statistics.nrMGets = new ArrayList<>();
        Statistics.queueLength = new ArrayList<>();
        Statistics.parsingTime = new ArrayList<>();
        Statistics.getTime = new ArrayList<>();
        Statistics.setTime = new ArrayList<>();
        Statistics.mgetTime = new ArrayList<>();
        Statistics.mgetMemTime = new ArrayList<>();
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
                            queueHandler.putToQueue(new PreRequest(readFromClient), clientSocket);
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
            out.write("Configuration : nrThreads: "+numThreads+" ,nrServers: "+Params.nrServers+" ,read sharded: "+this.readSharded+"\n");
            out.write("Times in queue ,"+printList(Statistics.timeInQueue)+"\n");
            out.write("Parsing times ,"+printList(Statistics.parsingTime)+"\n");
            out.write("service times ,"+printList(Statistics.serviceTime)+"\n");
            out.write("Times in get ,"+printList(Statistics.getTime)+"\n");
            out.write("Times in set ,"+printList(Statistics.setTime)+"\n");
            out.write("Times in mget ,"+printList(Statistics.mgetTime)+"\n");
            out.write("Times in mget only memcached part ,"+printList(Statistics.mgetMemTime)+"\n");
            out.write("Queue lenghts ,"+printList(Statistics.queueLength)+"\n");
            out.write("Number of gets ,"+printList(Statistics.nrGets)+"\n");
            out.write("Number of sets ,"+printList(Statistics.nrSets)+"\n");
            out.write("Number of mgets ,"+printList(Statistics.nrMGets)+"\n");
            out.write("\n\n");


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
    private <T> String printList(List<T> set){
        StringBuilder s = new StringBuilder();
        for(T elem : set){
            s.append(elem);
            s.append(",");
        }
        return s.toString();
    }


}
