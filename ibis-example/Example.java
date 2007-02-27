/* $Id$ */


import ibis.ipl.CapabilitySet;
import ibis.ipl.Ibis;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.Registry;
import ibis.ipl.SendPort;
import ibis.ipl.Upcall;
import ibis.ipl.WriteMessage;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * This is basically the example RPC program described in the Ibis
 * Programmers manual. A server computes lengths of strings, and
 * a client supplies the server with strings.
 */
public class Example implements ibis.ipl.PredefinedCapabilities {

    /**
     * The port type for both send and receive ports.
     */
    PortType porttype;

    /**
     * Registry where receive ports are registered.
     */
    Registry rgstry;

    /**
     * When the client is done, it will send a null string, which is
     * a signal for the server to terminate. The finish field is set
     * to true when a null string is received.
     */
    boolean finish = false;

    boolean failure = false;

    /**
     * Upcall handler class for the server.
     */
    private class RpcUpcall implements Upcall {
        /**
         */
        SendPort serverSender;

        RpcUpcall(SendPort p) {
            serverSender = p;
        }

        public void upcall(ReadMessage m) throws IOException {
            String s;

            try {
                s = (String) m.readObject();
            } catch (ClassNotFoundException e) {
                s = null;
            }
            m.finish();

            if (s == null) {
                synchronized (this) {
                    finish = true;
                    notifyAll();
                }
                return;
            }
            int len = s.length();
            WriteMessage w = serverSender.newMessage();
            w.writeInt(len);
            w.finish();
        }
    }

    private void server(IbisIdentifier client) {
        try {
            // Create a send port for sending answers
            SendPort serverSender = porttype.createSendPort();

            // Create an upcall handler
            RpcUpcall rpcUpcall = new RpcUpcall(serverSender);
            ReceivePort serverReceiver = porttype.createReceivePort("server",
                    rpcUpcall);
            serverReceiver.enableConnections();
            serverReceiver.enableUpcalls();

            serverSender.connect(client, "client");

            // Wait until finished
            synchronized (rpcUpcall) {
                while (!finish) {
                    try {
                        rpcUpcall.wait();
                    } catch (InterruptedException e) {
                        // ignored
                    }
                }
            }

            // System.out.println("Server done");

            // Close ports
            serverSender.close();
            serverReceiver.close();

            // System.out.println("Server closed ports");

        } catch (Exception e) {
            System.err.println("Server got exception: " + e);
            failure = true;
        }
    }

    private void client(IbisIdentifier server) {
        try {
            // Create a send port for sending requests
            SendPort clientSender = porttype.createSendPort();

            // Create a receive port for receiving answers
            ReceivePort clientReceiver = porttype.createReceivePort("client");
            clientReceiver.enableConnections();

            // Connect send port
            clientSender.connect(server, "server");

            FileReader f = new FileReader("Example.java");
            BufferedReader bf = new BufferedReader(f);

            // For every line in this file, compute its length by consulting
            // the server.
            String s;
            do {
                s = bf.readLine();
                WriteMessage w = clientSender.newMessage();
                w.writeObject(s);
                w.finish();
                if (s != null) {
                    ReadMessage r = clientReceiver.receive();
                    int len = r.readInt();
                    r.finish();
                    // System.out.println(s + ": " + len);
                    if (len != s.length()) {
                        System.err.println("String: \"" + s
                                + "\", expected length " + s.length()
                                + ", got length " + len);
                        failure = true;
                    }
                }
            } while (s != null);

            // System.out.println("Client is done");

            // Close ports
            clientSender.close();
            clientReceiver.close();

            // System.out.println("Client closed ports");

        } catch (IOException e) {
            System.err.println("Client got exception: " + e);
            failure = true;
        }
    }

    private void run() {
        // Create properties for the Ibis to be created.
        CapabilitySet props = new CapabilitySet(
                SERIALIZATION_OBJECT, WORLDMODEL_CLOSED,
                COMMUNICATION_RELIABLE, RECEIVE_EXPLICIT, RECEIVE_AUTO_UPCALLS
        );

        // Create an Ibis
        final Ibis ibis;
        try {
            ibis = IbisFactory.createIbis(props, null, null, null);
        } catch (Exception e) {
            System.err.println("Could not create Ibis: " + e);
            e.printStackTrace();
            failure = true;
            return;
        }

        // Install shutdown hook that terminates ibis
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    ibis.end();
                } catch (IOException e) {
                    // Ignored
                }
            }
        });

        // Create properties for the port type
        CapabilitySet portprops = new CapabilitySet(
            COMMUNICATION_RELIABLE, SERIALIZATION_OBJECT,
            RECEIVE_EXPLICIT, RECEIVE_AUTO_UPCALLS
        );

        // Create the port type
        try {
            porttype = ibis.createPortType(portprops);
        } catch (Exception e) {
            System.err.println("Could not create port type: " + e);
            failure = true;
            return;
        }

        // Elect a server
        IbisIdentifier me = ibis.identifier();
        rgstry = ibis.registry();
        IbisIdentifier server = null;
        IbisIdentifier client = null;
        boolean i_am_server = false;
        try {
            server = rgstry.elect("Server");
            if (server.equals(me)) {
                client = rgstry.getElectionResult("Client");
                i_am_server = true;
            } else {
                rgstry.elect("Client");
            }
        } catch (Exception e) {
            System.err.println("Could not elect: " + e);
            failure = true;
            return;
        }

        // Start either a server or a client.
        if (i_am_server) {
            server(client);
        } else {
            client(server);
        }
    }

    public static void main(String args[]) {
        Example ex = new Example();
        ex.run();
        if (ex.failure) {
            System.exit(1);
        }
        System.out.println("Test succeeded!");
        System.exit(0); // let shutdown hook terminate ibis
    }
}
