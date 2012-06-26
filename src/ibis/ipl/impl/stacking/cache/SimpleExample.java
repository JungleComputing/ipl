/**
 * Test class which tests my CacheIbis implementation.
 *
 * Maricel Mihalcea
 */
package ibis.ipl.impl.stacking.cache;

import ibis.ipl.*;
import java.io.IOException;
import java.util.Properties;

public class SimpleExample {
    
    PortType portType = new PortType(
            PortType.CONNECTION_ULTRALIGHT,
            PortType.RECEIVE_EXPLICIT,
            PortType.SERIALIZATION_OBJECT,
            PortType.CONNECTION_MANY_TO_ONE);
    PortType portTypeUpcall = new PortType(
            PortType.CONNECTION_ULTRALIGHT,
            PortType.RECEIVE_AUTO_UPCALLS,
            PortType.SERIALIZATION_OBJECT,
            PortType.CONNECTION_MANY_TO_ONE);
    IbisCapabilities ibisCapabilities = new IbisCapabilities(
            IbisCapabilities.CONNECTION_CACHING,
            IbisCapabilities.ELECTIONS_STRICT);

    private static class MessageUpcaller implements MessageUpcall {

        public MessageUpcaller() {
        }

        @Override
        public void upcall(ReadMessage r) throws IOException, ClassNotFoundException {
            // Read the message.
            byte b1 = r.readByte();
            byte b2 = r.readByte();
            System.out.println("Server received: " + b1 + " and " + b2);
        }
    }

    private void server(Ibis myIbis) {
        try {
            System.out.println("\n\nI am the master!!");

            // Create a receive port and enable connections.
            ReceivePort receiver = myIbis.createReceivePort(portType, "server");
            System.out.println("Created receive port:\t" + receiver.getClass());

            receiver.enableConnections();
            System.out.println("enabled conn");

            // Read the message.
            ReadMessage r = receiver.receive();
            byte b1 = r.readByte();
            byte b2 = r.readByte();
            System.out.println("Server received: " + b1 + " and " + b2);

            // Close receive port.
            // This port doesn't close. Why?!?!?!
            // i have to force it with < 0 value to close it.
            receiver.close(-1);
            System.out.println("Port closed.");
        } catch (Exception ex) {
            System.out.println("Error at server:\n" + ex);
            ex.printStackTrace();
        }
    }

    private void serverUpcall(Ibis myIbis) {
        try {
            System.out.println("\n\nI am the master-upcall!!");

            MessageUpcall mu = new MessageUpcaller();

            // Create a receive port and enable connections.
            ReceivePort receiver = myIbis.createReceivePort(portTypeUpcall,
                    "server", mu);

            System.out.println("Created receive port:\t" + receiver.getClass());

            receiver.enableConnections();
            receiver.enableMessageUpcalls();
            System.out.println("enabled conn and msg upcalls");

            while (true) {
                synchronized (this) {
                    System.out.println("waiting for ever...");
                    this.wait();
                }
            }
        } catch (Exception ex) {
            System.out.println("Error at server upcall:" + ex);
            ex.printStackTrace();
        }
    }

    private void client(Ibis myIbis, IbisIdentifier server) {
        try {
            System.out.println("\nI am the client");

            // Create a send port for sending requests and connect.
            SendPort sender = myIbis.createSendPort(portType);
            System.out.println("Created send port:\t" + sender.getClass());

            for (int sec = 3; sec > 0; sec--) {
                System.out.println("Connecting to server in " + sec);
                Thread.sleep(1000);
            }
            sender.connect(server, "server");
            System.out.println("connected to the server");

            // Send the message.
            WriteMessage w = sender.newMessage();
            w.writeByte(Byte.MAX_VALUE);
            w.writeByte(Byte.MIN_VALUE);
            w.finish();
            System.out.println("Message finished!");

            // Close ports.
            for (int sec = 3; sec > 0; sec--) {
                System.out.println("Closing connection in " + sec);
                Thread.sleep(1000);
            }
            sender.close();
            System.out.println("Port closed.");
        } catch (Exception ex) {
            System.out.println("Error at client:\n" + ex);
            ex.printStackTrace();
        }
    }

    private void run() {
        try {
            Properties prop = new Properties();
            prop.setProperty("ibis.implementation", "cache");

            // Create an ibis instance.
            Ibis ibis = IbisFactory.createIbis(ibisCapabilities, prop, true, null,
                    portType, portTypeUpcall);

            System.out.println("\n\tInstantiated ibis version: " + ibis.getVersion());

            // Elect a server
            IbisIdentifier server = ibis.registry().elect("Server");

            System.out.println("Server is " + server);

            // If I am the server, run server, else run client.
            if (server.equals(ibis.identifier())) {
                serverUpcall(ibis);
            } else {
                client(ibis, server);
            }

            // End ibis.
            ibis.end();
        } catch (Exception ex) {
            System.out.println("Error at run:\n" + ex);
            ex.printStackTrace();
        }
    }

    public static void main(String args[]) {
        new SimpleExample().run();
    }
}
