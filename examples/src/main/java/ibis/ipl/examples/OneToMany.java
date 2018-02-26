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
 * Example application showing one-to-many communication. One of the Ibises in
 * the pool (determined by an election) sends out the time to all other members
 * of the pool.
 */

public class OneToMany implements MessageUpcall {

    PortType portType = new PortType(PortType.COMMUNICATION_RELIABLE,
            PortType.SERIALIZATION_DATA, PortType.RECEIVE_AUTO_UPCALLS,
            PortType.CONNECTION_ONE_TO_MANY, PortType.CONNECTION_DOWNCALLS);

    IbisCapabilities ibisCapabilities = new IbisCapabilities(
            IbisCapabilities.ELECTIONS_STRICT,
            IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED);

    private void server(Ibis myIbis) throws Exception {
        // create a sendport to send messages with
        SendPort sendPort = myIbis.createSendPort(portType);

        // ones every 10 seconds, send the time to all the members in the pool
        // including ourselves. Stops after two minutes (12 * 10 seconds)
        for (int i = 0; i < 12; i++) {
            IbisIdentifier[] joinedIbises = myIbis.registry().joinedIbises();
            for (IbisIdentifier joinedIbis : joinedIbises) {
                sendPort.connect(joinedIbis, "receive port");
            }

            // if a connection is lost because a Ibis leave while we are
            // sending, print the error. The message will still be send to all
            // the remaining ibises.
            try {
                System.err.println("broadcasting time message");
                WriteMessage message = sendPort.newMessage();
                message.writeString("The current time is: " + new Date());
                message.finish();
            } catch (IOException e) {
                System.err.println("error when sending message: " + e);
            }

            // poll the sendport for any connections that have been lost
            ReceivePortIdentifier[] lostConnections = sendPort
                    .lostConnections();
            for (ReceivePortIdentifier receiver : lostConnections) {
                System.err.println("lost connection to: " + receiver);
            }

            Thread.sleep(10000);
        }

        sendPort.close();
    }

    /**
     * Client function. Pretends to be busy for a while, and exits.
     */
    private void client(Ibis myIbis, IbisIdentifier server) throws Exception {
        // no nothing for a while
        Thread.sleep(120000);
    }

    /**
     * Function called by ibis whenever a message is received.
     */
    public void upcall(ReadMessage readMessage) throws IOException,
            ClassNotFoundException {
        String message = readMessage.readString();

        System.err.println("Received message: " + message);
    }

    private void run() throws Exception {
        // Create an ibis instance.
        Ibis ibis = IbisFactory.createIbis(ibisCapabilities, null, portType);

        // create a receive port to receive messages with
        ReceivePort receiver = ibis.createReceivePort(portType, "receive port",
                this);
        // enable connection to our receive port
        receiver.enableConnections();

        // enable upcalls for messages
        receiver.enableMessageUpcalls();

        // Elect a server
        IbisIdentifier server = ibis.registry().elect("Server");

        // If I am the server, run server, else run client.
        if (server.equals(ibis.identifier())) {
            server(ibis);
        } else {
            client(ibis, server);
        }

        // close receive port
        receiver.close();

        // End ibis.
        ibis.end();

        ibis.printManagementProperties(System.err);
    }

    public static void main(String args[]) {
        try {
            new OneToMany().run();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

}
