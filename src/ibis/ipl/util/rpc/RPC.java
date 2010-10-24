package ibis.ipl.util.rpc;

import ibis.ipl.PortType;

public class RPC {
	
	/**
     * Port type used for sending a request to the server.
     */
    public static final PortType rpcRequestPortType = new PortType(PortType.COMMUNICATION_RELIABLE,
            PortType.SERIALIZATION_OBJECT, PortType.RECEIVE_AUTO_UPCALLS,
            PortType.CONNECTION_MANY_TO_ONE);

    /**
     * Port type used for sending a reply back.
     */
    public static final PortType rpcReplyPortType = new PortType(PortType.COMMUNICATION_RELIABLE,
            PortType.SERIALIZATION_OBJECT, PortType.RECEIVE_EXPLICIT,
            PortType.CONNECTION_ONE_TO_ONE);

    public static final PortType[] rpcPortTypes = {rpcRequestPortType, rpcReplyPortType};

   

    
  


}
