import ibis.rmi.*;

interface MyServer extends Remote { 	
	public void foo() throws RemoteException;
	public void quit() throws RemoteException;
} 
