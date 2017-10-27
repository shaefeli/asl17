package ch.ethz.asltest;

import java.util.Set;

/**
 * Created by Simon on 27.10.17.
 */
public class Statistics {
    public static String fileName = "statistics.txt";
    public static int timeWindowStat = 1;
    public static Set<Long> timeInQueue;
    public static Set<Long> parsingTime;
    public static Set<Long> getTime;
    public static Set<Long> setTime;
    public static Set<Long> mgetTime;
    public static Set<Long> mgetMemTime;
    public static Set<Integer> queueLength;
    public static Set<Integer> nrGets;
    public static Set<Integer> nrSets;
    public static Set<Integer> nrMGets;
}
