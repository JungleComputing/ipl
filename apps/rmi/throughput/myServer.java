import java.rmi.*;

interface myServer extends Remote { 	
	public void one(Object o) throws RemoteException;
	public Object two(Object o) throws RemoteException;

	// for termination
	public void done() throws RemoteException;
} 
