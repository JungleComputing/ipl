import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import ibis.util.PoolInfo;

/**
 * Data buffer class for visualisation tool
 *
 * @author Rob van Nieuwpoort?
 * @author Rutger Hofman
 */
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

	GlobalData(PoolInfo info) throws RemoteException {
		total_num = info.size();
		nodes     = new i_SOR[total_num];
		num_nodes = 0;
System.err.println(this + ": in ctor");
// Thread.dumpStack();
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
			} catch (InterruptedException e) {
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
			} catch (InterruptedException e) {
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
			} catch (InterruptedException e) {
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
 

private int scatter_hit = 0;
private int scatter_release = -1;
private double[] scatter_vector;

public synchronized double[] scatter2all(int rank, double value) throws RemoteException {
    if (scatter_release == -1) {
	scatter_release = total_num;
    }

    while (scatter_release != total_num) {
	try {
	    wait();
	} catch (InterruptedException e) {
	    throw new RemoteException(e.toString());
	}
    }

    if (scatter_hit == 0) {
	scatter_vector = new double[total_num];
    }

    scatter_vector[rank] = value;

    scatter_hit++;
    if (scatter_hit == total_num) {
	scatter_release = 0;
	notifyAll();
    } else {
	while (scatter_hit < total_num) {
	    try {
		wait();
	    } catch (InterruptedException e) {
		throw new RemoteException(e.toString());
	    }
	}
    }

    double[] res = scatter_vector;

    scatter_release++;
    if (scatter_release == total_num) {
	scatter_hit = 0;
	notifyAll();
    }

    return res;
}


private int sync_release = -1;
private int sync_hit = 0;

public synchronized void sync() throws RemoteException {
    if (sync_release == -1) {
	sync_release = total_num;
    }

    while (sync_release != total_num) {
	try {
	    wait();
	} catch (InterruptedException e) {
	    throw new RemoteException(e.toString());
	}
    }

    sync_hit++;
    if (sync_hit == total_num) {
	sync_release = 0;
	notifyAll();
    } else {
	while (sync_hit < total_num) {
	    try {
		wait();
	    } catch (InterruptedException e) {
		throw new RemoteException(e.toString());
	    }
	}
    }

    sync_release++;
    if (sync_release == total_num) {
	sync_hit = 0;
	notifyAll();
    }
}
}
