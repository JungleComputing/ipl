import java.rmi.*;
import java.rmi.registry.*;
import java.net.*;
import java.util.Date;
import ibis.util.PoolInfo;

class Server {

	static Registry local = null;

	DistanceTable distanceTable;
	JobQueueImpl jobQueue;
	MinimumImpl minimum;
	int nrCities;
	PoolInfo info;

	Server(PoolInfo info, String[] argv) {
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

		distanceTable = DistanceTable.readTable(filename);
		nrCities = distanceTable.getSize();

		try {
			jobQueue = new JobQueueImpl(Misc.MAX_JOBS);
			Naming.rebind("JobQueue", jobQueue);

			minimum = new MinimumImpl(bound);
			Naming.rebind("Minimum", minimum);
		} catch (Exception e) {
			System.out.println("ERROR: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}


	void distributor(int hops, int length, int[] path) {
		int me, dist;

		if(hops == Misc.MAX_HOPS - 1) {
			jobQueue.addJob(new Job(length, path));
			return;
		}

		me = path[hops];
		for(int i=1; i<nrCities; i++) {
			if(!Misc.present(i, hops, path)) {
				path[hops+1] = i;
				dist = distanceTable.distance(me, i);
				distributor(hops+1, length+dist, path);
			}
		}
	}


	void generateJobs() {
		int[] path = new int[nrCities];

		path[0] = 0;
		distributor(0, 0, path);
	}


	public void start() {
		long begin, end;

		begin = System.currentTimeMillis();
		generateJobs();

		try {
			System.out.println("" + jobQueue.jobsLeft() +
					   " Jobs generated");

			jobQueue.allDone(); // wait until everybody is done.
			end = System.currentTimeMillis();

			System.out.println("Minimum route = " + minimum.get());
			System.out.println("Calculation Time = " +
				(end - begin) + "ms");
			info.printTime("TSP-RMI", end-begin);

		} catch (Exception e) {
			System.out.println("ERROR: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}

		try {
			Naming.unbind("JobQueue");
			Naming.unbind("Minimum");
			System.exit(0);
		} catch (Exception e) {
			System.out.println("ERROR: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void main(String argv[]) {

	    try {
		PoolInfo info = new PoolInfo();
		int cpu = info.rank();

		System.out.println("I am " + info.hostName(cpu));
		System.out.println("Getting registry on " + info.hostName(0));

		local = RMI_init.getRegistry(info.hostName(0));

		System.out.println("Got registry on " + info.hostName(0));

		Client c = new Client(info, argv);

		c.start();

		if (cpu == 0) {
			new Server(info, argv).start();
		}

		c.join();
		System.exit(0);
	    } catch(Exception e) {
		System.out.println("Oops " + e);
		e.printStackTrace();
	    }
	}
}
