import ibis.group.*;

class Main {

	static final int COUNT  = 1000;
	static final int REPEAT = 10;
	static final int SIZE   = 50*1024;

	public static void main(String[] argv) {

		try {
			int nodes  = Group.size();
			int rank   = Group.rank();
			int count  = COUNT;
			int repeat = REPEAT;
			int size   = SIZE;
			int num    = 0;

			boolean lat = false;
			boolean tp  = false;
			boolean nocopy  = false;

			byte [] tpdata = null;
			Data data = null;
			i_Data group = null;

			long start = 0;
			long end = 0;
			long bartime = 0;
			double best = 0.0;
			
			for (int i = 0; i < argv.length; i++) {
				if (false) {
				} else if (argv[i].equals("-count")) {
					count = Integer.parseInt(argv[i+1]);
					i++;
				} else if (argv[i].equals("-size")) {
					size = Integer.parseInt(argv[i+1]);
					i++;
				} else if (argv[i].equals("-repeat")) {
					repeat = Integer.parseInt(argv[i+1]);
					i++;
				} else if (argv[i].equals("-lat")) {
					lat = true;
				} else if (argv[i].equals("-tp")) {
					tp = true;
				} else if (argv[i].equals("-nocopy")) {
					nocopy = true;
				} else {
					if (rank == 0) {
						System.out.println("No such option: " + argv[i]);
						System.out.println("usage : bench [-count N] [-size M] [-repeat X] [-lat] [-tp]");
					}
					System.exit(33);
				}
			}

			if (!lat && !tp) { 
				lat = true;
				tp = true;
			} 

			if (rank == 0) { 
				Group.create("Data", i_Data.class, nodes);
			} 

			data = new Data();
			Group.join("Data", data);	

			group = (i_Data) Group.lookup("Data");
			GroupMethod m = Group.findMethod(group, "void storeL()");

			CombinedInvocation clat = new CombinedInvocation("Latency", rank, nodes, new LatCombiner(), new SingleInvocation(0));
			m.configure(clat, new DiscardReply());
			
			m = Group.findMethod(group, "void storeT(byte[])");
			CombinedInvocation ctp = new CombinedInvocation("Throughput", rank, nodes, new TPCombiner(size, nocopy), new SingleInvocation(0));
			m.configure(ctp, new DiscardReply());
			
			// everybody ready ?
			//

			int myrank = data.myGroupRank;

			if (lat) {
				
				if (myrank == 0) { 
					System.out.println("Running collective reduce latency test: count = " + count + " repeat = " + repeat);
					best = 1000000.0;
				} 
			
				for (int r=0;r<repeat;r++) { 

					if (myrank == 0) { 
						start = System.currentTimeMillis();
					} 
					
					for (int c=0;c<count;c++) { 
					    group.storeL();
					    if (myrank == 0) { 
						    data.retrieveL();
					    } 
					}

					
					if (myrank == 0) { 
						end = System.currentTimeMillis();
						double temp;

						temp = (1000.0*(end-start))/count;
						System.out.println("Test " + r + " took " + temp + " usec/bcast");
						if (temp < best) { 
							best = temp;
						} 
					} 
				} 

				if (myrank == 0) { 
					System.out.println("Lowest latency " + best + " usec/bcast");
				} 
			}

			if (tp) { 				
				if (myrank == 0) { 
					best = 0.0;
					System.out.println("Running collective reduce throughput test: count = " + count + " repeat = " + repeat + " size = " + size);
				} 

				tpdata = new byte[size/nodes];
	
				for (int r=0;r<repeat;r++) { 

					if (myrank == 0) { 
						start = System.currentTimeMillis();
					} 
					
					for (int c=0;c<count;c++) { 
					    group.storeT(tpdata);
					    if (myrank == 0) { 
						    data.retrieveT();
					    } 
					}

					if (myrank == 0) { 
						end = System.currentTimeMillis();
						double time;

						time = ((end-start))/1000.0;

						double temp = (size * count)/(1024.0*1024.0);
						temp = temp/time;

						System.out.println("Test " + r + " tp " + temp + " MByte/s");
						if (temp > best) { 
							best = temp;
						} 
					} 
				} 

				if (myrank == 0) { 
					System.out.println("Highest tp " + best + " MByte/s");
				} 
			}
			
		
		} catch (Exception e) {
			System.out.println("Oops " + e);
			e.printStackTrace();
		}
		System.exit(0);
	}
}
