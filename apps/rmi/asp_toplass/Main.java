import ibis.util.PoolInfo;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.Registry;

class Main {

public static void main(String[] argv) {
	try {
		PoolInfo info = PoolInfo.createPoolInfo();
	
		int n = 4000;
		
		int nodes      = info.size();
		int rank       = info.rank();
		
		boolean use_threads     = false;
		boolean use_thread_pool = false;
		boolean print_result    = false;
		
		int option = 0;
		
		Asp local           = null;
		i_GlobalData global = null;
		Registry reg        = null;
		i_Asp[] table       = null;
		
		for (int i = 0; i < argv.length; i++) {
			if (false) {
			} else if (argv[i].equals("-threads")) {
				use_threads = true;
			} else if (argv[i].equals("-print")) {
				print_result = true;			
			} else if (argv[i].equals("-thread-pool")) {
				use_threads = true;
				use_thread_pool = true;
			} else if (option == 0) {
				n = Integer.parseInt(argv[i]);
				option++;
			} else {
				if (rank == 0) {
					System.out.println("No such option: " +
							   argv[i]);
				}
				System.exit(33);
			}
		}
		
		if(option > 1) {
			if (rank == 0) {
				System.out.println("Usage: asp <n>");
			}
			System.exit(1);
		}
		
		// Start the registry.    
		reg = RMI_init.getRegistry(info.hostName(0));

		if (info.rank() == 0) {
			global = new GlobalData(info);
			RMI_init.bind("GlobalData", global);	
		} else {	    
			global = (i_GlobalData) RMI_init.lookup("//" + info.hostName(0) + "/GlobalData");
		}
		

		local = new Asp(global, n, use_threads, use_thread_pool, print_result);
		table = global.table((i_Asp) local, info.rank());
		local.setTable(table);		
		local.start();
		
		if (info.rank() == 0) {
			Thread.sleep(2000); 
			// Give the other nodes a chance to exit.
			// before cleaning up the GlobalData object.
		} 
		
		local  = null;
		table  = null;
		global = null;
		reg    = null;
		
		System.gc();
		
	} catch (Exception e) {
		System.out.println("Oops " + e);
		e.printStackTrace();
	}
	
	System.exit(0);
}



}
