/* $Id:$ */

package ibis.impl.test;

import ibis.ipl.CapabilitySet;
import ibis.ipl.Ibis;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.Registry;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.WriteMessage;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Simple Ibis test, on a single Ibis instance.
 */
public final class TestIbis extends TestCase
        implements ibis.ipl.PredefinedCapabilities {
    static final int COUNT = 100;

    Ibis ibis;
    Registry registry;
    CapabilitySet oneToOneType;

    public static Test suite() {
        return new TestSuite(TestIbis.class);
    }

    public void testIbis() {
        try {
            oneToOneType = new CapabilitySet(
                COMMUNICATION_RELIABLE, SERIALIZATION_OBJECT, RECEIVE_EXPLICIT,
                CONNECTION_ONE_TO_ONE);

            ibis = IbisFactory.createIbis(oneToOneType, null, null, null);

            registry = ibis.registry();

            final IbisIdentifier masterId = registry.elect("master");

            boolean master = masterId.equals(ibis.identifier());

            // Since there will be only one ibis instance, master must be true
            assertTrue(master);

            Thread masterThread = new Thread("Master thread") {
                public void run() {
                    try {
                        System.out.println("Starting master ...");
                        master();
                    } catch(Exception e) {
                        System.err.println("master caught exception: " + e);
                        e.printStackTrace();
                        assertTrue(false);
                    }
                }
            };

            masterThread.start();

            Thread workerThread = new Thread("Worker thread") {
                public void run() {
                    try {
                        System.out.println("Starting worker ...");
                        worker(masterId);
                    } catch(Exception e) {
                        System.err.println("worker caught exception: " + e);
                        e.printStackTrace();
                        assertTrue(false);
                    }
                }
            };

            workerThread.start();

            workerThread.join();
            System.out.println("Worker finished ..."); 
            masterThread.join();
            System.out.println("Master finished ..."); 

            ibis.end();

        } catch (Throwable e) {
            System.err.println("testIbis caught exception: " + e);
            e.printStackTrace();
            assertTrue(false);
        }
    }

    public void connect(SendPort s, ReceivePortIdentifier ident) {
        try {
            s.connect(ident, 5000);
        } catch (Exception e) {
            System.err.println("connect caught exception: " + e);
            e.printStackTrace();
            assertTrue(false);
        }
    }

    public void connect(SendPort s, IbisIdentifier id, String name) {
        try {
            s.connect(id, name, 5000);
        } catch (Exception e) {
            System.err.println("connect caught exception: " + e);
            e.printStackTrace();
            assertTrue(false);
        }
    }

    void master() throws Exception {
        SendPort sendPort = null;
        ReceivePort rport = ibis.createReceivePort(oneToOneType,
                "master receive port");
        rport.enableConnections();
        System.out.println("Master created rport: " + rport.name());

        for (int i = 0; i < COUNT; i++) {

            ReadMessage readMessage = rport.receive();
            SendPortIdentifier origin = readMessage.origin();
            int data = readMessage.readInt();
            readMessage.finish();

            assertTrue(data == i);

            if (sendPort == null) {
                sendPort = ibis.createSendPort(oneToOneType);
                connect(sendPort, origin.ibis(), "client receive port");
            }

            WriteMessage writeMessage = sendPort.newMessage();
            writeMessage.writeInt(data);
            writeMessage.finish();
        }
        sendPort.close();
        rport.close();
    }

    void worker(IbisIdentifier masterId) throws Exception {
        ReceivePort rport = ibis.createReceivePort(oneToOneType,
                "client receive port");
        rport.enableConnections();
        System.out.println("Worker created rport: " + rport.name());
        SendPort sport = ibis.createSendPort(oneToOneType);

        connect(sport, masterId, "master receive port");

        for (int i = 0; i < COUNT; i++) {
            WriteMessage writeMessage = sport.newMessage();
            writeMessage.writeInt(i);
            writeMessage.finish();

            ReadMessage readMessage = rport.receive();
            int n = readMessage.readInt();
            readMessage.finish();
            assertTrue(n == i);
        }

        sport.close();
        rport.close();
    }
}
