import ibis.group.GroupMember;

class Data extends GroupMember implements i_Data {

	int [] times;
	
	int ncpus;
	int nresults;

	int max, min;
	long sum, sum_sq;

	double stddev, avg, avg_sq;

	Data(int ncpus) {
		super();
		this.ncpus = ncpus;
		nresults = 0;
		
		max = 0;
		min = Integer.MAX_VALUE;

		times = new int[ncpus]; 
	}

	public synchronized void put(int cpu, int time) {

		times[cpu] = time;

		if (time > max) max = time;
		if (time < min) min = time;

		sum    += time;
		sum_sq += (long)time*(long)time;

		nresults++;

		if (nresults == ncpus) {			
			avg    = ((double) sum) / ncpus;
			avg_sq = ((double) sum_sq) / ncpus;	
			
			double temp = avg_sq - (avg*avg);

			if (temp < 0.0) {
				System.out.println("eeek");
			} else {
				stddev = Math.sqrt(temp);
			}			      

			System.out.println("time : max = " + max + 
					   " min = " + min + 
					   " avg = " + avg + 
					   " stddev = " + stddev);
		}
		
		notifyAll();
	}

	private synchronized void wait_for_data() {
		
		while (nresults < ncpus) {
			
			try {
				wait();
			} catch (Exception e) { 
				System.err.println("Oops Data got exception " + e);
			}
		}
	}


	int max_time() {		
		wait_for_data();
		return max;
	}		

	int min_time() {		
		wait_for_data();
		return min;
	}	

	double avg_time() { 
		wait_for_data();
		return avg;
	}

	double stddev_time() {
		wait_for_data();
		return stddev;
	}

}


