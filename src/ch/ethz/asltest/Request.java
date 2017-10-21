package ch.ethz.asltest;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Simon on 06.10.17.
 */
public class Request {

    public int requestType =RequestType.UNKNOWN;
    public String restOfMessage = ""; //The whole request without the request type
    public List<String> keys = new ArrayList<>();   //In case of a multi get we want to control the number of keys, not going to be filled in case of simple get.


    //Main constructor: returns a request from the raw text received from the Socket
    //Used by MyMiddleware
    public Request(String message){
        parseMessage(message);
    }

    //used by main constructor: returns a proper request after having read the raw message
    private Request(int requestType, String restMessage){
        this.restOfMessage = restMessage;
        this.requestType = requestType;
    }

    //used by main constructor: returns a proper request after having read the raw message for a multi get
    private Request(int requestType, String restMessage, List<String> keys){
        this.restOfMessage = restMessage;
        this.requestType = requestType;
        this.keys = keys;
    }

    //Used by Multi get handler to create a proper multi get requests out of some keys.
    public Request(int requestType, List<String> keys){
        this.requestType = requestType;
        this.keys = keys;
        this.restOfMessage = createStringWithKeys(keys);
    }

    //Used for creating an init request or an unknown request
    public Request(int requestType){
        this.requestType = requestType;
    }






    //Interprets the incoming message and decides what request type it is
    private Request parseMessage(String message){
        StringBuilder requestTypeBuilder = new StringBuilder();
        int nrSpaces = 0;
        char readChar;
        for(int i=0;i<message.length();i++){
            //We are reading the requestType
            readChar = message.charAt(i);
            if(readChar == ' '){
                nrSpaces++;
            }
            else if(nrSpaces == 0){
                requestTypeBuilder.append(readChar);
            }
            else if(nrSpaces == 1){
                String reqType = requestTypeBuilder.toString();
                String restMessage = message.substring(i);

                switch(reqType){
                    case "set":
                        requestType = RequestType.SET;
                        restOfMessage = restMessage;
                        return new Request(requestType,restMessage);
                    case "get":
                        requestType = RequestType.GET;
                        restOfMessage = restMessage;
                        keys = extractKeys(restMessage);    //We extract the keys to control if it is a get or a mget
                        if(keys.size() >1){
                            requestType = RequestType.MGET;
                            return new Request(requestType, restMessage, keys);
                        }
                        else{
                            return new Request(requestType,restMessage);
                        }
                    case "init":
                        requestType = RequestType.INIT;
                        restOfMessage = restMessage;
                        return new Request(requestType);

                    default:
                        requestType = RequestType.UNKNOWN;
                        return new Request(requestType);

                }
            }

        }
        //If it didn't recognize these patterns it is unknown
        return new Request(RequestType.UNKNOWN);
    }

    public String toString(){
        switch(requestType){
            case 1:
                return "set "+restOfMessage;
            case 2:
                return "get "+restOfMessage;
            case 3:
                return "init";
            case 4:
                return "get "+restOfMessage;
            default:
                return "UNKNOWN";

        }
    }

    //Gets all keys from the multi get / get and puts into field keys
    private List<String> extractKeys(String restMessage){
        List<String> keys = new ArrayList<>();
        StringBuilder keyBuilder = new StringBuilder();
        for(int i=0; i< restMessage.length(); i++){
            char readChar = restMessage.charAt(i);
            if(readChar == ' ' || i == restMessage.length()-1){
                if(i==restMessage.length()-1){
                    keyBuilder.append(readChar);
                }
                keys.add(keyBuilder.toString());
                keyBuilder = new StringBuilder();
            }
            else{
                keyBuilder.append(readChar);
            }
        }
        return keys;
    }
    private String createStringWithKeys(List<String> keys){
        StringBuilder rest = new StringBuilder();
        for(String key : keys){
            rest.append(key);
            rest.append(" ");
        }
        return rest.toString();
    }
}
