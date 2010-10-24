package ibis.ipl.util.rpc;

import ibis.ipl.Ibis;
import ibis.ipl.MessageUpcall;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

import java.io.IOException;
import java.util.Date;

class RemoteObject implements MessageUpcall {
    
    private final ReceivePort receivePort;
    
    private final Ibis ibis;
    
    RemoteObject(Ibis ibis, String name, Object object, Class<?> interfaceClass ) throws IOException {
        this.ibis = ibis;
        
        receivePort = ibis.createReceivePort(RPC.rpcRequestPortType,
                name, this);
        // enable connections
        receivePort.enableConnections();
        // enable upcalls
        receivePort.enableMessageUpcalls();

        System.err.println("remote object " + receivePort.name() + " created");

    }
    
    void unexport() throws IOException {
        receivePort.close();
    }
    
    
    

	 /**
     * Function called by Ibis to give us a newly arrived message. This message
     * will contain the ReceivePortIdentifier of the receive port of the ibis
     * that send the request. We connect to this receive port, and send the
     * reply.
     */
    public void upcall(ReadMessage message) throws IOException,
            ClassNotFoundException {
        ReceivePortIdentifier requestor = (ReceivePortIdentifier) message
                .readObject();

        System.err.println("received request from: " + requestor);

        // finish the request message. This MUST be done before sending
        // the reply message. It ALSO means Ibis may now call this upcall
        // method agian with the next request message
        message.finish();

        // create a sendport for the reply
        SendPort replyPort = ibis.createSendPort(RPC.rpcReplyPortType);

        // connect to the requestor's receive port
        replyPort.connect(requestor);

        // create a reply message
        WriteMessage reply = replyPort.newMessage();
        reply.writeString("the time is " + new Date());
        reply.finish();

        replyPort.close();
    }

}
