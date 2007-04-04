package ibis.ipl.apps.sor;

/* $Id$ */

/**
 * Reducer.java
 *
 * Performs a reduce2all(max, double).
 *
 * Simplest implementation is an O(n) algorithm where node 0  is heavily
 * taxed.
 * @author Rutger Hofman
 */

import ibis.ipl.CapabilitySet;
import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.Registry;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;
import ibis.util.PoolInfo;
import ibis.util.Timer;
import ibis.util.TypedProperties;

import java.io.IOException;

public class Reducer implements ibis.ipl.PredefinedCapabilities {

    static TypedProperties tp = new TypedProperties(System.getProperties());

    private final static boolean TIMINGS = tp.booleanProperty(
            "timing.reduce", false);

    private Timer t_reduce_send = Timer.createTimer();

    private Timer t_reduce_receive = Timer.createTimer();

    private int rank;

    private int size;

    private SendPort reduceS;

    private ReceivePort reduceR;

    protected Reducer() {
        // Dunno, would not see any use here
    }

    public Reducer(Ibis ibis, PoolInfo info) throws IOException {

        rank = info.rank();
        size = info.size();

        Registry registry = ibis.registry();
        CapabilitySet portTypeReduce = new CapabilitySet(SERIALIZATION_DATA,
            CONNECTION_MANY_TO_ONE, COMMUNICATION_RELIABLE, RECEIVE_EXPLICIT);


        CapabilitySet portTypeBroadcast = new CapabilitySet(SERIALIZATION_DATA,
            CONNECTION_ONE_TO_MANY, COMMUNICATION_RELIABLE, RECEIVE_EXPLICIT);

        if (rank == 0) {
            // one-to-many to bcast result
            reduceR = ibis.createReceivePort(portTypeReduce, "SORreduceR");
            reduceR.enableConnections();
            reduceS = ibis.createSendPort(portTypeBroadcast, "SORreduceS");
            for (int i = 1; i < size; i++) {
                IbisIdentifier id = registry.getElectionResult("" + i);
                reduceS.connect(id, "SORreduceR");
            }
        } else {
            reduceR = ibis.createReceivePort(portTypeBroadcast, "SORreduceR");
            reduceR.enableConnections();
            reduceS = ibis.createSendPort(portTypeReduce, "SORreduceS");

            // many-to-one to gather values
            IbisIdentifier id = registry.getElectionResult("" + 0);
            reduceS.connect(id, "SORreduceR");
        }
    }

    public double reduce(double value) throws IOException {

        //sanity check
        //if (Double.isNaN(value)) {
        //    System.err.println(rank + ": Eek! NaN used in reduce");
        //    new Exception().printStackTrace(System.err);
        //    System.exit(1);
        //}

        // System.err.println(rank + ": BEGIN REDUCE");
        //
        //

        if (rank == 0) {
            if (TIMINGS)
                t_reduce_receive.start();
            for (int i = 1; i < size; i++) {
                ReadMessage rm = reduceR.receive();
                double temp = rm.readDouble();

                //if (Double.isNaN(value)) {
                //    System.err.println(rank + ": Eek! NaN used in reduce");
                //    new Exception().printStackTrace(System.err);
                //    System.exit(1);
                //}

                value = Math.max(value, temp);
                rm.finish();
            }
            if (TIMINGS)
                t_reduce_receive.stop();

            if (TIMINGS)
                t_reduce_send.start();
            WriteMessage wm = reduceS.newMessage();
            wm.writeDouble(value);
            wm.finish();
            if (TIMINGS)
                t_reduce_send.stop();
        } else {
            if (TIMINGS)
                t_reduce_send.start();
            WriteMessage wm = reduceS.newMessage();
            wm.writeDouble(value);
            wm.finish();
            if (TIMINGS)
                t_reduce_send.stop();

            if (TIMINGS)
                t_reduce_receive.start();
            ReadMessage rm = reduceR.receive();
            value = rm.readDouble();
            rm.finish();
            if (TIMINGS)
                t_reduce_receive.stop();
        }

        return value;
    }

    public void end() throws IOException {

        if (reduceS != null) {
            reduceS.close();
            reduceS = null;
        }
        if (reduceR != null) {
            reduceR.close();
            reduceR = null;
        }

        if (TIMINGS) {
            System.err.println(rank + ": t_reduce_send "
                    + t_reduce_send.nrTimes() + " av.time "
                    + t_reduce_send.averageTime());
            System.err.println(rank + ": t_reduce_rcve "
                    + t_reduce_receive.nrTimes() + " av.time "
                    + t_reduce_receive.averageTime());
        }
    }

}
