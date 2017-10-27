package ch.ethz.asltest;

import java.util.HashSet;
import java.util.Set;
import java.util.TimerTask;

/**
 * Created by Simon on 27.10.17.
 */
public class StatisticsAggregator extends TimerTask {
    public void run() {
        Set<Long> timesQueue = new HashSet<>(RequestHandler.timeInQueue);
        RequestHandler.timeInQueue = RequestHandler.forSetQueue.newKeySet();
        int nrGets = RequestHandler.nrGets.getAndSet(0);
        int nrSets = RequestHandler.nrSets.getAndSet(0);
        int nrMGets = RequestHandler.nrMGets.getAndSet(0);
        Set<Integer> queueLengths = new HashSet<>(QueueHandler.queueLength);
        QueueHandler.queueLength = QueueHandler.forSetQueueLength.newKeySet();
        Set<Long> parsingTimes = new HashSet<>(MyMiddleware.parseTime);
        QueueHandler.queueLength = MyMiddleware.forSetParse.newKeySet();
        Set<Long> getTimes = new HashSet<>(RequestHandler.timeInGet);
        RequestHandler.timeInGet = RequestHandler.forSetGet.newKeySet();
        Set<Long> setTimes = new HashSet<>(RequestHandler.timeInSet);
        RequestHandler.timeInSet = RequestHandler.forSetSet.newKeySet();
        Set<Long> mgetTimes = new HashSet<>(RequestHandler.timeInMGet);
        RequestHandler.timeInMGet = RequestHandler.forSetMemMGet.newKeySet();
        Set<Long> mgetMemTimes = new HashSet<>(RequestHandler.timeInMGetMem);
        RequestHandler.timeInMGetMem = RequestHandler.forSetMemMGet.newKeySet();

        setOpTimes(getTimes,setTimes,mgetTimes,mgetMemTimes);
        setParseTime(parsingTimes);
        setQueueTime(timesQueue);
        setQueueLength(queueLengths);
        updateValues(nrGets, nrSets, nrMGets);
    }

    private void updateValues(int nrG, int nrS, int nrMG) {
        Statistics.nrGets.add(nrG);
        Statistics.nrSets.add(nrS);
        Statistics.nrMGets.add(nrMG);
    }

    private long average(Set<Long> set){
        long sum = 0;
        for (long i : set) {
            sum += i;
        }
        if(set.size()!=0){
            return sum/set.size();
        }
        return 0;

    }
    private void setOpTimes(Set<Long> gets, Set<Long> sets, Set<Long> mgets, Set<Long> mMemgets){
        Statistics.getTime.add(average(gets));
        Statistics.setTime.add(average(sets));
        Statistics.mgetTime.add(average(mgets));
        Statistics.mgetMemTime.add(average(mMemgets));

    }
    private void setQueueTime(Set<Long> times) {
          Statistics.timeInQueue.add(average(times));
    }

    private void setQueueLength(Set<Integer> lengths) {
        if (lengths.size() != 0) {
            int sum = 0;
            for (int i : lengths) {
                sum += i;
            }
            int average = sum / lengths.size();
            Statistics.queueLength.add(average);
        }
    }

    private void setParseTime(Set<Long> parseTimes) {
            Statistics.parsingTime.add(average(parseTimes));

    }
}