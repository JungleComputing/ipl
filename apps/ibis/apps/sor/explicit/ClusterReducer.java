/**
 * Reducer.java
 *
 * Performs a reduce2all(max, double).
 *
 * Uses PoolInfo.IPCluster() to determine which cluster any host is in.
 * For each cluster, build a standard spanning tree.
 * Between clusters, do the reduce as in MagPIe.
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

import ibis.util.PoolInfo;

public class ClusterReducer extends TreeReducer {

    private ReceivePort[]	reduceRinter;
    private SendPort		reduceSinter;

    private int 		myCluster;
    private int			clusterSize;


    public ClusterReducer(Ibis ibis, PoolInfo info)
	    throws IOException, IbisException {

	int rank = info.rank();
	int size = info.size();

	int[] cluster = info.clusterIPRank();
	clusterSize = info.clusterIPSize();
	myCluster = cluster[rank];

	/*
	 * Number of hosts in myCluster
	 */
	int localSize = 0;
	for (int i = 0; i < size; i++) {
	    if (cluster[i] == myCluster) {
		localSize++;
	    }
	}

	/*
	 * Intra-cluster, we also have a ranking of hosts to set up the local
	 * spanning tree.
	 * These two arrays translate between global ranking and local ranking.
	 */
	int[] globalRank = new int[localSize];
	int[] localRank = new int[size];
	int r = 0;
	for (int i = 0; i < size; i++) {
	    if (cluster[i] == myCluster) {
		globalRank[r] = i;
		localRank[i]  = r;
		r++;
	    }
	}

	/*
	 * The global rank of the root of each cluster
	 */
	int[] clusterRoot = new int[clusterSize];
	for (int i = 0; i < clusterSize; i++) {
	    clusterRoot[i] = -1;
	}
	for (int i = 0; i < size; i++) {
	    if (clusterRoot[cluster[i]] == -1) {
		clusterRoot[cluster[i]] = i;
	    }
	}

	// Sanity check
	if (localRank[rank] == 0 && clusterRoot[myCluster] != rank) {
	    System.err.println("Oh ho oh no cluster root != cluster first");
	    System.exit(33);
	}
// if (localRank[rank] == 0)
// System.err.println(rank + ": I am root for cluster " + myCluster);
// System.err.print(rank + ": my cluster = {");
// for (int i = 0; i < clusterSize; i++) System.err.print(globalRank[i] + " ");
// System.err.println("}");

	StaticProperties reqprops = new StaticProperties();
	reqprops.add("serialization", "data");
	// reqprops.add("communication", "OneToOne, Reliable, ExplicitReceipt");
	reqprops.add("communication",
		"OneToOne, Reliable, ExplicitReceipt");

	PortType portTypeReduce = ibis.createPortType("SOR Reduce", reqprops);


	reqprops = new StaticProperties();
	reqprops.add("serialization", "data");
	reqprops.add("communication",
		"OneToMany, OneToOne, Reliable, ExplicitReceipt");

	PortType portTypeBroadcast = ibis.createPortType("SOR Broadcast", reqprops);


	if (localRank[rank] == 0) {
	    parent = LEAF_NODE;
	} else {
	    parent = (localRank[rank] - 1) / 2;
	    parent = globalRank[parent];
	}

	int children = 0;
	for (int c = 0; c < 2; c++) {
	    child[c] = 2 * localRank[rank] + c + 1;
	    if (child[c] >= localSize) {
		child[c] = LEAF_NODE;
	    } else {
		children++;
		child[c] = globalRank[child[c]];
	    }
	}

	Registry registry = ibis.registry();

	/* Create and connect ports for the intra-cluster reduce phase */
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
	    int childrank = localRank[rank] - 2 * localRank[parent] - 1;
	    reduceSreduce = portTypeReduce.createSendPort("SOR" + rank + "reduceS");
	    ReceivePortIdentifier id;
	    id = registry.lookup("SOR" + parent + "_" + childrank + "_reduceR");
	    reduceSreduce.connect(id);
	}

	/* Create and connect ports for the intra-cluster bcast phase */
	if (parent != LEAF_NODE) {
	    reduceRbcast = portTypeBroadcast.createReceivePort("SOR" + rank + "reduceR");
	    reduceRbcast.enableConnections();
	}

	if (children > 0) {
	    reduceSbcast = portTypeBroadcast.createSendPort("SOR" + rank + "reduceS");
	    for (int c = 0; c < 2; c++) {
		if (child[c] != LEAF_NODE) {
		    ReceivePortIdentifier id;
		    id = registry.lookup("SOR" + child[c] + "reduceR");
		    reduceSbcast.connect(id);
		}
	    }
	}
// System.err.println(rank + ": local connection OK");


	if (rank == clusterRoot[myCluster]) {
	    /* Create and connect ports for the inter-cluster reduce phase */

	    reqprops = new StaticProperties();
	    reqprops.add("serialization", "data");
	    reqprops.add("communication",
		    "OneToMany, OneToOne, Reliable, ExplicitReceipt");

	    PortType portTypeInter = ibis.createPortType("SOR InterCluster", reqprops);

	    reduceRinter = new ReceivePort[clusterSize];
	    for (int i = 0; i < clusterSize; i++) {
		if (i != myCluster) {
		    reduceRinter[i] = portTypeInter.createReceivePort("SOR" + rank + "_" + i + "_interR");
		    reduceRinter[i].enableConnections();
		}
	    }

	    reduceSinter = portTypeInter.createSendPort("SOR" + rank + "interS");
	    for (int i = 0; i < clusterSize; i++) {
		if (i != myCluster) {
		    ReceivePortIdentifier id;
		    id = registry.lookup("SOR" + clusterRoot[i] + "_" + myCluster + "_interR");
		    reduceSinter.connect(id);
		}
	    }
// System.err.println(rank + ": interlocal connection OK");
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

	} else {
	    /* Do the inter-cluster all2all */
	    if (clusterSize > 1) {
		WriteMessage wm = reduceSinter.newMessage();
		wm.writeDouble(value);
		wm.finish();
	    }

	    for (int i = 0; i < clusterSize; i++) {
		if (i != myCluster) {
		    ReadMessage rm = reduceRinter[i].receive();
		    value = Math.max(value, rm.readDouble());
		    rm.finish();
		}
	    }
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

	if (reduceSinter != null) {
	    reduceSinter.close();
	    reduceSinter = null;
	}
	if (reduceRinter != null) {
	    for (int i = 0; i < reduceRinter.length; i++) {
		if (reduceRinter[i] != null) {
		    reduceRinter[i].close();
		    reduceRinter[i] = null;
		}
	    }
	    reduceRinter = null;
	}
    }

}
