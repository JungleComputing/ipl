import java.rmi.*;

interface myServer extends Remote { 	
	public void foo() throws RemoteException;
	public int bar() throws RemoteException;
} 
