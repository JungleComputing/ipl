import ibis.rmi.*;
import ibis.rmi.registry.*;
import ibis.rmi.server.*;
import java.io.IOException;
import java.net.InetAddress;

public class RMI_init {

    static Registry reg = null;
    static boolean isInitialized = false;

    public static Registry getRegistry(String registryOwner) 
	throws IOException {

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

	InetAddress[] reg_names = InetAddress.getAllByName(registryOwner);
	int i;
	for (i = 0; i < reg_names.length; i++) {
// System.out.println("Compare hosts " + InetAddress.getLocalHost() + " and " + reg_names[i]); System.out.flush();
	    if (reg_names[i].equals(InetAddress.getLocalHost())) {
		break;
	    }
	}
	if (i != reg_names.length) {
	    reg = LocateRegistry.createRegistry(port);
	} else {
	    while (reg == null) {
		try {
		    reg = LocateRegistry.getRegistry(registryOwner, port);
		} catch (ibis.rmi.RemoteException e) {
		    try {
			System.out.println("Look up registry: sleep a while..");
			Thread.sleep(500);
		    } catch (InterruptedException eI) {
		    }
		}
	    }
	}

	System.out.println("Registry is " + reg);

	return reg;
    }

	public static Remote lookup(String id) { 
		boolean connected = false;
		Remote temp = null;
		int wait = 1000;

		while (!connected) { 			
			try { 
				temp = Naming.lookup(id);
				connected = true;
			} catch (Exception e) { 
				try { 
					Thread.sleep(wait);
					if (wait < 6000) { 
						wait *= 2;
					}
				} catch (Exception e2) { 
					// ignore
				} 
			} 
		}
		return temp;
	}  
}
