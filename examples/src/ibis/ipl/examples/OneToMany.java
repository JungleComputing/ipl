/* $Id: Hello.java 6430 2007-09-20 16:37:59Z ceriel $ */

package ibis.ipl.examples;

import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.MessageUpcall;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

import java.io.IOException;
import java.util.Date;

/**
 * Example application showing one-to-many communication. One of the Ibisis in
 * the pool (determined by an election) send out the time to all other members
 * of the pool.
 */

public class OneToMany implements MessageUpcall {

    PortType portType =
        new PortType(PortType.COMMUNICATION_RELIABLE,
                PortType.SERIALIZATION_DATA, PortType.RECEIVE_AUTO_UPCALLS,
                PortType.CONNECTION_ONE_TO_MANY);

    IbisCapabilities ibisCapabilities =
        new IbisCapabilities(IbisCapabilities.ELECTIONS_STRICT,
                IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED);

    private void server(Ibis myIbis) throws Exception {
        //create a sendport to send messages with
        SendPort sendPort = myIbis.createSendPort(portType);
        
        //ones every second, send the time to all the members in the pool
        //including ourselves
        for (int i = 0; i < 30; i++) {
            IbisIdentifier[] joinedIbises = myIbis.registry().joinedIbises();
            for (IbisIdentifier joinedIbis: joinedIbises) {
                sendPort.connect(joinedIbis, "receive port");
            }

            WriteMessage message = sendPort.newMessage();
            message.writeString("The time is now: " + new Date());
            message.finish();
            
            Thread.sleep(1000);
        }
        
        sendPort.close();
    }

  
    /**
     * Client function. Pretends to be busy for 30 seconds, and exits.
     */
    private void client(Ibis myIbis, IbisIdentifier server) throws Exception {
        // no nothing for a while
        Thread.sleep(30000);
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
        
        //create a receive port to receive messages with
        ReceivePort receiver =
            ibis.createReceivePort(portType, "receive port", this);
        //enable connection to our receive port
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

        //close receive port
        receiver.close();
        
        // End ibis.
        ibis.end();
    }

    public static void main(String args[]) {
        try {
            new OneToMany().run();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

}
