package ch.ethz.asltest;

import java.util.List;
import java.util.Set;

/**
 * Created by Simon on 06.10.17.
 * Parameters to run the middleware
 */
public class Params {
    public static final boolean verbose = false;
    public static boolean shardedRead;
    public static boolean middlewareOn = true;
    public static final int keepAliveTime = 10000;
    public static int nrThreads;
    public static  List<String> mcAdresses ;
    public static int nrServers = 0;

}
