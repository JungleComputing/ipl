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

import java.io.IOException;

class Main {

	static PoolInfo info;
	static Ibis ibis; 
	static Registry registry;
	
	private static void usage(String[] args) {
		
		if (info.rank() == 0) {
			
			System.out.println("Usage: sor {<N> {<ITERATIONS>}}");
			System.out.println("");
			System.out.println("N x N   : (int, int). Problem matrix size");
			System.out.println("ITERATIONS    : (int). Number of iterations to calculate. 0 means dynamic termination detection.");
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
			} catch (IOException e) { 

				if (retries++ < 10) { 
					System.out.println("caught exception upon connecting: " + e);
					System.out.println(info.rank() + " Could not connect " + send + " to " + id + " will retry");
					retry = true;
					try { 
						Thread.sleep(1000);
					} catch (Exception ex) { 
						// ignore
					}
				} else { 
					System.out.println("caught exception upon connecting: " + e);
					System.out.println(info.rank() + " Could not connect " + send + " to " + id + " will NOT retry");
				}
			} 

		} while(retry);
	} 

	public static void main (String[] args) {
		
		try {
			/* set up problem size */
			
			int N = 1026;					
			// int N = 200;					
			int nrow = 0;
			int ncol = 0;
			int maxIters = -1;
			
			info = PoolInfo.createPoolInfo();

			int options = 0;
			for (int i = 0; i < args.length; i++) {
			    if (false) {
			    } else if (options == 0) {
				N = Integer.parseInt(args[i]);
				N += 2;
				options++;
			    } else if (options == 1) {
				maxIters = Integer.parseInt(args[i]);
				options++;
			    } else {
				usage(args);
				System.exit(33);
			    }
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
			reqprops.add("communication", "OneToMany, OneToOne, ManyToOne, Reliable, ExplicitReceipt");
			// reqprops.add("communication", "OneToOne, Reliable, ExplicitReceipt");

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
			// reqprops.add("communication", "OneToOne, Reliable, ExplicitReceipt");
			reqprops.add("communication", "OneToOne, ManyToOne, Reliable, ExplicitReceipt");
			
			PortType portTypeReduce = ibis.createPortType("SOR Reduce", reqprops);

	    
			reqprops = new StaticProperties();
			reqprops.add("serialization", "data");
			reqprops.add("communication", "OneToMany, OneToOne, Reliable, ExplicitReceipt");
			
			PortType portTypeBroadcast = ibis.createPortType("SOR Broadcast", reqprops);

			ReceivePort reduceR = null;
			SendPort reduceS = null;
			
			if (info.rank() == 0) { 
				// one-to-many to bcast result
				reduceR = portTypeReduce.createReceivePort(name + "reduceR");
				reduceR.enableConnections();
				reduceS = portTypeBroadcast.createSendPort(name + "reduceS");
				for (int i=1;i<info.size();i++) { 
					ReceivePortIdentifier id = findReceivePort("SOR" + i + "reduceR");
					connect(reduceS, id);
				} 
			} else { 

				reduceR = portTypeBroadcast.createReceivePort(name + "reduceR");
				reduceR.enableConnections();
				reduceS = portTypeReduce.createSendPort(name + "reduceS");

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

			SOR local = new SOR(nrow, ncol, N, info.rank(), info.size(), maxIters, leftS, rightS, leftR, rightR, reduceS, reduceR);	    
			local.start("warmup");
			local.start("SOR");

			if (leftS != null) {
				leftS.close();
			} 

			if (rightS != null) {
				rightS.close();
			}

			reduceS.close();
			reduceR.close();

			if (leftR != null) {
				leftR.close();
			} 

			if (rightR != null) {
				rightR.close();
			}

			ibis.end();
			
		} catch (Exception e) {
			System.out.println("Oops " + e);
			e.printStackTrace();
		}
	}
	
}
