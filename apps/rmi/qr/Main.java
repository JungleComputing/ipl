import java.rmi.*;
import java.rmi.registry.*;

import ibis.util.PoolInfo;

class Main {

	static final int N = 2000;

	public static void main(String [] args) {

		try { 
			PoolInfo info = new PoolInfo();
			
			int n = N;
			int ncpus = info.size();
			int cpu = info.rank();
			
			// parse paremeters here.

			Registry reg = RMI_init.getRegistry(info.hostName(cpu));		
			Data real_data = null;
				
			if (args.length > 0) { 
				n = Integer.parseInt(args[0]);
			} 

			// RuntimeSystem.setTarget(0);
			if (cpu == 0) {
				System.out.println("Starting QR of A[" + n + "][" + n + "] on " + ncpus + " cpus.");

				real_data = new Data(info, ncpus);
				Naming.bind("Data", real_data);
				
				Reduce reduce = new Reduce(ncpus);		
				Naming.bind("Reduce", reduce);
			} 

			BroadcastObject bcast = new BroadcastObject(cpu, ncpus, n);
			Naming.bind("BCAST" + cpu, bcast);
			
			i_Data data = (i_Data) RMI_init.lookup("//" + info.hostName(0) + "/Data");
			i_Reduce reduce = (i_Reduce) RMI_init.lookup("//" + info.hostName(0) + "/Reduce");

			Remote [] temp = data.signup(cpu, "BCAST" + cpu);
			
			i_BroadcastObject [] bco = new i_BroadcastObject[ncpus];
			for (int i=0;i<ncpus;i++) bco[i] = (i_BroadcastObject) temp[i];
			
			bcast.connect(bco);
			
			System.out.println("New QR_Worker on " + ncpus);
			
			QR_Pivot work = new QR_Pivot(info, n, data, reduce, bcast);
			work.qrfac();
			
			if (cpu == 0) { 
				long    max   = real_data.max_time();
				double avg    = real_data.avg_time();
				double stddev = real_data.stddev_time();
			
				System.out.println("time : max = " + max + " avg = " + avg + " stddev = " + stddev);
			}  
			System.exit(0);	
		} catch (Exception e) {
			System.out.println("Main got " + e);
			e.printStackTrace();
			System.exit(1);
		} 
	}
}


