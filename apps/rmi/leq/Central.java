import ibis.rmi.*;
import ibis.rmi.server.*;

class Central extends UnicastRemoteObject implements i_Central {

	final double BOUND = 0.001;

	int size;
	int cpus;

	double [] data;
	double sum_residue;

	int count;
	BroadcastObject bco;
	Barrier bar;

	int phase;

	public Central(BroadcastObject bco, int size, int cpus) throws RemoteException { 
		super();
		this.size = size;
		this.cpus = cpus;
		bar = new Barrier(cpus);
		data = new double[size];
		this.bco = bco;
	} 

	public void sync() throws RemoteException {
// System.out.println("sync!");
	    bar.sync();
	}

	public synchronized void put(int offset, int size, double [] update, double residue) throws RemoteException { 

// System.out.println("put( " + offset + ", "+ size + ", " + update + ", " + residue);

		System.arraycopy(update, 0, data, offset, size);
		count++;
		sum_residue = sum_residue + residue;		

		if (count == cpus) { 

			phase++;
			
//			if ((phase % 10)== 0) {
//				System.out.println("Central residue = " + sum_residue);
//			}
				
			if (sum_residue < Main.BOUND) {
				bco.put(null, true);
			} else { 
				sum_residue = 0.0;

				double [] temp = data;
				data = new double[this.size];
				bco.put(temp, false);
			}
			count = 0;
		} 
	}
} 



