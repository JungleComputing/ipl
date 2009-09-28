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
 * This program is to be run as two instances. One is a server, the other a
 * client. The client sends a hello message to the server. The server prints it.
 * This version uses upcalls to receive messages.
 */

public class HelloUpcall implements MessageUpcall {

    PortType portType = new PortType(PortType.COMMUNICATION_RELIABLE,
            PortType.SERIALIZATION_DATA, PortType.RECEIVE_AUTO_UPCALLS,
            PortType.CONNECTION_ONE_TO_ONE);

    IbisCapabilities ibisCapabilities = new IbisCapabilities(
            IbisCapabilities.ELECTIONS_STRICT);

    /** Set to true when server received message. */
    boolean finished = false;

    /**
     * Function called by Ibis to give us a newly arrived message.
     * 
     * @param message
     *            the message
     * @throws IOException
     *             when the message cannot be read
     */
    public void upcall(ReadMessage message) throws IOException {
        String s = message.readString();
        System.out.println("Received string: " + s);
        setFinished();
    }

    synchronized void setFinished() {
        finished = true;
        notifyAll();
    }

    private void server(Ibis myIbis) throws IOException {

        // Create a receive port, pass ourselves as the message upcall
        // handler
        ReceivePort receiver = myIbis.createReceivePort(portType, "server",
                this);
        // enable connections
        receiver.enableConnections();
        // enable upcalls
        receiver.enableMessageUpcalls();

        synchronized (this) {
            while (!finished) {
                try {
                    wait();
                } catch (Exception e) {
                    // ignored
                }
            }
        }

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
            new HelloUpcall().run();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
}
