package ch.ethz.asltest;

import java.net.Socket;
import java.util.concurrent.*;

/**
 * Created by simon on 06.10.17.
 */
public class QueueHandler {
    ThreadPoolExecutor workerThreadPool;

    public QueueHandler(int numThreadsPTP){
        workerThreadPool = new ThreadPoolExecutor(numThreadsPTP,numThreadsPTP,Config.keepAliveTime, TimeUnit.SECONDS,new LinkedBlockingQueue<>());
        workerThreadPool.setThreadFactory(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                return new RequestHandlerInitializer(runnable);
            }
        });
    }

    public void putToQueue(String message, Socket clientSocket){
        System.out.println("Hello");
        workerThreadPool.execute(new RequestHandler(message, clientSocket));
    }
    public int sizeOfQueue(){
        return workerThreadPool.getQueue().size();
    }
}
