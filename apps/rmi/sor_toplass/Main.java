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
 
import ibis.util.PoolInfo;

import ibis.rmi.*;
import ibis.rmi.server.UnicastRemoteObject;
import ibis.rmi.registry.*;

class Main {

static PoolInfo info = SOR.info;

private static void usage(String[] args) {

	if (info.rank() == 0) {

		System.out.println("Usage: sor <NROW> <NCOL> <ITERATIONS> <COMMUNICATION> <THREAD>");
		System.out.println("");
		System.out.println("NROW x NCOL   : (int, int). Problem matrix size");
		System.out.println("ITERATIONS    : (int). Number of iterations to calculate. 0 means dynamic termination detection.");
		System.out.println("COMMUNICATION : ( \"sync\", \"async\"). Communication type. \"async\" = always split phase calculation.");
		System.out.println("THREAD        : communication thread type. Only legal value is \"wait\", a single thread is reused");
		System.out.println("");

		for (int i=0;i<args.length;i++) {
			System.out.println(i + " : " + args[i]);
		}
	}
}

public static void main (String[] args) {

    SOR local = null;
    i_SOR [] table = null;
    i_GlobalData global = null;
    Registry reg = null;
    
    /* set up problem size */
    int nrow, ncol, nit, sync;

    try {

	    sync = nit = nrow = ncol = 0;
	    
	    if (args.length == 5) {
		    nrow = Integer.parseInt(args[0]);
		    ncol = Integer.parseInt(args[1]);
		    nit  = Integer.parseInt(args[2]);
		    
		    if (args[3].equals("async")) {

			    sync = SOR.ASYNC_SEND;
			    
			    if (args[4].equals("new")) {
				    // Historical, now obsolete, never working, option.
				    System.out.println("'async new' is not supported");
				    System.exit(1);
			    } else if (args[4].equals("wait")) {
				    // Now default and only option, but still recognized.
			    } else {
				    usage(args);
				    System.exit(1);
			    }
			    
		    } else if (args[3].equals("sync")) {
			    
			    sync = SOR.SYNC_SEND;
			    
		    } else {
			    
			    usage(args);
			    System.exit(1);
			    
		    }   
		    
	    } else if (args.length == 4) {

		    nrow = Integer.parseInt(args[0]);
		    ncol = Integer.parseInt(args[1]);
		    nit  = Integer.parseInt(args[2]);
		    
		    if (args[3].equals("sync")) {
			    sync = SOR.SYNC_SEND;
		    } else if (args[3].equals("async")) {
			    sync = SOR.ASYNC_SEND;
		    } else {
			    usage(args);
			    System.exit(1);
		    }   
		    
	    } else { 	    
		    usage(args);
		    System.exit(1);
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
	   	    
	    if (info.rank() == 0) {
		    System.out.println("Starting SOR");
		    System.out.println("");
		    System.out.println("CPUs          : " + info.size());
		    System.out.println("Matrix size   : " + nrow + "x" + ncol);
		    System.out.println("Iterations    : " + ((nit == 0) ? "dynamic" : ("" + nit)));
		    System.out.println("Calculation   : " + ((sync == SOR.SYNC_SEND) ? "one-phase"   : "split-phase"));
		    System.out.println("Communication : " + ((sync == SOR.SYNC_SEND) ? "synchronous" : "asynchronous"));
		    System.out.println("Threads       : " + ((sync == SOR.SYNC_SEND) ? "not used"    : "waiting thread for every RMI"));
		    System.out.println("");
	    }
	    
	    if (info.rank() == 0) {
		    global = new GlobalData(info);
		    Naming.bind("GlobalData", global);	
	    } else {	    

		    int i = 0;
		    boolean done = false;

		    while (!done && i < 10) {
			    
			    i++;
			    done = true;

			    try { 
				    global = (i_GlobalData) Naming.lookup("//" + info.hostName(0) + "/GlobalData");
			    } catch (Exception e) {
				    done = false;
				    Thread.sleep(2000);					 				    
			    } 
		    }
		    
		    if (!done) {
			    System.out.println(info.rank() + " could not connect to " + "//" + info.hostName(0) + "/GlobalData");
			    System.exit(1);
		    }

	    }
	    
	    local = new SOR(nrow, ncol, nit, sync, global);	    

	    table = global.table((i_SOR) local, info.rank());
	    local.setTable(table);

	    local.start();

	    if (info.rank() == 0) {
		    Thread.sleep(2000); 
		    // Give the other nodes a chance to exit.
		    // before cleaning up the GlobalData object.
	    } 
		
	    local = null;
	    table = null;
	    global = null;
	    reg = null;
	    
	    System.gc();
	    
    } catch (Exception e) {
	    System.out.println("Oops " + info.rank() + e);
	    e.printStackTrace();
    }

    System.exit(0);
}

}
