import java.rmi.Remote;
import java.rmi.RemoteException;

interface MyServer extends Remote { 	
	public void foo() throws RemoteException;
	public void quit() throws RemoteException;
} 
