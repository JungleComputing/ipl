package ibis.server.remote;

public class Protocol {

    public static final String OPCODE_GET_LOCAL_ADDRESS = "OPCODE_GET_LOCAL_ADDRESS";
    
    public static final String OPCODE_ADD_HUB = "OPCODE_ADD_HUB";

    public static final String OPCODE_GET_HUBS = "OPCODE_GET_HUBS";

    public static final String OPCODE_GET_SERVICE_NAMES = "OPCODE_GET_SERVICE_NAMES";
    
    public static final String OPCODE_GET_STATISTICS = "OPCODE_GET_STATISTICS";
    
    public static final String OPCODE_END = "OPCODE_END";

    public static final String REPLY_OK = "OK";

    public static final String REPLY_ERROR = "ERROR";
    
    public static final String CLIENT_COMMAND = "CLIENT_COMMAND:";
    
    public static final String SERVER_REPLY = "SERVER_REPLY:"; 

}
