package ch.ethz.asltest;

import java.nio.channels.SocketChannel;
import java.util.concurrent.*;

/**
 * Created by Simon on 06.10.17.
 */
public class QueueHandler {
    public ThreadPoolExecutor workerThreadPool;

    public QueueHandler(int numThreadsPTP){
        workerThreadPool = new ThreadPoolExecutor(numThreadsPTP,numThreadsPTP,Config.keepAliveTime, TimeUnit.SECONDS,new LinkedBlockingQueue<>());
    }

    public void putToQueue(Request request, SocketChannel clientSocket){
        // Receive TCP packet and give it to the queue Handler
        if(Config.verbose){
            System.out.println("Size of queue: "+sizeOfQueue());
            System.out.println(workerThreadPool.getActiveCount());
        }
        workerThreadPool.execute(new RequestHandler(request, clientSocket));
    }
    public  void putToQueueInit(Request request){
        // Receive TCP packet and give it to the queue Handler
        if(Config.verbose){
            System.out.println("Size of queue: "+sizeOfQueue());
            System.out.println(workerThreadPool.getActiveCount());
        }
        workerThreadPool.execute(new RequestHandler(request));
    }
    public int sizeOfQueue(){
        return workerThreadPool.getQueue().size();
    }
}
