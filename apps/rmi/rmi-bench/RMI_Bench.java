import ibis.util.PoolInfo;

import ibis.rmi.registry.*;
import ibis.rmi.*;
import java.net.*;
import java.io.*;
// import ibis.rmi.server.RMISocketFactory;
// import FastSocket.RMI_FSSocketFactory;
import java.io.IOException;

class RMI_Bench {

    static final int SERVER_HOST	= 1; // 0;
    static final int REGISTRY_HOST	= 0; // 0;


    public static void main(String[] argv) {
	PoolInfo dasInfo = new PoolInfo();
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

	// System.out.println(my_cpu + ": connected to registry");

	if (ncpus == 1 || my_cpu == SERVER_HOST) {
	    try {
		System.out.println("RMI Benchmark");
		Server s = new Server(argv, local);
		if (ncpus == 1) {
		    new Thread(s).start();
		} else {
		    s.run();
		}
	    } catch (ibis.rmi.RemoteException e) {
		System.err.println("Cannot create Server object: " + e);
		e.printStackTrace();
		System.exit(34);
	    }
	}

	if (ncpus == 1 || my_cpu != SERVER_HOST) {
	    new Client(argv, masterName, local);
	}

	// System.exit(0);
    }
}
