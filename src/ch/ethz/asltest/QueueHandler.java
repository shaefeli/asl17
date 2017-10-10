package ch.ethz.asltest;

import java.net.Socket;
import java.util.concurrent.*;

/**
 * Created by simon on 06.10.17.
 */
public class QueueHandler {
    public static ThreadPoolExecutor workerThreadPool;

    public QueueHandler(int numThreadsPTP){
        workerThreadPool = new ThreadPoolExecutor(numThreadsPTP,numThreadsPTP,Config.keepAliveTime, TimeUnit.SECONDS,new LinkedBlockingQueue<>());
    }

    public static void putToQueue(String message, Socket clientSocket){
        // Receive TCP packet and give it to the queue Handler
        if(!message.equals("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")) {
            workerThreadPool.execute(new RequestHandler(message, clientSocket));
        }
        else{
            System.out.println("Message di mierda");
        }
    }
    public int sizeOfQueue(){
        return workerThreadPool.getQueue().size();
    }
}
