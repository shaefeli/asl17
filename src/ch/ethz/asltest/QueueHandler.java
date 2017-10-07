package ch.ethz.asltest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.*;

/**
 * Created by simon on 06.10.17.
 */
public class QueueHandler {
    public static ThreadPoolExecutor workerThreadPool;

    public QueueHandler(int numThreadsPTP){
        workerThreadPool = new ThreadPoolExecutor(numThreadsPTP,numThreadsPTP,Config.keepAliveTime, TimeUnit.SECONDS,new LinkedBlockingQueue<>());
        workerThreadPool.setThreadFactory(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                return new RequestHandlerInitializer(runnable);
            }
        });
    }

    public static void putToQueue(String message, Socket clientSocket){
        // Receive TCP packet and give it to the queue Handler
        workerThreadPool.execute(new RequestHandler(message, clientSocket));
    }
    public int sizeOfQueue(){
        return workerThreadPool.getQueue().size();
    }
}
