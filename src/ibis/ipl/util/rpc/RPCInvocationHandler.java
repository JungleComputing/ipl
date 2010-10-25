package ibis.ipl.util.rpc;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

class RPCInvocationHandler implements InvocationHandler {

    private final Ibis ibis;
    private final IbisIdentifier ibisIdentifier;
    private final String name;
    
    private final Class<?> interfaceClass;

    RPCInvocationHandler(Class<?> interfaceClass, ReceivePortIdentifier address, Ibis ibis) {
        this.interfaceClass = interfaceClass;
        this.ibisIdentifier = address.ibisIdentifier();
        this.name = address.name();
        this.ibis = ibis;
    }

    public RPCInvocationHandler(Class<?> interfaceClass,
            IbisIdentifier ibisIdentifier, String name, Ibis ibis) {
        this.interfaceClass = interfaceClass;
        this.ibisIdentifier = ibisIdentifier;
        this.name = name;
        this.ibis = ibis;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        
        // Create a send port for sending the request and connect.
        SendPort sendPort = ibis.createSendPort(RPC.rpcRequestPortType);
        sendPort.connect(ibisIdentifier, name);

        // Create a receive port for receiving the reply from the server
        // this receive port does not need a name, as we will send the
        // ReceivePortIdentifier to the server directly
        ReceivePort receivePort = ibis.createReceivePort(RPC.rpcReplyPortType,
                null);
        receivePort.enableConnections();

        // Send the request message. This message contains the identifier of
        // our receive port so the server knows where to send the reply
        WriteMessage request = sendPort.newMessage();
        request.writeObject(receivePort.identifier());
        request.writeString(method.getName());
        request.writeObject(method.getParameterTypes());
        request.writeObject(args);
        request.finish();

        ReadMessage reply = receivePort.receive();
        Object result = reply.readObject();
        reply.finish();

        System.err.println("server replies: " + result);

        // Close ports.
        sendPort.close();
        receivePort.close();

        return result;
    }

}
