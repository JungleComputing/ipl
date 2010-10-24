package ibis.ipl.util.rpc;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

import java.io.IOException;

class RPCInvocationHandler {

    private final Ibis ibis;

    RPCInvocationHandler(Ibis ibis) {
        this.ibis = ibis;
    }

    private void client(IbisIdentifier server) throws IOException {

        // Create a send port for sending the request and connect.
        SendPort sendPort = ibis.createSendPort(RPC.rpcRequestPortType);
        sendPort.connect(server, "server");

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
        request.finish();

        ReadMessage reply = receivePort.receive();
        String result = reply.readString();
        reply.finish();

        System.err.println("server replies: " + result);

        // Close ports.
        sendPort.close();
        receivePort.close();
    }

}
