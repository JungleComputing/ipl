package ibis.ipl.util.rpc;

import java.lang.reflect.Proxy;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePortIdentifier;

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

   
    
    //FIXME: try to return correct class, not generic Object...
    public static Object createProxy(Class<?> interfaceClass, ReceivePortIdentifier address, Ibis ibis) {

        RPCInvocationHandler handler = new RPCInvocationHandler(interfaceClass, address, ibis);
        

    Object proxy = Proxy.newProxyInstance(interfaceClass.getClassLoader(),
            new Class[] { interfaceClass }, handler);
  
    
    return proxy;
    
    }

  //FIXME: try to return correct class, not generic Object...
    public static Object createProxy(Class<?> interfaceClass, IbisIdentifier ibisIdentifier, String name, Ibis ibis) {

    RPCInvocationHandler handler = new RPCInvocationHandler(interfaceClass, ibisIdentifier, name, ibis);
        

    Object proxy = Proxy.newProxyInstance(interfaceClass.getClassLoader(),
            new Class[] { interfaceClass }, handler);
  
    
    return proxy;
    
    }
}
