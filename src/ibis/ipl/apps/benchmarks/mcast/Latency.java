package ibis.ipl.apps.benchmarks.mcast;

/* $Id$ */

import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.Registry;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

class Latency {

    static Ibis ibis;

    static Registry registry;

    public static void main(String[] args) {
        /* Parse commandline. */

        try {
            IbisCapabilities sp = new IbisCapabilities(
                    IbisCapabilities.CLOSEDWORLD,
                    IbisCapabilities.ELECTIONS);
            PortType oneToMany = new PortType(
                    PortType.SERIALIZATION_OBJECT, PortType.COMMUNICATION_RELIABLE,
                    PortType.RECEIVE_AUTO_UPCALLS,  PortType.RECEIVE_EXPLICIT,
                     PortType.CONNECTION_ONE_TO_MANY);
            PortType manyToOne = new PortType(
                    PortType.SERIALIZATION_OBJECT, PortType.COMMUNICATION_RELIABLE,
                    PortType.RECEIVE_AUTO_UPCALLS,  PortType.RECEIVE_EXPLICIT,
                    PortType.CONNECTION_MANY_TO_ONE);

            ibis = IbisFactory.createIbis(sp, null, true, null, oneToMany, manyToOne);
            registry = ibis.registry();

            IbisIdentifier master = registry.elect("0");

            int size = registry.getPoolSize();

            ReceivePort rport;
            SendPort sport;
            
            if (master.equals(ibis.identifier())) {
                rport = ibis.createReceivePort(oneToMany, "receive port");
                rport.enableConnections();
                sport = ibis.createSendPort(manyToOne, "send port");
                sport.connect(rport.identifier());

                System.err.println("*******  connect to myself");

                for (int i = 1; i < size; i++) {

                    System.err.println("******* receive");

                    ReadMessage r = rport.receive();
                    ReceivePortIdentifier id = (ReceivePortIdentifier) r
                            .readObject();
                    r.finish();

                    System.err.println("*******  connect to " + id);

                    sport.connect(id);
                }

                System.err.println("*******  connect done ");

                WriteMessage w = sport.newMessage();
                w.writeInt(42);
                w.finish();
            } else {
                rport = ibis.createReceivePort(oneToMany, "receive port");
                rport.enableConnections();
                sport = ibis.createSendPort(manyToOne, "send port");
                IbisIdentifier id = registry.getElectionResult("0");
                System.err.println("*******  connect to 0");
                sport.connect(id, "receive port");
                System.err.println("*******  connect done");

                WriteMessage w = sport.newMessage();
                w.writeObject(rport.identifier());
                w.finish();
            }
            sport.close();
            ReadMessage r = rport.receive();
            int result = r.readInt();
            r.finish();

            System.out.println("got " + result);

            rport.close();
            ibis.end();

        } catch (Exception e) {
            System.out.println("Got exception " + e);
            System.out.println("StackTrace:");
            e.printStackTrace();
        }
    }
}
