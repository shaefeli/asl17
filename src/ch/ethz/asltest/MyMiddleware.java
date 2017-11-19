package ch.ethz.asltest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by Simon on 29.09.17.
 */
public class MyMiddleware implements Runnable{

    private int numThreads;
    private boolean readSharded;

    private QueueHandler queueHandler;
    private Selector connectionSelector;
    private ServerSocketChannel welcomeSocket;

    //For statistics
    public static AtomicInteger nrArrivals = new AtomicInteger(0);

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

        Params.nrThreads = numThreadsPTP;
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
        Statistics.nrMissesGets = new ArrayList<>();
        Statistics.throughput = new ArrayList<>();
        Statistics.nrMGets = new ArrayList<>();
        Statistics.queueLength = new ArrayList<>();
        Statistics.parsingTime = new ArrayList<>();
        Statistics.getTime = new ArrayList<>();
        Statistics.setTime = new ArrayList<>();
        Statistics.mgetTime = new ArrayList<>();
        Statistics.mgetMemTime = new ArrayList<>();
        Statistics.serviceTimesPerThread = new ArrayList [Params.nrThreads];
        for (int i=0;i<Params.nrThreads;i++){
            Statistics.serviceTimesPerThread[i] = new ArrayList<>();
        }
        Statistics.arrivalRate = new ArrayList<>();
        Timer timer = new Timer();
        timer.schedule(new StatisticsAggregator(),0,Statistics.timeWindowStat*1000);
    }

    //allocate buffer is 1100, because a key is maximum 5 bytes, there are maximum 10 of them
    //and a value is maximum 1024B => total = 1024*10+5*10 = 102450 + flags and all: 11000.
    public void run(){
        System.out.println("Ready to serve on port: "+welcomeSocket.toString());
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
                            nrArrivals.getAndIncrement();
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
        Statistics.nrMGets.removeIf(p -> p == 0);
        Statistics.nrMissesGets.removeIf(p -> p == 0);
        Statistics.arrivalRate.removeIf(p -> p== 0);

        for (int i=0;i<Params.nrThreads;i++){
            Statistics.serviceTimesPerThread[i].removeIf(p -> p == 0);
            //Statistics.serviceTimesPerThread[i]=remove(Statistics.serviceTimesPerThread[i]);
        }

        /*Statistics.timeInQueue = remove(Statistics.timeInQueue);
        Statistics.parsingTime = remove(Statistics.parsingTime);
        Statistics.serviceTime = remove(Statistics.serviceTime);
        Statistics.throughput = remove(Statistics.throughput);
        Statistics.getTime = remove(Statistics.getTime);
        Statistics.setTime = remove(Statistics.setTime);
        Statistics.mgetTime = remove(Statistics.mgetTime);
        Statistics.mgetMemTime = remove(Statistics.mgetMemTime);
        Statistics.queueLength = remove(Statistics.queueLength);
        Statistics.nrGets = remove(Statistics.nrGets);
        Statistics.nrSets = remove(Statistics.nrSets);
        Statistics.nrMGets = remove(Statistics.nrMGets);
        Statistics.nrMissesGets = remove(Statistics.nrMissesGets);
        Statistics.arrivalRate = remove(Statistics.arrivalRate);*/

        BufferedWriter out = null;
        try
        {
            File statF = new File(Statistics.fileName);
            //append
            if(statF.exists() && !statF.isDirectory()){
                List<String> lines = Files.lines(Paths.get(Statistics.fileName)).collect(Collectors.toList());
                List<String> addedLines = new ArrayList<>();
                for(String line : lines){
                    if(line.startsWith("Configuration")){
                        addedLines.add(line);
                    }
                    else if(line.startsWith("Thread")){
                        for (int i=0;i<Params.nrThreads;i++){
                            if(line.startsWith("Thread "+i+" ")){
                                addedLines.add(line+","+averageLong(Statistics.serviceTimesPerThread[i]));
                            }
                            else if(line.startsWith("Thread std "+i+" ")){
                                addedLines.add(line+","+stdDeviationLong(Statistics.serviceTimesPerThread[i]));
                            }
                        }
                    }
                    //Adding to the averages
                    else if(line.startsWith("Times in queue")){
                        addedLines.add(line+","+averageLong(Statistics.timeInQueue));
                    }
                    else if(line.startsWith("Parsing times")){
                        addedLines.add(line+","+averageLong(Statistics.parsingTime));
                    }
                    else if(line.startsWith("Service times")){
                        addedLines.add(line+","+averageLong(Statistics.serviceTime));
                    }
                    else if(line.startsWith("Throughput")){
                        addedLines.add(line+","+average(Statistics.throughput));
                    }
                    else if(line.startsWith("Times in get")){
                        addedLines.add(line+","+averageLong(Statistics.getTime));
                    }
                    else if(line.startsWith("Times in set")){
                        addedLines.add(line+","+averageLong(Statistics.setTime));
                    }
                    else if(line.startsWith("Times in mget")){
                        addedLines.add(line+","+averageLong(Statistics.mgetTime));
                    }
                    else if(line.startsWith("Times in mget only")){
                        addedLines.add(line+","+averageLong(Statistics.mgetMemTime));
                    }
                    else if(line.startsWith("Queue lengths")){
                        addedLines.add(line+","+average(Statistics.queueLength));
                    }
                    else if(line.startsWith("Number of gets")){
                        addedLines.add(line+","+average(Statistics.nrGets));
                    }
                    else if(line.startsWith("Number of sets")){
                        addedLines.add(line+","+average(Statistics.nrSets));
                    }
                    else if(line.startsWith("Number of mgets")){
                        addedLines.add(line+","+average(Statistics.nrMGets));
                    }
                    else if(line.startsWith("Number of misses get")){
                        addedLines.add(line+","+average(Statistics.nrMissesGets));
                    }
                    else if(line.startsWith("Arrival rate")){
                        addedLines.add(line+","+average(Statistics.arrivalRate));
                    }
                    //Adding to the standard
                    if(line.startsWith("std Times in queue")){
                        addedLines.add(line+","+stdDeviationLong(Statistics.timeInQueue));
                    }
                    else if(line.startsWith("std Parsing times")){
                        addedLines.add(line+","+stdDeviationLong(Statistics.parsingTime));
                    }
                    else if(line.startsWith("std Service times")){
                        addedLines.add(line+","+stdDeviationLong(Statistics.serviceTime));
                    }
                    else if(line.startsWith("std Throughput")){
                        addedLines.add(line+","+stdDeviation(Statistics.throughput));
                    }
                    else if(line.startsWith("std Times in get")){
                        addedLines.add(line+","+stdDeviationLong(Statistics.getTime));
                    }
                    else if(line.startsWith("std Times in set")){
                        addedLines.add(line+","+stdDeviationLong(Statistics.setTime));
                    }
                    else if(line.startsWith("std Times in mget")){
                        addedLines.add(line+","+stdDeviationLong(Statistics.mgetTime));
                    }
                    else if(line.startsWith("std Times in mget only")){
                        addedLines.add(line+","+stdDeviationLong(Statistics.mgetMemTime));
                    }
                    else if(line.startsWith("std Queue lengths")){
                        addedLines.add(line+","+stdDeviation(Statistics.queueLength));
                    }
                    else if(line.startsWith("std Number of gets")){
                        addedLines.add(line+","+stdDeviation(Statistics.nrGets));
                    }
                    else if(line.startsWith("std Number of sets")){
                        addedLines.add(line+","+stdDeviation(Statistics.nrSets));
                    }
                    else if(line.startsWith("std Number of mgets")){
                        addedLines.add(line+","+stdDeviation(Statistics.nrMGets));
                    }
                    else if(line.startsWith("std Number of misses get")){
                        addedLines.add(line+","+stdDeviation(Statistics.nrMissesGets));
                    }
                    else if(line.startsWith("std Arrival rate")){
                        addedLines.add(line+","+stdDeviation(Statistics.arrivalRate));
                    }
                    //else do nothing

                }
                Files.write(Paths.get(Statistics.fileName), addedLines);
            }
            //Create new
            else {
                FileWriter fstream = new FileWriter(Statistics.fileName, true);
                out = new BufferedWriter(fstream);
                out.write("Configuration : nrThreads: " + numThreads + " ,nrServers: " + Params.nrServers + " ,read sharded: " + this.readSharded + "\n");

                out.write("Times in queue ," + averageLong(Statistics.timeInQueue) + "\n");
                out.write("Parsing times ," + averageLong(Statistics.parsingTime) + "\n");
                out.write("Service times ," + averageLong(Statistics.serviceTime) + "\n");
                out.write("Throughput ," + average(Statistics.throughput) + "\n");
                out.write("Times in get ," + averageLong(Statistics.getTime) + "\n");
                out.write("Times in set ," + averageLong(Statistics.setTime) + "\n");
                out.write("Times in mget ," + averageLong(Statistics.mgetTime) + "\n");
                out.write("Times in mget only memcached part ," + averageLong(Statistics.mgetMemTime) + "\n");
                out.write("Queue lenghts ," + average(Statistics.queueLength) + "\n");
                out.write("Number of gets ," + average(Statistics.nrGets) + "\n");
                out.write("Number of sets ," + average(Statistics.nrSets) + "\n");
                out.write("Number of mgets ," + average(Statistics.nrMGets) + "\n");
                out.write("Number of misses get ," + average(Statistics.nrMissesGets) + "\n");
                out.write("Arrival rate ," + average(Statistics.arrivalRate) + "\n");

                out.write("std Times in queue ," + stdDeviationLong(Statistics.timeInQueue) + "\n");
                out.write("std Parsing times ," + stdDeviationLong(Statistics.parsingTime) + "\n");
                out.write("std Service times ," + stdDeviationLong(Statistics.serviceTime) + "\n");
                out.write("std Throughput ," + stdDeviation(Statistics.throughput) + "\n");
                out.write("std Times in get ," + stdDeviationLong(Statistics.getTime) + "\n");
                out.write("std Times in set ," + stdDeviationLong(Statistics.setTime) + "\n");
                out.write("std Times in mget ," + stdDeviationLong(Statistics.mgetTime) + "\n");
                out.write("std Times in mget only memcached part ," + stdDeviationLong(Statistics.mgetMemTime) + "\n");
                out.write("std Queue lenghts ," + stdDeviation(Statistics.queueLength) + "\n");
                out.write("std Number of gets ," + stdDeviation(Statistics.nrGets) + "\n");
                out.write("std Number of sets ," + stdDeviation(Statistics.nrSets) + "\n");
                out.write("std Number of mgets ," + stdDeviation(Statistics.nrMGets) + "\n");
                out.write("std Number of misses get ," + stdDeviation(Statistics.nrMissesGets) + "\n");
                out.write("std Arrival rate ," + stdDeviation(Statistics.arrivalRate) + "\n");

                out.write("\n");
                for (int i=0;i<Params.nrThreads;i++){
                    out.write("Thread "+i+" ," + averageLong(Statistics.serviceTimesPerThread[i]) + "\n");
                    out.write("Thread std "+i+" ," + stdDeviationLong(Statistics.serviceTimesPerThread[i]) + "\n");
                }

                out.write("\n\n");

            }

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
        int nrItemsToRemove = 10;

        if(list.size() > 11){
            for(int i=0;i<nrItemsToRemove;i++){
                list.remove(0);

            }
            list.remove(list.size()-1);
            return list;

        }
        else{
            //If the list has only few non zero elements consider it as all 0
            return new ArrayList<>();
        }
    }
    private double average(List<Integer>  list){
        double sum = 0;
        for(int i:list){
            sum+= i;
        }
        if(list.size()==0){
            return 0;
        }
        return sum/list.size();

    }

    private double stdDeviation(List<Integer> list){
        double average = average(list);
        double sumDiffSquare = 0;
        for(long i : list){
            sumDiffSquare+=Math.pow(i-average,2);
        }
        if(list.size()==0){
            return 0;
        }
        if(list.size()>1){
            return Math.sqrt(sumDiffSquare/(list.size()-1));
        }
        else return 0;

    }
    private double averageLong(List<Long>  list){
        double sum = 0;
        for(long i:list){
            sum+= i;
        }
        if(list.size()==0){
            return 0;
        }
        return sum/list.size();

    }

    private double stdDeviationLong(List<Long> list){
        double average = averageLong(list);
        double sumDiffSquare = 0;
        for(long i : list){
            sumDiffSquare+=Math.pow(i-average,2);
        }
        if(list.size()==0){
            return 0;
        }
        if(list.size()>1){
            return Math.sqrt(sumDiffSquare/(list.size()-1));
        }
        else return 0;
    }


}
