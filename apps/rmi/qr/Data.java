import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import java.io.IOException;

import ibis.util.PoolInfo;

class Data extends UnicastRemoteObject implements i_Data {

    long [] times;

    int ncpus;
    int nresults;

    long max, min;
    long sum, sum_sq;

    double stddev, avg, avg_sq;

    Remote [] bco;

    PoolInfo info;
    int num = 0;

    Data(PoolInfo info, int ncpus) throws RemoteException {
	super();
	this.info = info;
	this.ncpus = ncpus;
	nresults = 0;

	max = 0;
	min = Integer.MAX_VALUE;

	times = new long[ncpus]; 
	bco = new i_BroadcastObject[ncpus];
    }

    public synchronized Remote [] signup(int cpu, String name) throws RemoteException {

	try {
	    bco[cpu] = RMI_init.lookup("//" + info.hostName(cpu) + "/" + name);

	} catch (java.io.IOException eM) { 
	    throw new RemoteException("Naming.lookup fails", eM);
	}
	num++;

	while (num < ncpus) { 
	    try { 
		wait();
	    } catch (Exception e) { 
		// ignore
	    } 
	} 
	notifyAll();		
	return bco;
    } 

    public synchronized void put(int cpu, long time) throws RemoteException {

	times[cpu] = time;

	if (time > max) max = time;
	if (time < min) min = time;

	sum    += time;
	sum_sq += time*time;

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


    long max_time() {		
	wait_for_data();
	return max;
    }		

    long min_time() {		
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


