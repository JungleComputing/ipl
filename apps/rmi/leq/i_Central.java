import java.rmi.Remote;
import java.rmi.RemoteException;

interface i_Central extends Remote { 
	public void put(int offset, int size, double [] update, double residue) throws RemoteException;
	public void sync() throws RemoteException;
} 
