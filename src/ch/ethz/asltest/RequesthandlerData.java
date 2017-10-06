package ch.ethz.asltest;

import java.net.Socket;

/**
 * Created by simon on 06.10.17.
 */
public class RequesthandlerData {
    public static final ThreadLocal<Socket> serverSocket = new ThreadLocal<Socket>();
}
