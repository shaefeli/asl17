package ch.ethz.asltest;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by Simon on 29.09.17.
 */
public class MyMiddleware implements Runnable{
    QueueHandler queueHandler;
    private Selector connectionSelector;
    private ServerSocketChannel welcomeSocket;
    public MyMiddleware(String ip, int port, List<String> mcAddresses, int numThreadsPTP, boolean readSharded){
        //Shutdown hook for the middleware
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    welcomeSocket.close();
                    printAllStatistics();
                } catch(Exception e){
                    System.err.println("Error while shutting down");
                }
            }
        });

        Config.nrServers = mcAddresses.size();
        Config.shardedRead = readSharded;
        try{
            connectionSelector = Selector.open();
            welcomeSocket = ServerSocketChannel.open();
            InetSocketAddress serverAdress = new InetSocketAddress(ip,port);
            welcomeSocket.bind(serverAdress);
            welcomeSocket.configureBlocking(false);
            int ops = welcomeSocket.validOps();
            welcomeSocket.register(connectionSelector,ops,null);

        }catch(Exception e){
            System.err.println("Failed to create initial Server Socket");
            e.printStackTrace();
        }

        Config.mcAdresses = mcAddresses;
        queueHandler =new QueueHandler(numThreadsPTP);
        //Initialize every thread
        Request request = new Request(3);
        for(int i = 0; i<numThreadsPTP;i++){
            queueHandler.putToQueueInit(request);
        }
    }
    //allocate buffer is 1100, because a key is maximum 5 bytes, there are maximum 10 of them
    //and a value is maximum 1024B => total = 1024*10+5*10 = 102450 + flags and all: 11000.
    public void run(){
        while (Config.middlewareOn) {
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
                        if (Config.verbose) {
                            System.out.println("Connection accepted " + clientSocket.getLocalAddress());
                        }
                    } else if (connection.isReadable()) {
                        SocketChannel clientSocket = (SocketChannel) connection.channel();
                        ByteBuffer bufferFromClient = ByteBuffer.allocate(11000);
                        int nrBytes = clientSocket.read(bufferFromClient);
                        if(nrBytes<0){
                            clientSocket.close();
                            connection.cancel();
                        }
                        else {
                            String readFromClient = new String(bufferFromClient.array()).trim();
                            Request r = new Request(readFromClient);
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

    }


}
