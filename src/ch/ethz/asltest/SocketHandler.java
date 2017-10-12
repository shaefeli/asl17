package ch.ethz.asltest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Created by Simon on 07.10.17.
 */
public class SocketHandler implements Runnable{
    private Socket clientSocket;
    public SocketHandler(Socket clientSocket){
        this.clientSocket = clientSocket;
    }
    public void run(){
        while(Config.middlewareOn){
            String userInput;
            try{
                BufferedReader din = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));


                boolean completeMessagereceived = true;
                Request storedSetRequest = new Request("");

                while ((userInput = din.readLine()) != null) {
                    boolean isSetRequest = false;
                    Request request = new Request(userInput);
                    //System.out.println("Line received : "+request.toString());
                    if(request.requestType == 1){
                        //System.out.println("Request type set");
                        storedSetRequest = new Request(userInput);
                        completeMessagereceived = false;
                    }

                    else if(request.requestType == 0 && !completeMessagereceived){
                        storedSetRequest.restOfMessage = storedSetRequest.restOfMessage +"\n"+userInput+"\r";
                        System.out.println(storedSetRequest.restOfMessage);
                        completeMessagereceived = true;
                        isSetRequest = true;
                    }

                    if(completeMessagereceived){
                        if(isSetRequest){
                            //System.out.println("Request to queueHandler: "+storedSetRequest.toString());
                            QueueHandler.putToQueue(storedSetRequest,clientSocket);
                        }
                        else{
                            //System.out.println("Request to queueHandler: "+request.toString());
                            QueueHandler.putToQueue(request,clientSocket);
                        }


                        storedSetRequest = new Request("");
                    }
                }
            }catch(Exception e){

            }



        }

    }
}
