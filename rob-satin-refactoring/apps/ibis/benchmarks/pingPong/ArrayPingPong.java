/* $Id$ */


import ibis.ipl.*;

import java.util.Properties;
import java.util.Random;

import java.io.IOException;

class ArrayPingPong {
static class Sender {
    SendPort sport;
    ReceivePort rport;

    Sender(ReceivePort rport, SendPort sport) {
        this.rport = rport;
        this.sport = sport;
    }

    void send(int count, int repeat, int arraySize) throws Exception {
	double[] a = new double[arraySize];

        for (int r = 0; r < repeat; r++) {

            long time = System.currentTimeMillis();

            for (int i = 0; i < count; i++) {
                WriteMessage writeMessage = sport.newMessage();
		writeMessage.writeArray(a);
                writeMessage.finish();

                ReadMessage readMessage = rport.receive();
		readMessage.readArray(a);
                readMessage.finish();
            }

            time = System.currentTimeMillis() - time;

            double speed = (time * 1000.0) / (double) count;
	    double tp = ((count * arraySize * 8) / (1024*1024)) / (time / 1000.0);

            System.err.println(count + " calls took "
                    + (time / 1000.0) + " s, time/call = " + speed
                    + " us, throughput = " + tp + " MB/s, msg size = " + (arraySize * 8));
        }
    }
}

static class ExplicitReceiver {

    SendPort sport;

    ReceivePort rport;

    ExplicitReceiver(ReceivePort rport, SendPort sport) {
        this.rport = rport;
        this.sport = sport;
    }

    void receive(int count, int repeat, int arraySize) throws Exception {

	double[] a = new double[arraySize];

        for (int r = 0; r < repeat; r++) {
            for (int i = 0; i < count; i++) {
                ReadMessage readMessage = rport.receive();
		readMessage.readArray(a);
                readMessage.finish();

                WriteMessage writeMessage = sport.newMessage();
		writeMessage.writeArray(a);
                writeMessage.finish();
            }
        }
    }
}


    static Ibis ibis;

    static Registry registry;

    public static void connect(SendPort s, ReceivePortIdentifier ident) {
        boolean success = false;
        do {
            try {
                s.connect(ident);
                success = true;
            } catch (Exception e) {
                try {
                    Thread.sleep(500);
                } catch (Exception e2) {
                    // ignore
                }
            }
        } while (!success);
    }

    public static ReceivePortIdentifier lookup(String name) throws Exception {
        ReceivePortIdentifier temp = null;

        do {
            temp = registry.lookupReceivePort(name);

            if (temp == null) {
                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                    // ignore
                }
            }

        } while (temp == null);

        return temp;
    }

    public static void main(String[] args) {
        int count = 1000;
        int repeat = 10;
        int rank = 0, remoteRank = 1;

        try {
            StaticProperties s = new StaticProperties();
	    s.add("Serialization", "ibis");
            s.add("Communication",
                    "OneToOne, Reliable, ExplicitReceipt");
            s.add("worldmodel", "closed");
            ibis = Ibis.createIbis(s, null);

            registry = ibis.registry();

            PortType t = ibis.createPortType("test type", s);

            SendPort sport = t.createSendPort("send port");
            ReceivePort rport;

            IbisIdentifier master = registry.elect("latency");

            if (master.equals(ibis.identifier())) {
                rank = 0;
                remoteRank = 1;
            } else {
                rank = 1;
                remoteRank = 0;
            }

	    Sender sender = null;
	    ExplicitReceiver receiver = null;
            if (rank == 0) {
                    rport = t.createReceivePort("test port 0");
                    rport.enableConnections();
                    ReceivePortIdentifier ident = lookup("test port 1");
                    connect(sport, ident);
                    sender = new Sender(rport, sport);

            } else {
                ReceivePortIdentifier ident = lookup("test port 0");
                connect(sport, ident);

                    rport = t.createReceivePort("test port 1");
                    rport.enableConnections();
                    receiver = new ExplicitReceiver(rport, sport);
            }

	    int dataSize = 1;
	    for(int i=0; i<20; i++) {
		if (rank == 0) {
                    sender.send(count, repeat, dataSize);
		} else {
                    receiver.receive(count, repeat, dataSize);
		}

		dataSize *= 2;

		if(i >= 12) count /= 2;
	    }

            /* free the send ports first */
            sport.close();
            rport.close();
            ibis.end();
        } catch (Exception e) {
            System.err.println("Got exception " + e);
            System.err.println("StackTrace:");
            e.printStackTrace();
        }
    }
}
