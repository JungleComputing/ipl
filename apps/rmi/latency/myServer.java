import ibis.rmi.*;

interface myServer extends Remote { 	
	public void foo() throws RemoteException;
	public void quit() throws RemoteException;
} 
