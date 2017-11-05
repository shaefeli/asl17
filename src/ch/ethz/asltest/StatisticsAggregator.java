package ch.ethz.asltest;

import java.util.*;

/**
 * Created by Simon on 27.10.17.
 */
public class StatisticsAggregator extends TimerTask {
    public void run() {
        long timeInQueue = RequestHandler.timeInQueue.getAndSet(0);
        int timeInQueueCount = RequestHandler.timeInQueueCount.getAndSet(0);

        int nrGets = RequestHandler.nrGets.getAndSet(0);
        int nrSets = RequestHandler.nrSets.getAndSet(0);
        int nrMGets = RequestHandler.nrMGets.getAndSet(0);

        int queueLength = QueueHandler.queueLength.getAndSet(0);
        int queueLengthCount = QueueHandler.queueLengthCount.getAndSet(0);

        long parsingTimes = MyMiddleware.parseTime.getAndSet(0);
        int parsingTimesCount = MyMiddleware.parseTimeCount.getAndSet(0);

        long getTimes = RequestHandler.timeInGet.getAndSet(0);
        int getTimesCount = RequestHandler.timeInGetCount.getAndSet(0);

        long setTimes = RequestHandler.timeInSet.getAndSet(0);
        int setTimesCount = RequestHandler.timeInSetCount.getAndSet(0);

        long mgetTimes = RequestHandler.timeInMGet.getAndSet(0);
        int mgetTimesCount = RequestHandler.timeInMGetCount.getAndSet(0);

        long mgetMemTimes = RequestHandler.timeInMGetMem.getAndSet(0);
        int mgetMemTimesCount = RequestHandler.timeInMGetMemCount.getAndSet(0);

        Statistics.nrMGets.add(nrMGets);
        Statistics.nrGets.add(nrGets);
        Statistics.nrSets.add(nrSets);
        Statistics.timeInQueue.add(computeAverage(timeInQueue,timeInQueueCount));
        Statistics.queueLength.add(computeAverage(queueLength,queueLengthCount));
        Statistics.parsingTime.add(computeAverage(parsingTimes,parsingTimesCount));
        Statistics.getTime.add(computeAverage(getTimes,getTimesCount));
        Statistics.setTime.add(computeAverage(setTimes,setTimesCount));
        Statistics.mgetTime.add(computeAverage(mgetTimes,mgetTimesCount));
        Statistics.mgetMemTime.add(computeAverage(mgetMemTimes,mgetMemTimesCount));



    }
    private long computeAverage(long a, int count){
        if(count == 0){
            return 0;
        }
        else{
            return a/count;
        }
    }
    private int computeAverage(int a, int count){
        if(count == 0){
            return 0;
        }
        else{
            return a/count;
        }
    }
}