import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.*;

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

	GlobalData(PoolInfoClient info, boolean synchronous) throws RemoteException {
		total_num = info.size();
		nodes     = new i_SOR[total_num];
		num_nodes = 0;
		this.synchronous = synchronous;
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

	synchronized void putMatrix(double[][] m) {

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
			for (int j=0; j<m[0].length; j++) {
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
