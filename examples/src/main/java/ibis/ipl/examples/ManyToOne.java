package ibis.ipl.examples;

import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.MessageUpcall;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

import java.io.IOException;

/**
 * Example of a many-to-one application. The server just waits until messages
 * are received and prints them, clients send a single message to the server and
 * exit.
 */

public class ManyToOne implements MessageUpcall {

    PortType portType = new PortType(PortType.COMMUNICATION_RELIABLE,
            PortType.SERIALIZATION_DATA, PortType.RECEIVE_AUTO_UPCALLS,
            PortType.CONNECTION_MANY_TO_ONE);

    IbisCapabilities ibisCapabilities = new IbisCapabilities(
            IbisCapabilities.ELECTIONS_STRICT);

    /**
     * Function called by Ibis to give us a newly arrived message
     * 
     * @param message
     *            the message
     * @throws IOException
     *             when the message cannot be read
     */
    public void upcall(ReadMessage message) throws IOException {
        String s = message.readString();
        System.out.println(message.origin() + " says: " + s);
    }

    private void server(Ibis myIbis) throws Exception {

        // Create a receive port, pass ourselves as the message upcall
        // handler
        ReceivePort receiver = myIbis.createReceivePort(portType, "server",
                this);
        // enable connections
        receiver.enableConnections();
        // enable upcalls
        receiver.enableMessageUpcalls();

        // do nothing for a minute (will get upcalls for messages
        Thread.sleep(60000);

        // Close receive port.
        receiver.close();
    }

    private void client(Ibis myIbis, IbisIdentifier server) throws IOException {

        // Create a send port for sending requests and connect.
        SendPort sender = myIbis.createSendPort(portType);
        sender.connect(server, "server");

        // Send the message.
        WriteMessage w = sender.newMessage();
        w.writeString("Hi there");
        w.finish();

        // Close ports.
        sender.close();
    }

    private void run() throws Exception {
        // Create an ibis instance.
        Ibis ibis = IbisFactory.createIbis(ibisCapabilities, null, portType);

        // Elect a server
        IbisIdentifier server = ibis.registry().elect("Server");

        // If I am the server, run server, else run client.
        if (server.equals(ibis.identifier())) {
            server(ibis);
        } else {
            client(ibis, server);
        }

        // End ibis.
        ibis.end();
    }

    public static void main(String args[]) {
        try {
            new ManyToOne().run();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
}
