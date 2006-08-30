/* $Id$ */

import ibis.util.PoolInfo;

// import java.rmi.server.RMISocketFactory;
// import FastSocket.RMI_FSSocketFactory;
import java.rmi.RemoteException;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import java.io.IOException;

class RMI_Bench {

    static final int SERVER_HOST	= 1; // 0;
    static final int REGISTRY_HOST	= SERVER_HOST;


    public static void main(String[] argv) {
	PoolInfo dasInfo = null;
	try {
	    dasInfo = PoolInfo.createPoolInfo();
	} catch(Exception e) {
	    System.err.println("Oops: " + e);
	    e.printStackTrace();
	    System.exit(1);
	}
	int	my_cpu = dasInfo.rank();
	int	ncpus  = dasInfo.size();
	String	masterName = dasInfo.hostName(REGISTRY_HOST);
	Registry local = null;

	// System.runFinalizersOnExit(true); 

	// System.out.println(my_cpu + ": hi, I'm alive");

	try {
	    local = new RMI_init().getRegistry(masterName);
	} catch (IOException ei) {
	    ei.printStackTrace();
	    System.exit(33);
	}

	System.out.println(my_cpu + ": connected to registry");
	System.out.println("ncpus " + ncpus + " my_cpu " + my_cpu);

	if (ncpus == 1 || my_cpu == SERVER_HOST) {
	    try {
		System.out.println("RMI Benchmark");
		Server s = new Server(argv, local);
		if (ncpus == 1) {
		    Thread t = new Thread(s);
		    t.setName("RMI_Bench server");
		    t.start();
		} else {
		    s.run();
		}
	    } catch (RemoteException e) {
		System.err.println("Cannot create Server object: " + e);
		e.printStackTrace();
		System.exit(34);
	    }
	}

	if (ncpus == 1 || my_cpu != SERVER_HOST) {
	    new Client(argv, masterName);
	}

	// System.exit(0);
    }
}
