package ch.ethz.asltest;

import java.util.List;

/**
 * Created by Simon on 06.10.17.
 */
public class Params {
    public static final boolean verbose = true;
    public static final boolean experiment = false;
    public static boolean shardedRead;
    public static boolean middlewareOn = true;
    public static final int keepAliveTime = 10000;
    public static  List<String> mcAdresses ;
    public static int nrServers = 0;
}
