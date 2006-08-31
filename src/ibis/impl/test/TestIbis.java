/* $Id:$ */

package ibis.impl.test;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;

import java.util.HashMap;
import java.io.IOException;

import ibis.ipl.*;
import ibis.io.*;
import ibis.util.*;

/**
 * Simple Ibis test, on a single Ibis instance.
 */
public final class TestIbis extends TestCase {
    static final int COUNT = 10000;

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

            boolean master = registry.elect("master").equals(ibis.identifier());

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
                        worker();
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

    public ReceivePortIdentifier lookup(String name) throws IOException {
        try {
            ReceivePortIdentifier temp = registry.lookupReceivePort(name, 5000);
            assertTrue(temp != null);
            return temp;
        } catch (Exception e) {
            System.err.println("lookup caught exception: " + e);
            e.printStackTrace();
            assertTrue(false);
        }
        return null;
    }

    void master() throws Exception {
        SendPort sendPort = null;
        ReceivePort rport = oneToOneType
                .createReceivePort("master receive port");
        rport.enableConnections();

        for (int i = 0; i < COUNT; i++) {

            ReadMessage readMessage = rport.receive();
            SendPortIdentifier origin = readMessage.origin();
            int data = readMessage.readInt();
            readMessage.finish();

            assertTrue(data == i);

            if (sendPort == null) {
                ReceivePortIdentifier peer = lookup("receiveport @ " + origin.ibis());
                sendPort = oneToOneType.createSendPort();
                connect(sendPort, peer);
            }

            WriteMessage writeMessage = sendPort.newMessage();
            writeMessage.writeInt(data);
            writeMessage.finish();
        }
        sendPort.close();
        rport.close();
    }

    void worker() throws Exception {
        ReceivePort rport = oneToOneType.createReceivePort("receiveport @ "
                + ibis.identifier());
        rport.enableConnections();
        SendPort sport = oneToOneType.createSendPort();

        ReceivePortIdentifier master = lookup("master receive port");

        connect(sport, master);

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
