import ibis.gmi.*;

class Data extends GroupMember implements i_Data {

	int slaves;
	int puts;

	int [] removed;
	long [] checks;
        long [] time;
	int [] change_ops;
	int [] modif;
	int [] revise;

	int started;
     
	Data(int slaves) {

		this.slaves = slaves;		

		removed    = new int[slaves];
		checks     = new long[slaves];
		time       = new long[slaves];
		change_ops = new int[slaves];
		modif      = new int[slaves];
		revise     = new int[slaves];

		started = 0;
	}

	public synchronized void put(int cpu, int removed, long checks, long time, int modif, int change_ops, int revise) {

		this.removed[cpu]    = removed;
		this.checks[cpu]     = checks;
		this.modif[cpu]      = modif;
		this.change_ops[cpu] = change_ops;
		this.time[cpu]       = time;
		this.revise[cpu]     = revise;

		puts++;
		
		notifyAll();
	}

	private synchronized void wait_for_all() {

		while (puts < slaves) {
			try {
				wait();
			} catch (InterruptedException e) {
				// don't care ?
			}
		}		
	}

	public String result() {

		int t_removed    = 0;
		long t_checks     = 0;
		int t_change_ops = 0;
		int t_modif      = 0;
		int t_revise     = 0;
		long t_time      = 0;

		wait_for_all();

		StringBuffer temp = new StringBuffer("Result\n");

		temp.append("removed\tchecks\t\tchangeOPS\tmodif\trevise\ttime\n");  
		
		for (int i=0;i<slaves;i++) {
			temp.append(removed[i]);
			temp.append("\t");			
			t_removed += removed[i];

			temp.append(checks[i]);
			temp.append("\t\t");
			t_checks += checks[i];

			temp.append(change_ops[i]);
			temp.append("\t\t");
			t_change_ops += change_ops[i];

			temp.append(modif[i]);
			temp.append("\t");
			t_modif += modif[i];

			temp.append(revise[i]);
			temp.append("\t");
			t_revise += revise[i];

			temp.append(time[i]);

			if (time[i] > t_time) {
				t_time = time[i];
			}

			temp.append("\n");
		}
			
		temp.append("-----------------------------------------------\n"); 

		temp.append(t_removed);
		temp.append("\t");			

		temp.append(t_checks);
		temp.append("\t\t");
		
		temp.append(t_change_ops);
		temp.append("\t\t");
		
		temp.append(t_modif);
		temp.append("\t");

		temp.append(t_revise);
		temp.append("\t");
		
		temp.append(t_time);
		temp.append("\n");
		
		//		return temp.toString();

		return ("time " + t_time);
	}

}
