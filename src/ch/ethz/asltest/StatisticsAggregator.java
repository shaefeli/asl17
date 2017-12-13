package ch.ethz.asltest;

import java.util.*;

/**
 * Created by Simon on 27.10.17.
 * Collects the statistics
 */
public class StatisticsAggregator extends TimerTask {
    public void run() {
        long[] serviceTimeInThread= new long[Params.nrThreads];
        int[] serviceTimeInThreadCount= new int[Params.nrThreads];
        System.arraycopy(RequestHandler.perThreadServiceTimes,0,serviceTimeInThread,0,Params.nrThreads);
        System.arraycopy(RequestHandler.perThreadServiceTimesCount,0,serviceTimeInThreadCount,0,Params.nrThreads);
        RequestHandler.perThreadServiceTimes= new long[Params.nrThreads];
        RequestHandler.perThreadServiceTimesCount= new int[Params.nrThreads];

        Long[] averagesServiceTime = new Long[Params.nrThreads];
        for (int i=0;i<Params.nrThreads;i++){
            averagesServiceTime[i] = computeAverage(serviceTimeInThread[i],serviceTimeInThreadCount[i]);
        }
        int nrArrivals = MyMiddleware.nrArrivals.getAndSet(0);
        int arrivalRate = nrArrivals/Statistics.timeWindowStat;

        long timeInQueue = RequestHandler.timeInQueue.getAndSet(0);
        int timeInQueueCount = RequestHandler.timeInQueueCount.getAndSet(0);

        long serviceTime = RequestHandler.serviceTime.getAndSet(0);
        int serviceTimeCount = RequestHandler.serviceTimeCount.getAndSet(0);

        int nrGets = RequestHandler.nrGets.getAndSet(0);
        int nrSets = RequestHandler.nrSets.getAndSet(0);
        int nrMGets = RequestHandler.nrMGets.getAndSet(0);
        int nrMissesGets = RequestHandler.nrMissesGet.getAndSet(0);

        int queueLength = QueueHandler.queueLength.getAndSet(0);
        int queueLengthCount = QueueHandler.queueLengthCount.getAndSet(0);

        long parsingTimes = RequestHandler.parseTime.getAndSet(0);
        int parsingTimesCount = RequestHandler.parseTimeCount.getAndSet(0);

        long getTimes = RequestHandler.timeInGet.getAndSet(0);
        int getTimesCount = RequestHandler.timeInGetCount.getAndSet(0);

        long setTimes = RequestHandler.timeInSet.getAndSet(0);
        int setTimesCount = RequestHandler.timeInSetCount.getAndSet(0);

        long mgetTimes = RequestHandler.timeInMGet.getAndSet(0);
        int mgetTimesCount = RequestHandler.timeInMGetCount.getAndSet(0);

        long mgetMemTimes = RequestHandler.timeInMGetMem.getAndSet(0);
        int mgetMemTimesCount = RequestHandler.timeInMGetMemCount.getAndSet(0);

        Statistics.arrivalRate.add(arrivalRate);
        Statistics.nrMGets.add(nrMGets);
        Statistics.nrGets.add(nrGets);
        Statistics.nrSets.add(nrSets);
        Statistics.nrMissesGets.add(nrMissesGets);
        Statistics.throughput.add((nrMGets+nrGets+nrSets)/Statistics.timeWindowStat);
        Statistics.serviceTime.add(computeAverage(serviceTime,serviceTimeCount));
        Statistics.timeInQueue.add(computeAverage(timeInQueue,timeInQueueCount));
        Statistics.queueLength.add(computeAverage(queueLength,queueLengthCount));
        Statistics.parsingTime.add(computeAverage(parsingTimes,parsingTimesCount));
        Statistics.getTime.add(computeAverage(getTimes,getTimesCount));
        Statistics.setTime.add(computeAverage(setTimes,setTimesCount));
        Statistics.mgetTime.add(computeAverage(mgetTimes,mgetTimesCount));
        Statistics.mgetMemTime.add(computeAverage(mgetMemTimes,mgetMemTimesCount));

        for (int i=0;i<Params.nrThreads;i++){
            Statistics.serviceTimesPerThread[i].add(averagesServiceTime[i]);
        }

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