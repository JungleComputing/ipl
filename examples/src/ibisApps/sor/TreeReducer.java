package ibisApps.sor;

/* $Id$ */

/**
 * Reducer.java
 *
 * Performs a reduce2all(max, double).
 *
 * Less trivial implementation is an O(log n) algorithm where node 0 is the
 * tree root.
 *
 * @author Rutger Hofman
 */

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.Registry;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;
import ibis.util.TypedProperties;

import java.io.IOException;

public class TreeReducer extends Reducer {

    static TypedProperties tp = new TypedProperties(System.getProperties());

    protected ReceivePort[] reduceRreduce;

    protected SendPort reduceSreduce;

    protected ReceivePort reduceRbcast;

    protected SendPort reduceSbcast;

    protected static final int LEAF_NODE = -1;

    protected int parent;

    protected int[] child = new int[2];

    protected TreeReducer() {
        // Java needs this. Someday, I will certify as a Java programmer, and
        // then I will know *why*.
    }

    public TreeReducer(Ibis ibis, int rank, int size) throws IOException {

        PortType portTypeReduce = new PortType(PortType.SERIALIZATION_DATA,
                PortType.CONNECTION_MANY_TO_ONE, PortType.COMMUNICATION_RELIABLE,
                PortType.RECEIVE_EXPLICIT);

        PortType portTypeBroadcast = new PortType(PortType.SERIALIZATION_DATA,
                PortType.CONNECTION_ONE_TO_MANY, PortType.COMMUNICATION_RELIABLE,
                PortType.RECEIVE_EXPLICIT);
        
        if (rank == 0) {
            parent = LEAF_NODE;
        } else {
            parent = (rank - 1) / 2;
        }

        int children = 0;
        for (int c = 0; c < 2; c++) {
            child[c] = 2 * rank + c + 1;
            if (child[c] >= size) {
                child[c] = LEAF_NODE;
            } else {
                children++;
            }
        }

        Registry registry = ibis.registry();

        /* Create and connect ports for the reduce phase */
        if (children > 0) {
            reduceRreduce = new ReceivePort[2];
            for (int c = 0; c < 2; c++) {
                if (child[c] != LEAF_NODE) {
                    reduceRreduce[c] = ibis.createReceivePort(portTypeReduce,
                            "SOR" + c + "_reduceR");
                    reduceRreduce[c].enableConnections();
                }
            }
        }

        if (parent != LEAF_NODE) {
            int childrank = rank - 2 * parent - 1;
            reduceSreduce = ibis.createSendPort(portTypeReduce, "SORreduceS");
            IbisIdentifier id = registry.getElectionResult("" + parent);
            reduceSreduce.connect(id, "SOR" + childrank + "_reduceR");
        }

        /* Create and connect ports for the bcast phase */
        if (parent != LEAF_NODE) {
            reduceRbcast = ibis.createReceivePort(portTypeBroadcast,
                    "SORreduceR");
            reduceRbcast.enableConnections();
        }

        if (children > 0) {
            reduceSbcast = ibis.createSendPort(portTypeBroadcast, "SORreduceS");
            for (int c = 0; c < 2; c++) {
                if (child[c] != LEAF_NODE) {
                    IbisIdentifier id = registry.getElectionResult("" + child[c]);
                    reduceSbcast.connect(id, "SORreduceR");
                }
            }
        }

    }

    public double reduce(double value) throws IOException {

        for (int c = 0; c < 2; c++) {
            if (child[c] != LEAF_NODE) {
                ReadMessage rm = reduceRreduce[c].receive();
                value = Math.max(value, rm.readDouble());
                rm.finish();
            }
        }

        if (parent != LEAF_NODE) {
            WriteMessage wm = reduceSreduce.newMessage();
            wm.writeDouble(value);
            wm.finish();

            ReadMessage rm = reduceRbcast.receive();
            value = rm.readDouble();
            rm.finish();
        }

        if (reduceSbcast != null) {
            WriteMessage wm = reduceSbcast.newMessage();
            wm.writeDouble(value);
            wm.finish();
        }

        return value;
    }

    public void end() throws IOException {

        if (reduceSreduce != null) {
            reduceSreduce.close();
            reduceSreduce = null;
        }
        if (reduceRreduce != null) {
            for (int c = 0; c < reduceRreduce.length; c++) {
                if (reduceRreduce[c] != null) {
                    reduceRreduce[c].close();
                    reduceRreduce[c] = null;
                }
            }
        }

        if (reduceSbcast != null) {
            reduceSbcast.close();
            reduceSbcast = null;
        }
        if (reduceRbcast != null) {
            reduceRbcast.close();
            reduceRbcast = null;
        }
    }

}
