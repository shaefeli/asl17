package ch.ethz.asltest;

import java.net.Socket;

/**
 * Created by simon on 06.10.17.
 */
//Class to initialize Worker Thread
public class RequestHandlerInitializer extends Thread{
    public RequestHandlerInitializer(Runnable r){
        super(r);
    }
    //The actual initialization of the woker thread
    public void run(){
        RequesthandlerData.serverSocket.set(new Socket()); //"Socket with the server"
        super.run();
    }
}
