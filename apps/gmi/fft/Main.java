/* Sun RMI One Dimensional Fast Fourier Transformation (FFT)

   port of the RMI version of Ronald Blankedaal
*/

import ibis.gmi.*;

class Main {

	int M, N, rootN, rowsperproc, cpus, host;
	double[] u, u2;
	int[][] distribution;

	Main(String[] argv) throws Exception {
		host = Group.rank();
		cpus = Group.size();

		if (argv.length == 0) {
			M = 16;
		} else if (argv.length != 1) {
			System.out.println("Parameters: M");
			System.exit(1);
		} else {
			try {
				M = Integer.parseInt(argv[0]);
			} catch (NumberFormatException e) {
				System.out.println(e.getMessage());
				System.exit(1);
			}
		}

		N = (1 << M);                
		rootN = (1 << (M / 2));      
		rowsperproc = rootN / cpus;  

		u  = new double[2 * rootN];   
		u2 = new double[2 * (N + rootN)];

		initU(u);
		initU2(u2);
		distribution = new int[2][cpus * cpus];   
		initdis();

		if (host == 0) {
			System.out.println("Sun RMI One Dimensional Fast Fourier Transformation");
			System.out.println("M:" + M + " N:" + N + " rootN:" + rootN + "  Running on " + cpus + " cpu(s), each having " + rowsperproc + " rows to process");			
			Group.create("FFT-Slaves", i_Slave.class, cpus);                              
		}

		Slave s = new Slave(cpus, N, M, rootN, rowsperproc, u, u2, distribution);
		Group.join("FFT-Slaves", s);	

		i_Slave group = (i_Slave) Group.lookup("FFT-Slaves");

		GroupMethod m = Group.findMethod(group, "void barrier()"); 
		m.configure(new CombinedInvocation("barrier", s.getRank(), cpus, new BarrierCombiner(), new SingleInvocation(0)), new ReturnReply(0));             		
		s.setGroup(group, s.getRank());
		s.run();
		System.exit(0);
	}

	void initdis() {
		int bla = 0;
		int end = cpus * cpus;

//		System.out.println("Initdis end = " + end);

		for (int i = 0; i < end; i++) {
			distribution[0][bla % end] = i / cpus;    
			distribution[1][bla % end] = i % cpus;
			bla += (cpus + 1);
		}

//  		for (int i = 0; i < cpus * cpus; i++) {
//  			System.out.println("X distribution[0][" + i + "] = " + (distribution[0][i]));
//  		}

//  		for (int i = 0; i < cpus * cpus; i++) {
//  			System.out.println("X distribution[1][" + i + "] = " + (distribution[1][i]));
//  		}


		//                                     1, 1, 1, 1, 1, 1
		//       0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5	
		//
		// op 4 [0, 3, 2, 1, 1, 0, 3, 2, 2, 1, 0, 3, 3, 2, 1, 0 ]
		//      [0, 1, 2, 3, 0, 1, 2, 3, 0, 1, 2, 3, 0, 1, 2, 3 ]
		//
	}


	void initU(double[] u) {
		for (int q = 0; (1 << q) < N; q++) {
			int n1 = 1 << q;
			int base = n1 - 1;
			for (int j = 0; j < n1; j++) {
				if (base + j > rootN - 1)
					return;
				u[(base + j)*2] = Math.cos(2.0 * Math.PI * j / (2 * n1));
				u[(base + j)*2 + 1] = -Math.sin(2.0 * Math.PI * j / (2 * n1));
			}
		}
	}


	void initU2(double[] u2) {
		for (int j = 0; j < rootN; j++) {
			int k = j * rootN;
			for (int i = 0; i < rootN; i++) {
				u2[(k + i)*2] = Math.cos(2.0 * Math.PI * i * j / N);
				u2[(k + i)*2 + 1] = -Math.sin(2.0 * Math.PI * i * j / N);
			}
		}
	}

	public static void main(String argv[]) {
		try { 
			new Main(argv);
		} catch (Exception e) { 
			System.err.println("oops: Main got exception " + e);
			e.printStackTrace();
		} 
	}
}
