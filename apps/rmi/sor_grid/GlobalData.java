//import ibis.util.PoolInfo;

import ibis.rmi.*;
import ibis.rmi.server.UnicastRemoteObject;
import ibis.rmi.registry.*;

class GlobalData extends UnicastRemoteObject implements i_GlobalData {

private int total_num;

// Used for the initial exchange of stubs.
private i_SOR [] nodes;
private int num_nodes;

// Used for the diff reduce.
private double diff    = 0.0;
private int diff_nodes = 0;
private boolean ready  = true;
private int it = 0;

GlobalData(PoolInfoClient info) throws RemoteException {
	total_num = info.size();
	nodes     = new i_SOR[total_num];
	num_nodes = 0;
}

public synchronized i_SOR [] table(i_SOR me, int node) throws RemoteException {

	// Note: This function can only be used once !
	num_nodes++;

	nodes[node] = me;

	if (num_nodes == total_num) {
		notifyAll();
	}
	else while (num_nodes < total_num) {
		try {
			wait();
		} catch (Exception e) {
			throw new RemoteException(e.toString());
		}
	} 
	
	return nodes;
}

public synchronized double reduceDiff(double value) throws RemoteException {
	// block if the last iteration is not done yet...
	while (!ready) {
		try {
			wait();
		} catch (Exception e) {
			throw new RemoteException(e.toString());
		}
	}

	diff_nodes++;

	if (diff_nodes == 1) {
		diff = value;
	} 

	if (diff < value) {
		diff = value;
	}

	if (diff_nodes == total_num) {
		ready = false;
		notifyAll();
	}
	else while (ready) {
		try {
			wait();
		} catch (Exception e) {
			throw new RemoteException(e.toString());
		}
	}

	diff_nodes--;

	//	System.out.println(diff + " <= " + value); 

	if (diff_nodes == 0) {
		// System.out.println("diff = " + diff);
		ready = true;
		notifyAll();

		//System.out.println("X");
		//		System.out.flush();
	}

	return diff;
}
 
}
