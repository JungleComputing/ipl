import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;

class JobQueueImpl extends UnicastRemoteObject implements JobQueue {

	Job[] jobArray;
	int size, first, last, count;
	int jobsWorkedOn;
	boolean all_jobs_generated;

	public JobQueueImpl(int size) throws RemoteException {
		this.size = size;
		jobArray = new Job[size];
		first = last = count = jobsWorkedOn = 0;
		all_jobs_generated = false;
	}


	public final synchronized void addJob(Job j) {
		while (count >= size) {
			try {
				wait();
			}
			catch(Exception e) {
                                System.out.println("Interrupted");
                                System.exit(1);
			}
		}

		jobArray[last] = j;
		last++;
		if(last >= size) last = 0;
		count++;
		notifyAll();
	}


	public final synchronized Job getJob() throws RemoteException {
		while (count <= 0 && ! all_jobs_generated) {
			try {
				wait();
			}
			catch(Exception e) {
                                System.out.println("Interrupted");
                                System.exit(1);
			}
		}

		if (count <= 0 && all_jobs_generated) return null;

		Job firstJob = jobArray[first];
		first++;
		if(first >= size) first = 0;
		count--;
		jobsWorkedOn++;
		notifyAll();

		return firstJob;
	}


	public final synchronized int jobsLeft() {
		return count;
	}


	public final synchronized void jobDone() {
		jobsWorkedOn--;
		notifyAll();
	}


	private int started = 0;

	public final synchronized void allStarted(int total) {
	    started++;
	    if (started == total) {
		notifyAll();
	    } else {
		while (started < total) {
		    try {
			wait();
		    } catch (InterruptedException e) {
			// ignore
		    }
		}
	    }
	}


	public final synchronized void allDone() {
		all_jobs_generated = true;
		while(count != 0 || jobsWorkedOn != 0) {
			try {
				wait();
			} catch (Exception e) {
				System.out.println("Interrupted");
				System.exit(1);
			}
		}
	}
}
