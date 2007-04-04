/* $Id$ */


import ibis.ipl.*;

import java.util.Properties;
import java.util.Random;

import java.io.IOException;
class ObjectPingPong implements PredefinedCapabilities {

static class Aap implements java.io.Serializable {
    byte b;
}

static class Sender {
    SendPort sport;
    ReceivePort rport;

    Sender(ReceivePort rport, SendPort sport) {
        this.rport = rport;
        this.sport = sport;
    }

    void send(int count, int repeat) throws Exception {
	Aap x = new Aap();
	Aap y = new Aap();

        for (int r = 0; r < repeat; r++) {

            long time = System.currentTimeMillis();

            for (int i = 0; i < count; i++) {
                WriteMessage writeMessage = sport.newMessage();
		writeMessage.writeObject(x);
//		writeMessage.writeObject(y);

                writeMessage.finish();


                ReadMessage readMessage = rport.receive();
                readMessage.finish();
            }

            time = System.currentTimeMillis() - time;

            double speed = (time * 1000.0) / (double) count;
            System.err.println("Latency: " + count + " calls took "
                    + (time / 1000.0) + " seconds, time/call = " + speed
                    + " micros");
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

    void receive(int count, int repeat) throws IOException {

        for (int r = 0; r < repeat; r++) {
            for (int i = 0; i < count; i++) {

                ReadMessage readMessage = rport.receive();
		try {
		    readMessage.readObject();
//		    readMessage.readObject();
		} catch (Exception e) {
		    System.err.println("e: " + e);
		}

                readMessage.finish();

                WriteMessage writeMessage = sport.newMessage();
                writeMessage.finish();
            }
        }
    }
}


    static Ibis ibis;

    static Registry registry;

    public static void main(String[] args) {
        int count = 10000;
        int repeat = 10;
        int rank = 0, remoteRank = 1;

        try {
            CapabilitySet s = new CapabilitySet(
                    WORLDMODEL_CLOSED, SERIALIZATION_OBJECT,
                    CONNECTION_ONE_TO_ONE,
                    COMMUNICATION_RELIABLE, RECEIVE_EXPLICIT);
            ibis = IbisFactory.createIbis(s, null, null, null);

            registry = ibis.registry();

            CapabilitySet t = s;

            SendPort sport = ibis.createSendPort(t, "send port");
            ReceivePort rport;
//            Latency lat = null;

            IbisIdentifier master = registry.elect("latency");
            IbisIdentifier remote;

            if (master.equals(ibis.identifier())) {
                rank = 0;
                remote = registry.getElectionResult("client");
            } else {
                registry.elect("client");
                rank = 1;
                remote = master;
            }

	    Sender sender = null;
	    ExplicitReceiver receiver = null;
            if (rank == 0) {
                rport = ibis.createReceivePort(t, "test port");
                rport.enableConnections();
                sport.connect(remote, "test port");
                sender = new Sender(rport, sport);
                sender.send(count, repeat);
            } else {
                sport.connect(remote, "test port");
                rport = ibis.createReceivePort(t, "test port");
                rport.enableConnections();
                receiver = new ExplicitReceiver(rport, sport);
                receiver.receive(count, repeat);
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
