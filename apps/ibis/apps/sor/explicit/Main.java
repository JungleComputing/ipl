/*
 * SOR.java
 * Successive over relaxation
 * Ibis version implementing a red-black SOR, based on earlier Orca source.
 *
 * Rob van Nieuwpoort & Jason Maassen
 *
 */
import java.util.Properties;

import ibis.ipl.*;
import ibis.util.*;

import java.io.IOException;

class Main {

	PoolInfo info;
	Ibis ibis;
	Registry registry;

	ReceivePort leftR;
	ReceivePort rightR;
	SendPort leftS;
	SendPort rightS;

	ReceivePort reduceR;
	SendPort reduceS;

	boolean finished = false;

	private void usage(String[] args) {

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

	private ReceivePortIdentifier findReceivePort(String name) throws Exception {

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

	private void connect(SendPort send, ReceivePortIdentifier id) {

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

	private synchronized void cleanup() {
	    if (finished) {
		return;
	    }

	    finished = true;

	    try {
		if (leftS != null) {
			leftS.close();
		}

		if (rightS != null) {
			rightS.close();
		}

		if (reduceS != null) {
		    reduceS.close();
		}
		if (reduceR != null) {
		    reduceR.close();
		}

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


	public static void main (String[] args) {
	    new Main(args);
	}

	Main(String[] args) {
	    long t_start = System.currentTimeMillis();
		try {
			/* set up problem size */

			int N = 1026;
			// int N = 200;
			int nrow = 0;
			int ncol = 0;
			int maxIters = -1;
			boolean warmup = true;

			info = PoolInfo.createPoolInfo();

			int options = 0;
			for (int i = 0; i < args.length; i++) {
			    if (false) {
			    } else if (args[i].equals("-no-warmup")) {
				warmup = false;
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

			Runtime.getRuntime().addShutdownHook(new Thread("SOR/Explicit ShutdownHook") {
			    public void run() {
				cleanup();
			    }
			});

			registry = ibis.registry();

			reqprops = new StaticProperties();
			reqprops.add("serialization", "data");
			reqprops.add("communication", "OneToOne, Reliable, ExplicitReceipt");

			PortType portTypeNeighbour = ibis.createPortType("SOR Neigbour", reqprops);

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
// System.err.println(info.rank() + ": hi, I'm connected...");

			if (info.rank() == 0) {
				System.out.println("Starting SOR");
				System.out.println("");
				System.out.println("CPUs          : " + info.size());
				System.out.println("Matrix size   : " + nrow + "x" + ncol);
				System.out.println("");
			}

			SOR local = new SOR(nrow, ncol, N, info.rank(), info.size(), maxIters, leftS, rightS, leftR, rightR, reduceS, reduceR);
			if (warmup) {
			    local.start("warmup");
			}
			local.start("SOR");

			cleanup();

		} catch (Exception e) {
			System.out.println("Oops " + e);
			e.printStackTrace();
		}
	}

}
