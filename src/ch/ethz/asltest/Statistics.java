package ch.ethz.asltest;

import java.util.List;

/**
 * Created by Simon on 27.10.17.
 */
public class Statistics {
    public static String fileName = "statistics.txt";
    public static int timeWindowStat = 1;
    public static List<Long> timeInQueue;
    public static List<Long> parsingTime;
    public static List<Long> getTime;
    public static List<Long> setTime;
    public static List<Long> mgetTime;
    public static List<Long> mgetMemTime;
    public static List<Integer> queueLength;
    public static List<Integer> nrGets;
    public static List<Integer> nrSets;
    public static List<Integer> nrMGets;
}
