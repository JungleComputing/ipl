import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import ibis.util.PoolInfo;

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

	// for visualization
	private int width, height;
	private float[][] rawData;
	private boolean newDataAvailable = false;
	private boolean synchronous = false;
	private boolean doScaling = true;

	GlobalData(PoolInfo info, boolean synchronous) throws RemoteException {
		total_num = info.size();
		nodes     = new i_SOR[total_num];
		num_nodes = 0;
		this.synchronous = synchronous;
System.err.println(this + ": in ctor");
Thread.dumpStack();
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
	// used for visualization
	public void setRawDataSize(int width, int height) throws RemoteException {
		this.width = width;
		this.height = height;
		rawData = new float[height][width];
	}

	public synchronized float[][] getRawData() throws RemoteException {
		// never send the same data twice...
		while(!newDataAvailable) {
			try {
				wait();
			} catch (Exception e) {
				System.err.println("eek: " + e);
			}
		}

		newDataAvailable = false;
		notifyAll();

		return rawData;
	}

	public synchronized void putMatrix(double[][] m)  throws RemoteException{

		if(synchronous) {
			while(newDataAvailable) {
				try {
					wait();
				} catch (Exception e) {
					System.err.println("eek: " + e);
				}
			}
		} else {
			if(newDataAvailable) return;
		}

		double min = 1000;
		double max = -1000;

		for(int i=0; i<m.length; i++) {
			for (int j=0; j<m[i].length; j++) {
				if(m[i][j] < min) {
					min = m[i][j];
				}
				if(m[i][j] > max) {
					max = m[i][j];
				}
			}
		}

		// create the result matrix, downsample m.
		for(int i=0; i<height; i++) {
			for (int j=0; j<width; j++) {
				int xpos = j * m[0].length / width;
				int ypos = i * m.length / height;
				float val;

				if(!doScaling) {
					val = (float) (m[ypos][xpos] / 20.0); // floats between 0.0 and 1.0
				} else {
					if(min >= 0.0) {
						val = (float) ((m[ypos][xpos] + min) / (max-min));
					} else {
						val = (float) ((m[ypos][xpos] - min) / (max-min));
					}
				}
				rawData[i][j] = val;
			}
		}

		newDataAvailable = true;
		notifyAll();
	}
}
