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
                    e.printStackTrace();
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
        Statistics.throughput = new ArrayList<>();
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
        Statistics.timeInQueue.removeIf(p -> p == 0);
        Statistics.parsingTime.removeIf(p -> p == 0);
        Statistics.serviceTime.removeIf(p -> p == 0);
        Statistics.throughput.removeIf(p -> p == 0);
        Statistics.getTime.removeIf(p -> p == 0);
        Statistics.setTime.removeIf(p -> p == 0);
        Statistics.mgetTime.removeIf(p -> p == 0);
        Statistics.mgetMemTime.removeIf(p -> p == 0);
        Statistics.queueLength.removeIf(p -> p == 0);
        Statistics.nrGets.removeIf(p -> p == 0);
        Statistics.nrSets.removeIf(p -> p == 0);
        Statistics.nrMGets.removeIf(p -> p == 0);

        remove(Statistics.timeInQueue);
        remove(Statistics.parsingTime);
        remove(Statistics.serviceTime);
        remove(Statistics.throughput);
        remove(Statistics.getTime);
        remove(Statistics.setTime);
        remove(Statistics.mgetTime);
        remove(Statistics.mgetMemTime);
        remove(Statistics.queueLength);
        remove(Statistics.nrGets);
        remove(Statistics.nrSets);
        remove(Statistics.nrMGets);

        BufferedWriter out = null;
        try
        {
            FileWriter fstream = new FileWriter(Statistics.fileName, true);
            out = new BufferedWriter(fstream);
            out.write("Configuration : nrThreads: "+numThreads+" ,nrServers: "+Params.nrServers+" ,read sharded: "+this.readSharded+"\n");
            out.write("Times in queue ,"+printList(Statistics.timeInQueue)+"\n");
            out.write("Parsing times ,"+printList(Statistics.parsingTime)+"\n");
            out.write("service times ,"+printList(Statistics.serviceTime)+"\n");
            out.write("throughput ,"+printList(Statistics.throughput)+"\n");
            out.write("Times in get ,"+printList(Statistics.getTime)+"\n");
            out.write("Times in set ,"+printList(Statistics.setTime)+"\n");
            out.write("Times in mget ,"+printList(Statistics.mgetTime)+"\n");
            out.write("Times in mget only memcached part ,"+printList(Statistics.mgetMemTime)+"\n");
            out.write("Queue lenghts ,"+printList(Statistics.queueLength)+"\n");
            out.write("Number of gets ,"+printList(Statistics.nrGets)+"\n");
            out.write("Number of sets ,"+printList(Statistics.nrSets)+"\n");
            out.write("Number of mgets ,"+printList(Statistics.nrMGets)+"\n");

            out.write("S Times in queue ,"+averageLong(Statistics.timeInQueue)+" / "+stdDeviationLong(Statistics.timeInQueue)+"\n");
            out.write("S Parsing times ,"+averageLong(Statistics.parsingTime)+" / "+stdDeviationLong(Statistics.parsingTime)+"\n");
            out.write("S service times ,"+averageLong(Statistics.serviceTime)+" / "+stdDeviationLong(Statistics.serviceTime)+"\n");
            out.write("S throughput ,"+average(Statistics.throughput)+" / "+stdDeviation(Statistics.throughput)+"\n");
            out.write("S Times in get ,"+averageLong(Statistics.getTime)+" / "+stdDeviationLong(Statistics.getTime)+"\n");
            out.write("S Times in set ,"+averageLong(Statistics.setTime)+" / "+stdDeviationLong(Statistics.setTime)+"\n");
            out.write("S Times in mget ,"+averageLong(Statistics.mgetTime)+" / "+stdDeviationLong(Statistics.mgetTime)+"\n");
            out.write("S Times in mget only memcached part ,"+averageLong(Statistics.mgetMemTime)+" / "+stdDeviationLong(Statistics.mgetMemTime)+"\n");
            out.write("S Queue lenghts ,"+average(Statistics.queueLength)+" / "+stdDeviation(Statistics.queueLength)+"\n");
            out.write("S Number of gets ,"+average(Statistics.nrGets)+" / "+stdDeviation(Statistics.nrGets)+"\n");
            out.write("S Number of sets ,"+average(Statistics.nrSets)+" / "+stdDeviation(Statistics.nrSets)+"\n");
            out.write("S Number of mgets ,"+average(Statistics.nrMGets)+" / "+stdDeviation(Statistics.nrMGets)+"\n");

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

    private <T> List<T> remove(List<T> list){

        if(list.size()!= 0){
            list.remove(0);
            list.remove(0);
            list.remove(0);
            list.remove(list.size()-1);
            return list;
        }
        else{
            return list;
        }
    }
    private double average(List<Integer>  list){
        double sum = 0;
        for(int i:list){
            sum+= i;
        }
        return sum/list.size();

    }

    private double stdDeviation(List<Integer> list){
        double average = average(list);
        double sumDiffSquare = 0;
        for(long i : list){
            sumDiffSquare+=Math.pow(i-average,2);
        }
        return Math.sqrt(sumDiffSquare/(list.size()-1));
    }
    private double averageLong(List<Long>  list){
        double sum = 0;
        for(long i:list){
            sum+= i;
        }
        return sum/list.size();

    }

    private double stdDeviationLong(List<Long> list){
        double average = averageLong(list);
        double sumDiffSquare = 0;
        for(long i : list){
            sumDiffSquare+=Math.pow(i-average,2);
        }
        return Math.sqrt(sumDiffSquare/(list.size()-1));
    }


}
