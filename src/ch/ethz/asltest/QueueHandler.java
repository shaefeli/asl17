package ch.ethz.asltest;

import java.net.Socket;
import java.util.concurrent.*;

/**
 * Created by Simon on 06.10.17.
 */
public class QueueHandler {
    public static ThreadPoolExecutor workerThreadPool;

    public QueueHandler(int numThreadsPTP){
        workerThreadPool = new ThreadPoolExecutor(numThreadsPTP,numThreadsPTP,Config.keepAliveTime, TimeUnit.SECONDS,new LinkedBlockingQueue<>());
    }

    public static void putToQueue(Request request, Socket clientSocket){
        // Receive TCP packet and give it to the queue Handler

        workerThreadPool.execute(new RequestHandler(request, clientSocket));


    }
    public static int sizeOfQueue(){
        return workerThreadPool.getQueue().size();
    }
}
