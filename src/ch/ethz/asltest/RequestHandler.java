package ch.ethz.asltest;

import java.net.Socket;
import java.util.concurrent.ThreadFactory;

/**
 * Created by simon on 06.10.17.
 */
//The request handler is one working thread
public class RequestHandler implements Runnable{
    private String message;
    private Socket clientSocket;
    public RequestHandler(String message, Socket clientSocket){
        this.message = message;
        this.clientSocket = clientSocket;
    }

    public void run(){
        Request request = new Request(message.toString());
        System.out.println(request.toString());
        try{
            clientSocket.setKeepAlive(true);
        }catch(Exception e){

        }

        //if multi get => disperse it correctly (other class)
        //Send request to servers
        //Wait for answer
        //Interpret the answer
        //Send back to client via the socket
    }
    private static Socket getSocket(){
        return RequesthandlerData.serverSocket.get();
    }
}
