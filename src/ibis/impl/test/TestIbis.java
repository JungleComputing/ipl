/* $Id:$ */

package ibis.impl.test;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.Registry;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.StaticProperties;
import ibis.ipl.WriteMessage;

import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Simple Ibis test, on a single Ibis instance.
 */
public final class TestIbis extends TestCase {
    static final int COUNT = 100;

    Ibis ibis;
    Registry registry;
    PortType oneToOneType;

    public static Test suite() {
        return new TestSuite(TestIbis.class);
    }

    public void testIbis() {
        try {
            StaticProperties s = new StaticProperties();
            s.add("communication", "OneToOne Reliable ExplicitReceipt");
            s.add("serialization", "ibis");
            s.add("worldmodel", "open");

            ibis = Ibis.createIbis(s, null);

            registry = ibis.registry();

            final IbisIdentifier masterId = registry.elect("master");

            boolean master = masterId.equals(ibis.identifier());

            // Since there will be only one ibis instance, master must be true
            assertTrue(master);

            oneToOneType = ibis.createPortType("one2one type", s);

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
        ReceivePort rport = oneToOneType
                .createReceivePort("master receive port");
        rport.enableConnections();
        System.out.println("Master created rport: " + rport.name());

        for (int i = 0; i < COUNT; i++) {

            ReadMessage readMessage = rport.receive();
            SendPortIdentifier origin = readMessage.origin();
            int data = readMessage.readInt();
            readMessage.finish();

            assertTrue(data == i);

            if (sendPort == null) {
                sendPort = oneToOneType.createSendPort();
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
        ReceivePort rport = oneToOneType
                .createReceivePort("client receive port");
        rport.enableConnections();
        System.out.println("Worker created rport: " + rport.name());
        SendPort sport = oneToOneType.createSendPort();

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
