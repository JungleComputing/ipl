import java.rmi.Naming;
import java.rmi.RemoteException;
import ibis.util.PoolInfo;

class Client extends Thread {
	DistanceTable distanceTable;
	JobQueue jobQueue;
	Minimum minimum;
	int nrCities;
	int min;
	MinimumReceiverImpl minReceiver;
	PoolInfo info;

	// static final boolean NODE_STATISTICS = true;
	static final boolean NODE_STATISTICS = false;

	int jobs_done = 0;
	int nodes_done = 0;

	Client(PoolInfo info, String[] argv) {
		String filename = null;
		int bound = Integer.MAX_VALUE;
		int cpu = info.rank();
		
		this.info = info;

		int options = 0;
		for (int i = 0; i < argv.length; i++) {
		    if (argv[i].equals("-bound")) {
			bound = Integer.parseInt(argv[++i]);
		    } else if (options == 0) {
			filename = argv[i];
			options++;
		    } else {
			if (cpu == 0) System.out.println(
				"Usage: java Server <city filename>");
			System.exit(1);
		    }
		}

		if (filename == null) {
		    if (cpu == 0) System.out.println(
			    "Usage: java Server <city filename>");
		    System.exit(1);
		}

		min = bound;

		distanceTable = DistanceTable.readTable(filename);
		nrCities = distanceTable.getSize();
		System.out.println("Distance table read");
	}

	public void run()
	{
		boolean cont = true;

		System.out.print("Waiting for server");
		do {
			try {
				Thread.sleep(100);

				this.jobQueue = (JobQueue)Naming.lookup("//" +
					info.hostName(0) + "/JobQueue");

				this.minimum = (Minimum)Naming.lookup("//" +
					info.hostName(0) + "/Minimum");

				cont = false;
			} catch (java.rmi.NotBoundException e) {
				System.out.print(".");
				System.out.flush();
			} catch (Exception e) {
				System.err.println("Exception " + e);
				e.printStackTrace(System.err);
			}
		} while (cont);

		System.out.println(" found");


		try {
			minReceiver = new MinimumReceiverImpl(this);

			// Register myself by server's minimum object.
			minimum.register(minReceiver);
		} catch (Exception e) {
			System.out.println("ERROR: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}

		try {
		    jobQueue.allStarted(info.size() + 1);
		} catch (RemoteException e) {
		    System.err.println("allStarted throws " + e);
		}

		long start = System.currentTimeMillis();

		Job job = null;

		do {
			try {
		                job = jobQueue.getJob();
			} catch (Exception e) {
				System.out.println("ERROR: " + e.getMessage());
				e.printStackTrace();
				System.exit(1);
			}

			if(job != null) {
				if (NODE_STATISTICS) {
				    jobs_done++;
				}
				calculateSubPath(Misc.MAX_HOPS-1,
						 job.length, job.path);
				try {
					jobQueue.jobDone();
				} catch (Exception e) {
					System.out.println("ERROR: " +
						e.getMessage());
					e.printStackTrace();
					System.exit(1);
				}
			}
		} while (job != null);

		if (NODE_STATISTICS) {
		    System.out.println(info.rank() + ": done " +
				       jobs_done + " jobs " +
				       nodes_done + " nodes");
		}
	}


	final void setMin(int min) {
		this.min = min;
	}


	final void calculateSubPath(int hops, int length, int[] path) {
		int me, dist;
		
		if (NODE_STATISTICS) {
		    nodes_done++;
		}

		try {
			if(length >= min) return;

			if(hops + 1 == nrCities) {
				length += distanceTable.distance(path[hops], path[0]);
				if(length < min) {
// System.out.println("Setting minimum " + length);
					minimum.set(length);
// System.out.println("Done setting minimum " + length);
					min = length;
				}
				return;
			}

		} catch (Exception e) {
			System.out.println("ERROR: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}

		// Path really is a partial route.
		// Call calculateSubPath recursively for each subtree.
		me = path[hops];

		for(int i=nrCities-1; i>0; i--) {
			if(!Misc.present(i, hops, path)) {
				path[hops+1] = i;
				dist = distanceTable.distance(me, i);
				calculateSubPath(hops+1, length+dist, path);
			}
		}
	}
}
