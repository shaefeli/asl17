package ch.ethz.asltest;

/**
 * Created by simon on 06.10.17.
 */
public class Request {
    //code 1: SET
    //code 2: GET
    //code 3: MULTI-GET
    //code 0: UNKNOWN
    public int requestType=0;
    public String key = "";
    public int flags = 0;
    public int exptime = 0;
    //Length of the data put in the message (0 except for set)
    public int bytes = 0;
    //Only for set
    public String value = "";

    public Request(String message){
        //Parse the request

        //Mock request for testing
        requestType = 1;
        key = "cazzo";
        flags=0;
        exptime=0;
        bytes=5;
        value="cazza";

    }
    public String toString(){
        StringBuilder requestBuilding = new StringBuilder();

        if(requestType == RequestType.get) {
            requestBuilding.append("GET");
        }
        else if(requestType == RequestType.set) {
            requestBuilding.append("SET");
        }
        else if(requestType == RequestType.multiget) {
            requestBuilding.append("GETs");
        }

        requestBuilding.append(" "+key+" "+flags+" "+exptime+" "+bytes+"\r\n");
        if(requestType == RequestType.set) {
            requestBuilding.append(value + "\r\n");
        }
        return requestBuilding.toString();
    }
}
