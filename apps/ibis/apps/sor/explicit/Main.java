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
import java.util.Properties;

import ibis.ipl.*;
import ibis.util.*;

class Main {

	static PoolInfo info;
	static Ibis ibis; 
	static Registry registry;
	
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
	
	private static ReceivePortIdentifier findReceivePort(String name) throws Exception { 

		ReceivePortIdentifier id = null;
		
		while (id == null) { 
			id = registry.lookup(name);
			
			if (id == null) { 
				try { 
					Thread.sleep(1000);
				} catch (Exception e) { 
					// ignore 
				} 
			} 
		} 

		return id;
	} 

	private static void connect(SendPort send, ReceivePortIdentifier id) { 

		int retries = 0;
		boolean retry;

		do { 
			retry = false;

			try { 			
				send.connect(id);
			} catch (Exception e) { 

				if (retries++ < 10) { 
					System.out.println(info.rank() + " Could not connect " + send + " to " + id + " will retry");
					retry = true;
					try { 
						Thread.sleep(1000);
					} catch (Exception ex) { 
						// ignore
					}
				} else { 
					System.out.println(info.rank() + " Could not connect " + send + " to " + id + " will NOT retry");
				}
			} 

		} while(retry);
	} 

	public static void main (String[] args) {
		
		try {
			/* set up problem size */
			
			int N = 1000;					
			int nrow = 0;
			int ncol = 0;
			
			info = new PoolInfo();

			if (args.length == 1) {
				N = Integer.parseInt(args[0]);
				N += 2;
			} 	    
			
			if ( N < info.size()) {
				/* give each process at least one row */
				if (info.rank() == 0) {
					System.out.println("Problem to small for number of CPU's");
				}
				System.exit(1);
			}
			
			ncol = nrow = N;
						
			// Init communication.
//			Properties p = System.getProperties();
//			String ibis_name = p.getProperty("ibis.name");
//
//			if (ibis_name == null) { 
//				ibis_name = "ibis.ipl.impl.tcp.TcpIbis";
//			} 

			String name = "SOR" + info.rank();

			StaticProperties reqprops =  new StaticProperties();

			reqprops.add("serialization", "data");
			reqprops.add("worldmodel", "closed");
			reqprops.add("communication", "OneToOne, ManyToOne, Reliable, ExplicitReceipt");

			try {
				ibis = Ibis.createIbis(reqprops, null);
			} catch(NoMatchingIbisException e) {
				System.err.println("Could not find an Ibis that can run this GMI implementation");
				System.exit(1);
			}

			registry = ibis.registry();

			reqprops = new StaticProperties();
			reqprops.add("serialization", "data");
			reqprops.add("communication", "OneToOne, Reliable, ExplicitReceipt");
			
			PortType portTypeNeighbour = ibis.createPortType("SOR Neigbour", reqprops);
						
			ReceivePort leftR = null;
			ReceivePort rightR = null;
			SendPort leftS = null;
			SendPort rightS = null;

			if (info.rank() != 0) { 
				leftR = portTypeNeighbour.createReceivePort(name + "leftR");
				leftS = portTypeNeighbour.createSendPort(name + "leftS");
				leftR.enableConnections();

//				System.out.println(info.rank() + " created " + name + "leftR and " + name + "leftS");
			} 

			if (info.rank() != info.size()-1) {
				rightR = portTypeNeighbour.createReceivePort(name + "rightR");
				rightS = portTypeNeighbour.createSendPort(name + "rightS");
				rightR.enableConnections();

//				System.out.println(info.rank() + " created " + name + "rightR and " + name + "rightS");
			} 

			if (info.rank() != 0) { 
//				System.out.println(info.rank() + " looking up " + "SOR" + (info.rank()-1) + "rightR");

				ReceivePortIdentifier id = findReceivePort("SOR" + (info.rank()-1) + "rightR");
//				System.out.println(info.rank() + " leftS = " + leftS + " id = " + id);
				connect(leftS, id);
			} 

			if (info.rank() != info.size()-1) { 
//				System.out.println(info.rank() + " looking up " + "SOR" + (info.rank()+1) + "rightR");

				ReceivePortIdentifier id = findReceivePort("SOR" + (info.rank()+1) + "leftR");
//				System.out.println(info.rank() + " rightS = " + rightS + " id = " + id);
				connect(rightS, id);
			} 

			reqprops = new StaticProperties();
			reqprops.add("serialization", "data");
			reqprops.add("communication", "OneToOne, ManyToOne, Reliable, ExplicitReceipt");
			
			PortType portTypeReduce = ibis.createPortType("SOR Reduce", reqprops);

			ReceivePort reduceR = null;
			SendPort reduceS = null;
			
			reduceR = portTypeReduce.createReceivePort(name + "reduceR");
			reduceR.enableConnections();
			reduceS = portTypeReduce.createSendPort(name + "reduceS");

			if (info.rank() == 0) { 
				// one-to-many to bcast result
				for (int i=1;i<info.size();i++) { 
					ReceivePortIdentifier id = findReceivePort("SOR" + i + "reduceR");
					connect(reduceS, id);
				} 
			} else { 
				// many-to-one to gather values
				ReceivePortIdentifier id = findReceivePort("SOR0reduceR");
				connect(reduceS, id);
			} 

			if (info.rank() == 0) {
				System.out.println("Starting SOR");
				System.out.println("");
				System.out.println("CPUs          : " + info.size());
				System.out.println("Matrix size   : " + nrow + "x" + ncol);
				System.out.println("");
			}

			SOR local = new SOR(nrow, ncol, N, info.rank(), info.size(), leftS, rightS, leftR, rightR, reduceS, reduceR);	    
			local.start();

                        // This seems to produce a lot of core dumps on PandaIbis ??
/*
			if (info.rank() != 0) { 
				leftS.free();
			} 

			if (info.rank() != info.size()-1) {
				rightS.free();
			}

			reduceS.free();
			reduceR.free();

			if (info.rank() != 0) { 
				leftR.free();
			} 

			if (info.rank() != info.size()-1) {
				rightR.free();
			}
*/
			ibis.end();
			
		} catch (Exception e) {
			System.out.println("Oops " + e);
			e.printStackTrace();
		}
	}
	
}
