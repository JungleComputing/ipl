public class Client {

	DistanceTable distanceTable;
	i_JobQueue jobQueue;
	Minimum minimum;
	int nrCities;
	int num_jobs  = 0;
	int num_sets  = 0;
	int rank;
	i_Minimum group;

	Client(i_JobQueue jobQueue, Minimum minimum, i_Minimum group, int rank) throws Exception{

		this.jobQueue = jobQueue;
		this.minimum = minimum;
		this.rank = rank;
		this.group = group;

		jobQueue.barrier();

		this.distanceTable = jobQueue.getTable();
		this.nrCities = distanceTable.getSize();	
		System.out.println("cities " + nrCities);
	}

	public void run() throws Exception {

	       	Job job;		

		long start = 0;
		long end = 0;

		if (rank == 0) { 
			start = System.currentTimeMillis();
		}
	
		do {
			job = jobQueue.getJob();

			if(job != null) {
				num_jobs++;
				calculateSubPath(Main.MAX_HOPS-1,
						 job.length, job.path);
			}

		} while (job != null);

		if (rank == 0) { 
			end = System.currentTimeMillis();
			double time = end-start;
			time = time / 1000.0;

			System.out.println("TSP took " + time + " seconds.");
		}
	}

	void calculateSubPath(int hops, int length, int[] path) throws Exception {
		int me, dist;		

		if(length >= minimum.get()) return;

		me = path[hops];

		if(hops + 1 == nrCities) {
			length += distanceTable.distance(me, path[0]);
			if(length < minimum.get()) {
				group.set(length);
			}
			return;
		}

		// Path really is a partial route.
		// Call calculateSubPath recursively for each subtree.

		for(int i=0; i<nrCities; i++) {
			if(!present(i, hops, path)) {
				path[hops+1] = i;
				dist = distanceTable.distance(me, i);
				calculateSubPath(hops+1, length+dist, path);
			}
		}
	}

	boolean present(int city, int hops, int[] path) {
		for(int i=0; i<=hops; i++) {
			if(path[i] == city) return true;
		}

		return false;
	}
}

