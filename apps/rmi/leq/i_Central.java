import java.rmi.*; 

interface i_Central extends Remote { 
	public void put(int offset, int size, double [] update, double residue) throws RemoteException;
	public void sync() throws RemoteException;
} 
