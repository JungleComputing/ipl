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

import java.io.IOException;

import ibis.ipl.Ibis;
import ibis.ipl.PortType;
import ibis.ipl.SendPort;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.ReadMessage;
import ibis.ipl.WriteMessage;
import ibis.ipl.Registry;
import ibis.ipl.StaticProperties;
import ibis.ipl.IbisException;
import ibis.ipl.NoMatchingIbisException;
import ibis.ipl.Upcall;

import ibis.util.PoolInfo;
import ibis.util.Timer;
import ibis.util.TypedProperties;

public class TreeReducer extends Reducer {

    private final static boolean TIMINGS = TypedProperties.booleanProperty("timing.reduce", false);

    protected ReceivePort[]	reduceRreduce;
    protected SendPort		reduceSreduce;

    protected ReceivePort	reduceRbcast;
    protected SendPort		reduceSbcast;

    protected static final int	LEAF_NODE = -1;

    protected int		parent;
    protected int[]		child = new int[2];

    protected TreeReducer() {
	// Java needs this. Someday, I will certify as a Java programmer, and
	// then I will know *why*.
    }


    public TreeReducer(Ibis ibis, PoolInfo info)
	    throws IOException, IbisException {

	int rank = info.rank();
	int size = info.size();

	StaticProperties reqprops = new StaticProperties();
	reqprops.add("serialization", "data");
	reqprops.add("communication", "OneToOne, Reliable, ExplicitReceipt");

	PortType portTypeReduce = ibis.createPortType("SOR Reduce", reqprops);


	reqprops = new StaticProperties();
	reqprops.add("serialization", "data");
	reqprops.add("communication",
		"OneToMany, OneToOne, Reliable, ExplicitReceipt");

	PortType portTypeBroadcast = ibis.createPortType("SOR Broadcast", reqprops);
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
		    reduceRreduce[c] = portTypeReduce.createReceivePort("SOR" + rank + "_" + c + "_reduceR");
		    reduceRreduce[c].enableConnections();
		}
	    }
	}

	if (parent != LEAF_NODE) {
	    int childrank = rank - 2 * parent - 1;
	    reduceSreduce = portTypeReduce.createSendPort("SOR" + rank + "reduceS");
	    ReceivePortIdentifier id;
	    id = registry.lookupReceivePort("SOR" + parent + "_" + childrank + "_reduceR");
	    reduceSreduce.connect(id);
	}

	/* Create and connect ports for the bcast phase */
	if (parent != LEAF_NODE) {
	    reduceRbcast = portTypeBroadcast.createReceivePort("SOR" + rank + "reduceR");
	    reduceRbcast.enableConnections();
	}

	if (children > 0) {
	    reduceSbcast = portTypeBroadcast.createSendPort("SOR" + rank + "reduceS");
	    for (int c = 0; c < 2; c++) {
		if (child[c] != LEAF_NODE) {
		    ReceivePortIdentifier id;
		    id = registry.lookupReceivePort("SOR" + child[c] + "reduceR");
		    reduceSbcast.connect(id);
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
