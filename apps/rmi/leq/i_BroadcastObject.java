import java.rmi.Remote;
import java.rmi.RemoteException;

interface i_BroadcastObject extends Remote { 
	public void put(double [] update, boolean stop) throws RemoteException;
} 
