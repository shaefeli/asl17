package ch.ethz.asltest;

/**
 * Created by Simon on 06.10.17.
 */
public class Request {

    //code 1: SET
    //code 2: GET
    //code 3: MULTI-GET
    //code 4: INIT
    //code 5: SET VALUE
    //code 0: UNKNOWN
    public int requestType =0;
    public String restOfMessage = "";

    public Request(String message){
        parseMessage(message);
    }

    public Request(int requestType){
        this.requestType = requestType;
    }

    public Request(int requestType, String restMessage){
        this.restOfMessage = restMessage;
        this.requestType = requestType;
    }
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
                if(reqType.equals("set")){
                    requestType = 1;
                    restOfMessage = restMessage;
                    return new Request(requestType,restMessage);
                }
                else if(reqType.equals("get")){
                    requestType = 2;
                    restOfMessage = restMessage;
                    return new Request(requestType,restMessage);
                }
                else if(reqType.equals("gets")){
                    requestType = 3;
                    restOfMessage = restMessage;
                    return new Request(requestType,restMessage);
                }
                else if(reqType.equals("init")){
                    requestType = 4;
                    restOfMessage = restMessage;
                    return new Request(requestType);
                }
                else{
                    requestType = 0;
                    return new Request(requestType);
                }
            }

        }
        return new Request(0);
    }
    public String toString(){
        switch(requestType){
            case 1:
                return "set "+restOfMessage;
            case 2:
                return "get "+restOfMessage;
            case 3:
                return "gets "+restOfMessage;
            case 4:
                return "init";
            case 5:
                return "";
            default:
                return "UNKNOWN";

        }
    }
}
