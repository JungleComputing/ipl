import ibis.rmi.*;
import ibis.rmi.server.UnicastRemoteObject;

class MinimumReceiverImpl extends UnicastRemoteObject implements MinimumReceiver {
	Client client;

	MinimumReceiverImpl(Client client) throws RemoteException {
		this.client = client;
	}


	public final void update(int min) throws RemoteException {
// System.out.println("Got update " + min);
		client.setMin(min);
	}
}
