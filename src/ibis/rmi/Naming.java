package ibis.rmi;

import ibis.ipl.*;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.net.MalformedURLException;

public class Naming {

	public static void bind(String name, Remote obj) throws AlreadyBoundException, MalformedURLException, RemoteException { 
		// Binds the specified name to a remote object.
		int result;
		
		try { 
			result = RTS.bind(name, obj);
		} catch (Exception e) { 
			throw new RemoteException("bind failed " + e);
		} 

		if (result == -1) { 
			throw new AlreadyBoundException("//" + RTS.hostname + "/" + name + " already bound");
		}
	}		

	public static Remote lookup(String name) throws NotBoundException, MalformedURLException, RemoteException {
		// Returns a reference, a stub, for the remote object associated with the specified name.
		try { 
			return RTS.lookup(name);
		} catch (IbisException e) { 
			throw new RemoteException("lookup(" + name + ") failed " + e);
		} 
	}		
	
	public static String[] list(String name)   throws RemoteException, MalformedURLException { 
		// Returns an array of the names bound in the registry.
		System.err.println("Naming.list not implemented");
		return null;
	} 

	public static void rebind(String name, Remote obj) throws RemoteException, MalformedURLException {
		// Rebinds the specified name to a new remote object.
		System.err.println("Naming.rebind not implemented");
	}

	public static void unbind(String name) throws RemoteException, NotBoundException, MalformedURLException {
		// Destroys the binding for the specified name that is associated with a remote object.   
		System.err.println("Naming.unbind not implemented");
	}

} 
