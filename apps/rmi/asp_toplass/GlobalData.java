import ibis.util.PoolInfo;

import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.*;

class GlobalData extends UnicastRemoteObject implements i_GlobalData {

private int total_num;

// Used for the initial exchange of stubs.
private i_Asp [] nodes;
private int num_nodes;

// Used for the diff reduce.
private int start = 0;
private int done  = 0;


GlobalData(PoolInfo info) throws RemoteException {
	total_num = info.size();
	nodes     = new i_Asp[total_num];
	num_nodes = 0;
}

public synchronized i_Asp [] table(i_Asp me, int node) throws RemoteException {

	// Note: This function can only be used once !
	num_nodes++;

	nodes[node] = me;

	while (num_nodes < total_num) {
		try {
			wait();
		} catch (Exception e) {
			throw new RemoteException(e.toString());
		}
	} 

	notifyAll();
	
	return nodes;
}

public synchronized void done() throws RemoteException {

	// simple barrier for termination detection. NOTE: can only be used once !!!
	done++;

	while (done < num_nodes) {
		try {
			wait();
		} catch (Exception e) {
			throw new RemoteException(e.toString());
		}
	}

	notifyAll();
}
 
public synchronized void start() throws RemoteException {

	// simple barrier for start detection. NOTE: can only be used once !!!
	start++;

	while (start < num_nodes) {
		try {
			wait();
		} catch (Exception e) {
			throw new RemoteException(e.toString());
		}
	}

	notifyAll();
}
 

}
