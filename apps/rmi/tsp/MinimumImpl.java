import ibis.rmi.*;
import ibis.rmi.server.UnicastRemoteObject;

class MinimumImpl extends UnicastRemoteObject implements Minimum {
	private int minimum;
	private MinimumReceiver[] minReceiverTable = new MinimumReceiver[Misc.MAX_CLIENTS];
	private int nrClients;

	public MinimumImpl(int minimum) throws RemoteException {
		this.minimum = minimum;
		nrClients = 0;
	}

	public MinimumImpl() throws RemoteException {
		// this(2817);
		this(Integer.MAX_VALUE);
	}


	public synchronized void set(int minimum) throws RemoteException {
// System.out.println("Received minimumImpl.set " + minimum);
		if(minimum < this.minimum) {
			this.minimum = minimum;

// System.out.println("Sending updates ...");
			// Send min to all clients...
			for(int i=0; i<nrClients; i++) {
// System.out.println("to client " + i);
				minReceiverTable[i].update(minimum);
			}
		}
// System.out.println("Done minimumImpl.set " + minimum);
	}


	public synchronized int get() throws RemoteException {
		return minimum;
	}


	public void register(MinimumReceiver minReceiver) throws RemoteException {
		minReceiverTable[nrClients] = minReceiver;
		nrClients++;
	}
}
