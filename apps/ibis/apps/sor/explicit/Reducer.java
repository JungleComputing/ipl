/**
 * Reducer.java
 *
 * Performs a reduce2all(max, double).
 *
 * Simplest implementation is an O(n) algorithm where node 0  is heavily
 * taxed.
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
import ibis.util.Timer;
import ibis.util.TypedProperties;

public class Reducer {

    private final static boolean TIMINGS = TypedProperties.booleanProperty("timing.reduce", false);

    private Timer t_reduce_send    = Timer.createTimer();
    private Timer t_reduce_receive = Timer.createTimer();

    private int		rank;
    private int		size;

    private SendPort reduceS;
    private ReceivePort reduceR;

    protected Reducer() {
	// Dunno, would not see any use here
    }

    public Reducer(Ibis ibis, PoolInfo info)
	    throws IOException, IbisException {

	rank = info.rank();
	size = info.size();

	StaticProperties reqprops = new StaticProperties();
	reqprops.add("serialization", "data");
	// reqprops.add("communication", "OneToOne, Reliable, ExplicitReceipt");
	reqprops.add("communication",
		"OneToOne, ManyToOne, Reliable, ExplicitReceipt");

	PortType portTypeReduce = ibis.createPortType("SOR Reduce", reqprops);


	reqprops = new StaticProperties();
	reqprops.add("serialization", "data");
	reqprops.add("communication",
		"OneToMany, OneToOne, Reliable, ExplicitReceipt");

	PortType portTypeBroadcast = ibis.createPortType("SOR Broadcast", reqprops);

	Registry registry = ibis.registry();

	if (rank == 0) {
	    // one-to-many to bcast result
	    reduceR = portTypeReduce.createReceivePort("SOR" + rank + "reduceR");
	    reduceR.enableConnections();
	    reduceS = portTypeBroadcast.createSendPort("SOR" + rank + "reduceS");
	    for (int i = 1 ; i < size; i++) {
		ReceivePortIdentifier id = registry.lookup("SOR" + i + "reduceR");
		reduceS.connect(id);
	    }

	} else {
	    reduceR = portTypeBroadcast.createReceivePort("SOR" + rank + "reduceR");
	    reduceR.enableConnections();
	    reduceS = portTypeReduce.createSendPort("SOR" + rank + "reduceS");

	    // many-to-one to gather values
	    ReceivePortIdentifier id = registry.lookup("SOR0reduceR");
	    reduceS.connect(id);
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
	    if (TIMINGS) t_reduce_receive.start();
	    for (int i=1;i<size;i++) {
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
	    if (TIMINGS) t_reduce_receive.stop();

	    if (TIMINGS) t_reduce_send.start();
	    WriteMessage wm = reduceS.newMessage();
	    wm.writeDouble(value);
	    wm.finish();
	    if (TIMINGS) t_reduce_send.stop();
	} else {
	    if (TIMINGS) t_reduce_send.start();
	    WriteMessage wm = reduceS.newMessage();
	    wm.writeDouble(value);
	    wm.finish();
	    if (TIMINGS) t_reduce_send.stop();

	    if (TIMINGS) t_reduce_receive.start();
	    ReadMessage rm = reduceR.receive();
	    value = rm.readDouble();
	    rm.finish();
	    if (TIMINGS) t_reduce_receive.stop();
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
	    System.err.println(rank
		    + ": t_reduce_send " + t_reduce_send.nrTimes()
		    + " av.time " + t_reduce_send.averageTime());
	    System.err.println(rank
		    + ": t_reduce_rcve " + t_reduce_receive.nrTimes()
		    + " av.time " + t_reduce_receive.averageTime());
	}
    }

}
