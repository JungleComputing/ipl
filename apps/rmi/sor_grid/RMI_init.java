import java.rmi.Remote;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.server.RMISocketFactory;
import java.rmi.RemoteException;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import java.io.IOException;
import java.net.InetAddress;

import ibis.util.IPUtils;

public class RMI_init {

    static Registry reg = null;
    static boolean isInitialized = false;


    public static Registry getRegistry(String registryOwner) 
	throws IOException {

	// System.out.println("In RMI_init.getRegistry");

	if (isInitialized) {
	    System.out.println("Registry already initialized");
	    return reg;
	}
	isInitialized = true;

	String version = System.getProperty("java.version");
	if (version == null || version.startsWith("1.1")) {
			    // Start a security manager.
	    // System.out.println("Start a security manager");
	    System.setSecurityManager(new RMISecurityManager());
	}

	int port = Registry.REGISTRY_PORT;

	InetAddress addr    = IPUtils.getLocalHostAddress();
	InetAddress regAddr = InetAddress.getByName(registryOwner);

	if (addr.equals(regAddr)) {
	    while (reg == null) {
		try {
		    reg = LocateRegistry.createRegistry(port);
		    System.out.println("Use LocateRegistry to create a Registry");
		    break;
		} catch (RemoteException e) {
		}
		try {
		    reg = LocateRegistry.getRegistry(registryOwner, port);
		    System.out.println("Use LocateRegistry to get a Registry from owner " + regAddr);
		    break;
		} catch (RemoteException e) {
		}
	    }

	} else {
	    System.out.println("Use LocateRegistry to get a Registry from owner " + regAddr);
	    while (reg == null) {
		try {
		    reg = LocateRegistry.getRegistry(registryOwner, port);
		} catch (RemoteException e) {
		    try {
			System.out.println("Look up registry: sleep a while..");
			Thread.sleep(100);
		    } catch (InterruptedException eI) {
		    }
		}
	    }
	}

	// System.out.println("Registry is " + reg);

	return reg;
    }


    /**
     * A wrapper for the RMI nameserver lookup
     */
    public static Remote lookup(String id) throws IOException { 
	boolean connected = false;
	Remote temp = null;
	int wait = 100;

	while (!connected) { 			
	    if (wait < 6000) { 
		wait *= 2;
	    }
	    try { 
		temp = Naming.lookup(id);
		connected = true;

	    } catch (java.rmi.NotBoundException eR) { 
		try { 
		    Thread.sleep(wait);
		} catch (Exception e2) { 
		    // ignore
		} 

	    } catch (java.rmi.ConnectException eR) { 
		try { 
		    Thread.sleep(wait);
		} catch (Exception e2) { 
		    // ignore
		} 
	    } 
	}
	return temp;
    }


    /**
     * A wrapper for the RMI nameserver bind
     */
    public static void bind(String name, Remote obj)
	    throws IOException, java.rmi.AlreadyBoundException {
	Naming.bind(name, obj);
    }


    /**
     * A wrapper for the RMI nameserver unbind
     */
    public static void unbind(String name)
	    throws IOException, java.rmi.NotBoundException {
	Naming.unbind(name);
    }


    /**
     * A wrapper for the RMI nameserver rebind
     */
    public static void rebind(String name, Remote obj) throws IOException {
	Naming.rebind(name, obj);
    }


    public static void main(String[] args) {
	System.out.println("Got registry:");
	try {
	    System.out.println(getRegistry(args[0]));
	} catch (IOException e) {
	    System.out.println("The bugger threw an exception: " + e);
	}
    }
}
