import java.rmi.*;

interface i_BroadcastObject extends Remote { 
	public void send(int broadcast, Object o, int owner) throws RemoteException;
}
