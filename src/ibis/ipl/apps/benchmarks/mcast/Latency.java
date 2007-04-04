package ibis.ipl.apps.benchmarks.mcast;

/* $Id$ */


import ibis.ipl.CapabilitySet;
import ibis.ipl.Ibis;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PredefinedCapabilities;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.Registry;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;
import ibis.util.PoolInfo;

class Latency implements PredefinedCapabilities {

    static Ibis ibis;

    static Registry registry;

    public static void main(String[] args) {
        /* Parse commandline. */

        try {
            PoolInfo info = PoolInfo.createPoolInfo();

            int rank = info.rank();
            int size = info.size();
            CapabilitySet sp = new CapabilitySet(WORLDMODEL_CLOSED,
                    SERIALIZATION_OBJECT, COMMUNICATION_RELIABLE,
                    RECEIVE_AUTO_UPCALLS,  RECEIVE_EXPLICIT,
                    CONNECTION_ONE_TO_ONE, CONNECTION_MANY_TO_ONE,
                    CONNECTION_ONE_TO_MANY);

            ibis = IbisFactory.createIbis(sp, null, null, null);
            registry = ibis.registry();

            CapabilitySet t = sp;

            ReceivePort rport = ibis.createReceivePort(t, "receive port");
            SendPort sport = ibis.createSendPort(t, "send port");

            rport.enableConnections();

            if (rank == 0) {
                registry.elect("0");
                sport.connect(rport.identifier());

                System.err.println(rank + "*******  connect to myself");

                for (int i = 1; i < size; i++) {

                    System.err.println(rank + "******* receive");

                    ReadMessage r = rport.receive();
                    ReceivePortIdentifier id = (ReceivePortIdentifier) r
                            .readObject();
                    r.finish();

                    System.err.println(rank + "*******  connect to " + id);

                    sport.connect(id);
                }

                System.err.println(rank + "*******  connect done ");

                WriteMessage w = sport.newMessage();
                w.writeInt(42);
                w.finish();

                sport.close();

            } else {
                IbisIdentifier id = registry.getElectionResult("0");
                System.err.println(rank + "*******  connect to 0");
                sport.connect(id, "receive port");
                System.err.println(rank + "*******  connect done");

                WriteMessage w = sport.newMessage();
                w.writeObject(rport.identifier());
                w.finish();

                sport.close();
            }

            ReadMessage r = rport.receive();
            int result = r.readInt();
            r.finish();

            System.out.println(rank + " got " + result);

            rport.close();
            ibis.end();

        } catch (Exception e) {
            System.out.println("Got exception " + e);
            System.out.println("StackTrace:");
            e.printStackTrace();
        }
    }
}
