import ibis.gmi.*;

class Main {

    public static final int
	MAX_HOPS	= 4,	 // Search depth of master.
	MAX_JOBS	= 10000;

    static JobQueue jobQueue;
    static DistanceTable distanceTable;
    static int nrCities;
    static Minimum minimum;

    static void distributor(int hops, int length, int[] path) {
	int me, dist;

	if(hops == MAX_HOPS - 1) {
	    jobQueue.addJob(new Job(length, path));
	    return;
	}

	me = path[hops];

	for(int i=0; i<nrCities; i++) {
	    if(!present(i, hops, path)) {
		path[hops+1] = i;
		dist = distanceTable.distance(me, i);
		distributor(hops+1, length+dist, path);
	    }
	}
    }

    static void generateJobs() {
	int[] path = new int[nrCities];

	path[0] = 0;
	distributor(0, 0, path);
    }

    // Test if a given city is present on the given path.
    static boolean present(int city, int hops, int[] path) {
	for(int i=0; i<=hops; i++) {
	    if(path[i] == city) return true;
	}

	return false;
    }

    private static void usage() {
	System.err.println("Illegal parameter(s).");
	System.err.println("Allowed parameters are:");
	System.err.println("   -bound <num>       : to set the upper bound;");
	System.err.println("   <filename>         : the distance table.");
    }

    public static void main(String[] argv) {

	int nodes      = Group.size();
	int rank       = Group.rank();
	int bound      = 0;
	String filename = null;

	try {
	    for (int i = 0; i < argv.length; i++) {
		if (argv[i].equals("-bound")) {
		    bound = Integer.parseInt(argv[++i]);
		} else if (filename == null) {
		    filename = argv[i];
		} else {
		    if (rank == 0) {
			usage();
		    }
		    System.exit(1);
		}
	    }
	    if (filename == null) {
		if (rank == 0) {
		    usage();
		}
		System.exit(1);
	    }
	    if (rank == 0) {

		System.out.println("Starting TSP");

		Group.create("Minimum", i_Minimum.class, nodes);
		Group.create("Queue", i_JobQueue.class, 1);
		distanceTable = DistanceTable.readTable(filename);
		nrCities = distanceTable.getSize();	
		jobQueue = new JobQueue(distanceTable);
		generateJobs();
		System.out.println(jobQueue.jobsLeft() +" Jobs generated");
		Group.join("Queue", jobQueue);
	    } 

	    if (bound == 0) bound = Integer.MAX_VALUE;	
	    minimum = new Minimum(bound);
	    Group.join("Minimum", minimum);

	    i_Minimum min = (i_Minimum) Group.lookup("Minimum");
	    GroupMethod method = Group.findMethod(min, "void set(int)");
	    method.configure(new GroupInvocation(), new ReturnReply(0));

	    i_JobQueue q = (i_JobQueue) Group.lookup("Queue");
	    method  = Group.findMethod(q, "DistanceTable getTable()");
	    method.configure(new SingleInvocation(0), new ReturnReply(0));
	    method  = Group.findMethod(q, "Job getJob()");
	    method.configure(new SingleInvocation(0), new ReturnReply(0));
	    method = Group.findMethod(q, "void barrier()");
	    method.configure(new CombinedInvocation("Queue", rank, nodes, new MyCombiner(), new SingleInvocation(0)), new ReturnReply(0));

	    Client c = new Client(q, minimum, min, rank);
	    c.run();

	    q.barrier();

	    if (minimum.getRank() == 0) {
		System.out.println("minimum = " + minimum.get());
	    }

	    Group.exit();

	} catch (Exception e) {
	    System.out.println("Oops " + rank + e);
	    e.printStackTrace();
	}

	System.exit(0);
    }
}
