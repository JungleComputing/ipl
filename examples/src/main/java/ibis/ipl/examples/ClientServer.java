package ibis.ipl.examples;

import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.MessageUpcall;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

import java.io.IOException;
import java.util.Date;

/**
 * Example of a client application. The server waits until a request comes in,
 * and sends a reply (in this case the current time). This application shows a
 * combination of two port types. One is a many-to-one port with upcalls, the
 * other a one-to-one port with explicit receive.
 */

public class ClientServer implements MessageUpcall {

    /**
     * Port type used for sending a request to the server
     */
    PortType requestPortType = new PortType(PortType.COMMUNICATION_RELIABLE,
            PortType.SERIALIZATION_OBJECT, PortType.RECEIVE_AUTO_UPCALLS,
            PortType.CONNECTION_MANY_TO_ONE);

    /**
     * Port type used for sending a reply back
     */
    PortType replyPortType = new PortType(PortType.COMMUNICATION_RELIABLE,
            PortType.SERIALIZATION_DATA, PortType.RECEIVE_EXPLICIT,
            PortType.CONNECTION_ONE_TO_ONE);

    IbisCapabilities ibisCapabilities = new IbisCapabilities(
            IbisCapabilities.ELECTIONS_STRICT);

    private final Ibis myIbis;

    /**
     * Constructor. Actually does all the work too :)
     */
    private ClientServer() throws Exception {
        // Create an ibis instance.
        // Notice createIbis uses varargs for its parameters.
        myIbis = IbisFactory.createIbis(ibisCapabilities, null,
                requestPortType, replyPortType);

        // Elect a server
        IbisIdentifier server = myIbis.registry().elect("Server");

        // If I am the server, run server, else run client.
        if (server.equals(myIbis.identifier())) {
            server();
        } else {
            client(server);
        }

        // End ibis.
        myIbis.end();
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
        SendPort replyPort = myIbis.createSendPort(replyPortType);

        // connect to the requestor's receive port
        replyPort.connect(requestor);

        // create a reply message
        WriteMessage reply = replyPort.newMessage();
        reply.writeString("the time is " + new Date());
        reply.finish();

        replyPort.close();
    }

    private void server() throws Exception {

        // Create a receive port, pass ourselves as the message upcall
        // handler
        ReceivePort receiver = myIbis.createReceivePort(requestPortType,
                "server", this);
        // enable connections
        receiver.enableConnections();
        // enable upcalls
        receiver.enableMessageUpcalls();

        System.err.println("server started");

        // do nothing for a minute (will get upcalls for messages
        Thread.sleep(60000);

        // Close receive port.
        receiver.close();

        System.err.println("server stopped");
    }

    private void client(IbisIdentifier server) throws IOException {

        // Create a send port for sending the request and connect.
        SendPort sendPort = myIbis.createSendPort(requestPortType);
        sendPort.connect(server, "server");

        // Create a receive port for receiving the reply from the server
        // this receive port does not need a name, as we will send the
        // ReceivePortIdentifier to the server directly
        ReceivePort receivePort = myIbis.createReceivePort(replyPortType, null);
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

    public static void main(String args[]) {
        try {
            new ClientServer();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
}
