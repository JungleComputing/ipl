package ibis.ipl.apps.benchmarks.biglatency;

/* $Id$ */


import ibis.ipl.CapabilitySet;
import ibis.ipl.Ibis;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.MessageUpcall;
import ibis.ipl.PredefinedCapabilities;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.Registry;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

import java.io.IOException;

class Sender {

    SendPort sport;

    ReceivePort rport;

    Sender(ReceivePort rport, SendPort sport) {
        this.rport = rport;
        this.sport = sport;
    }

    void send(int count) throws IOException {
        // warmup
        for (int i = 0; i < count; i++) {
            WriteMessage writeMessage = sport.newMessage();
            writeMessage.writeByte((byte) 0);
            writeMessage.writeByte((byte) 1);
            writeMessage.writeInt(2);
            writeMessage.writeInt(3);
            writeMessage.writeInt(4);
            writeMessage.writeInt(5);
            writeMessage.writeInt(6);
            writeMessage.finish();

            ReadMessage readMessage = rport.receive();
            readMessage.readByte();
            readMessage.readByte();
            readMessage.finish();
        }

        // test
        long time = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            WriteMessage writeMessage = sport.newMessage();
            writeMessage.writeByte((byte) 0);
            writeMessage.writeByte((byte) 1);
            writeMessage.writeInt(2);
            writeMessage.writeInt(3);
            writeMessage.writeInt(4);
            writeMessage.writeInt(5);
            writeMessage.writeInt(6);
            writeMessage.finish();

            ReadMessage readMessage = rport.receive();
            readMessage.readByte();
            readMessage.readByte();
            readMessage.finish();
        }

        time = System.currentTimeMillis() - time;

        double speed = (time * 1000.0) / (double) count;
        System.err.println("Latency: " + count + " calls took "
                + (time / 1000.0) + " seconds, time/call = " + speed
                + " micros");
    }
}

class ExplicitReceiver {

    SendPort sport;

    ReceivePort rport;

    ExplicitReceiver(ReceivePort rport, SendPort sport) {
        this.rport = rport;
        this.sport = sport;
    }

    void receive(int count) throws IOException {
        for (int i = 0; i < count; i++) {
            ReadMessage readMessage = rport.receive();
            readMessage.readByte();
            readMessage.readByte();
            readMessage.readInt();
            readMessage.readInt();
            readMessage.readInt();
            readMessage.readInt();
            readMessage.readInt();
            readMessage.finish();

            WriteMessage writeMessage = sport.newMessage();
            writeMessage.writeByte((byte) 0);
            writeMessage.writeByte((byte) 1);
            writeMessage.finish();
        }
    }
}

class UpcallReceiver implements MessageUpcall {

    SendPort sport;

    int count = 0;

    int max;

    UpcallReceiver(SendPort sport, int max) {
        this.sport = sport;
        this.max = max;
    }

    public void upcall(ReadMessage readMessage) {

        try {
            readMessage.readByte();
            readMessage.readByte();
            readMessage.readInt();
            readMessage.readInt();
            readMessage.readInt();
            readMessage.readInt();
            readMessage.readInt();
            readMessage.finish();

            WriteMessage writeMessage = sport.newMessage();
            writeMessage.writeByte((byte) 0);
            writeMessage.writeByte((byte) 1);
            writeMessage.finish();

            count++;

            if (count == max) {
                synchronized (this) {
                    notifyAll();
                }
            }

        } catch (Exception e) {
            System.out.println("EEEEEK " + e);
            e.printStackTrace();
        }
    }

    synchronized void finish() {
        while (count < max) {
            try {
                wait();
            } catch (Exception e) {
            }
        }

        System.err.println("Finished");

    }
}

class Latency implements PredefinedCapabilities {

    static Ibis ibis;

    static Registry registry;

    public static void main(String[] args) {
        /* Parse commandline. */

        boolean upcall = false;

        int count = Integer.parseInt(args[0]);
        int rank;

        if (args.length > 1) {
            upcall = args[1].equals("-u");
        }

        try {
            CapabilitySet p = new CapabilitySet(SERIALIZATION_OBJECT,
                    COMMUNICATION_RELIABLE,
                    CONNECTION_ONE_TO_ONE,
                    RECEIVE_AUTO_UPCALLS, RECEIVE_EXPLICIT);
            ibis = IbisFactory.createIbis(p, null, null, null);
            registry = ibis.registry();
            CapabilitySet t = p;

            SendPort sport = ibis.createSendPort(t);
            ReceivePort rport;
            IbisIdentifier master = registry.elect("latency");
            IbisIdentifier remote;
            if (master.equals(ibis.identifier())) {
                rank = 0;
                remote = registry.getElectionResult("client");
            } else {
                rank = 1;
                registry.elect("client");
                remote = master;
            }

            if (rank == 0) {

                rport = ibis.createReceivePort(t, "test port");
                rport.enableConnections();
                sport.connect(remote, "test port");
                Sender sender = new Sender(rport, sport);
                sender.send(count);

            } else {
                sport.connect(remote, "test port");

                if (upcall) {
                    UpcallReceiver receiver = new UpcallReceiver(sport,
                            2 * count);
                    rport = ibis.createReceivePort(t, "test port", receiver);
                    rport.enableConnections();
                    rport.enableMessageUpcalls();
                    receiver.finish();
                } else {
                    rport = ibis.createReceivePort(t, "test port");
                    rport.enableConnections();
                    ExplicitReceiver receiver = new ExplicitReceiver(rport,
                            sport);
                    receiver.receive(2 * count);
                }
            }

            /* free the send ports first */
            sport.close();
            rport.close();
            ibis.end();

            System.exit(0);

        } catch (Exception e) {
            System.out.println("Got exception " + e);
            System.out.println("StackTrace:");
            e.printStackTrace();
        }
    }
}
