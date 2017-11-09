package ch.ethz.asltest;

import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Simon on 06.10.17.
 */
public class QueueHandler {
    public ThreadPoolExecutor workerThreadPool;
    //Statistics related
    public static AtomicInteger queueLengthCount = new AtomicInteger(0);
    public static AtomicInteger queueLength = new AtomicInteger(0);

    public QueueHandler(int numThreadsPTP){
        //Note that we use a LinkedBlockingQueue, here since it does exactly what we want.
        //Unbounded queues. Using an unbounded queue will cause new tasks to wait in the queue when all corePoolSize threads are busy.
        // Thus, no more than corePoolSize threads will ever be created
        workerThreadPool = new ThreadPoolExecutor(numThreadsPTP,numThreadsPTP, Params.keepAliveTime, TimeUnit.SECONDS,new LinkedBlockingQueue<>());
    }

    public void putToQueue(PreRequest prerequest, SocketChannel clientSocket){
        //Put the request to queue and execute it (after waiting in the queue)
        if(Params.verbose){
            System.out.println("Size of queue: "+sizeOfQueue());
            System.out.println(workerThreadPool.getActiveCount());
        }
        workerThreadPool.execute(new RequestHandler(prerequest, clientSocket));
        queueLength.addAndGet(sizeOfQueue());
        queueLengthCount.getAndIncrement();
    }
    public  void putToQueueInit(Request request){
        //Put the request to queue and execute it (after waiting in the queue), for initilaizing the threads
        if(Params.verbose){
            System.out.println("Size of queue: "+sizeOfQueue());
            System.out.println(workerThreadPool.getActiveCount());
        }
        workerThreadPool.execute(new RequestHandler(request));
    }
    private int sizeOfQueue(){
        return workerThreadPool.getQueue().size();
    }
}
