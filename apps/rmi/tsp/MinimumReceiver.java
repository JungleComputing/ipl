import ibis.rmi.*;

public interface MinimumReceiver extends Remote {
	public void update(int min) throws RemoteException;
}
