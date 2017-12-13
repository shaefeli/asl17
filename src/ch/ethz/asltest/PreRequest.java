package ch.ethz.asltest;

/**
 * Created by simon on 09.11.17.
 * Binds a message with a specific time point
 */
public class PreRequest {
    public String toParse;
    public long startingTimeQueue;

    public PreRequest(String toParse){
        this.startingTimeQueue = System.nanoTime();
        this.toParse = toParse;
    }
}
