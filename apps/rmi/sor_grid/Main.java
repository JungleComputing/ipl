/*
 * SOR.java
 * Successive over relaxation
 * SUN RMI version implementing a red-black SOR, based on earlier Orca source.
 * with cluster optimization, and split phase optimization, reusing a thread
 * each Wide-Area send. (All switchable)
 *
 * Rob van Nieuwpoort & Jason Maassen
 *
 */

import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.Registry;

import ibis.util.PoolInfo;

class Main {

    private static void usage(String[] args) {

	System.out.println("Usage: sor [options] [ <NROW> [ <NCOL> [ <ITERATIONS> ] ] ] [ <COMMUNICATION> [ <THREAD> ] ] <args>*");
	System.out.println("");
	System.out.println("NROW NCOL   : (int, int). Problem matrix size");
	System.out.println("ITERATIONS    : (int). Number of iterations to calculate. 0 means dynamic termination detection.");
	System.out.println("COMMUNICATION : ( \"sync\", \"async\"). Communication type. \"async\" = always split phase calculation.");
	System.out.println("THREAD        : communication thread type. Only legal value is \"wait\", a single thread is reused");
	System.out.println("args:");
	System.out.println("-viz: support visualization, computation does not wait for external viz client.");
	System.out.println("-asyncViz: support visualization, computation is done in lockstep with external viz client.");
	System.out.println("-var-cpu: determine relative processor speed first, to divide work better");
	System.out.println("\noptions you gave: ");

	for (int i=0;i<args.length;i++) {
	    System.out.println(i + " : " + args[i]);
	}
	System.exit(1);
    }

    public static void main (String[] args) {

	try {
	    PoolInfo info = PoolInfo.createPoolInfo();

	    SOR local = null;
	    i_SOR [] table = null;
	    i_GlobalData global = null;
	    Registry reg = null;

	    /* set up problem size */
	    int nrow = 1000, ncol = 1000, nit = 50;
	    int sync = SOR.ASYNC_SEND;
	    boolean visualization = false;
	    boolean hetero_speed = false;
	    boolean asyncVisualization = false;
	    int optionCount = 0;
	    boolean warmup = false;

	    for(int i=0; i<args.length; i++) {
		if (false) {
		} else if(args[i].equals("async")) {
		    sync = SOR.ASYNC_SEND;
		} else if(args[i].equals("sync")) {
		    sync = SOR.SYNC_SEND;
		} else if(args[i].equals("wait")) {
		    // Now default and only option, but still recognized.
		} else if (args[i].equals("-viz")) {
		    visualization = true;
		} else if (args[i].equals("-var-cpu")) {
		    hetero_speed = true;
		} else if (args[i].equals("-asyncViz")) {
		    visualization = true;
		    asyncVisualization = true;
		} else if (args[i].equals("-warmup")) {
		    warmup = true;
		} else {
		    if(optionCount == 0) {
			try {
			    nrow = Integer.parseInt(args[i]);
			} catch (Exception e) {
			    usage(args);
			}
			optionCount++;
		    } else if(optionCount == 1) {
			try {
			    ncol = Integer.parseInt(args[i]);
			} catch (Exception e) {
			    usage(args);
			}
			optionCount++;
		    } else if(optionCount == 2) {
			try {
			    nit = Integer.parseInt(args[i]);
			} catch (Exception e) {
			    usage(args);
			}
			optionCount++;
		    } else {
			usage(args);
		    }
		}
	    }

	    if ( nrow < info.size()) {
		/* give each process at least one row */
		if (info.rank() == 0) {
		    System.out.println("Problem to small for number of CPU's");
		}
		System.exit(1);
	    }

	    // Start the registry.    
	    reg = RMI_init.getRegistry(info.hostName(0));

	    VisualBuffer visual = null;

	    if (info.rank() == 0) {
		System.out.println("Starting SOR");
		System.out.println("");
		System.out.println("CPUs          : " + info.size());
		System.out.println("Matrix size   : " + nrow + "x" + ncol);
		System.out.println("Iterations    : " + ((nit == 0) ? "dynamic" : ("" + nit)));
		System.out.println("Calculation   : " + ((sync == SOR.SYNC_SEND) ? "one-phase"   : "split-phase"));
		System.out.print("Visualization : ");
		if (visualization) {
		    if (asyncVisualization) {
			System.out.println("enabled, asynchronous");
		    } else {
			System.out.println("enabled, synchronous");
		    }
		} else {
		    System.out.println("disabled");
		}
		System.out.println();

		global = new GlobalData(info);
		RMI_init.bind("GlobalData", global);
		System.err.println("I am the master: " + info.hostName(0));
	    } else {
		global = (i_GlobalData) RMI_init.lookup("//" + info.hostName(0) + "/GlobalData");

	    }

	    if (visualization) {
		visual = new VisualBuffer(info, asyncVisualization);
// System.err.println("Bound " + visual + " as VisualBuffer");
		RMI_init.bind("VisualBuffer", visual);
// System.err.println("Lookup " + visual + " as " + RMI_init.lookup("//" + info.hostName(info.rank()) + "/VisualBuffer"));
	    }

	    double[] nodeSpeed = null;	/* Speed of node[i] */
	    double speed = 1.0;
	    if (hetero_speed) {
		PoolInfo seqInfo = new PoolInfo(true);
		GlobalData seqGlobal = new GlobalData(seqInfo);
		local = new SOR(1024, 1024, nit, sync, seqGlobal, null, seqInfo);
		table = seqGlobal.table((i_SOR) local, seqInfo.rank());
		local.setTable(table);
		local.start(false, "Calibrate");
		speed = 1.0 / local.getElapsedTime();
	    }
	    
	    local = new SOR(nrow, ncol, nit, sync, global, visual, info);
	    if (hetero_speed) {
		nodeSpeed = global.scatter2all(info.rank(), speed);
		System.err.println(info.rank() + ": speed " + nodeSpeed[info.rank()]);
		local.setNodeSpeed(nodeSpeed);
		global.sync();
	    }

	    table = global.table((i_SOR) local, info.rank());
	    local.setTable(table);

	    if (warmup) {
		local.start(true, "warmup");
	    }

	    local.start(true, "SOR");

System.err.println(info.rank() + ": quits...");
	    if (info.rank() == 0) {
		Thread.sleep(2000); 
		// Give the other nodes a chance to exit.
		// before cleaning up the GlobalData object.
	    }
	    System.exit(0);
	} catch (Exception e) {
	    System.err.println("OOPS: " + e);
	    e.printStackTrace();
	}
    }
}
