/*
 * SOR.java
 * Successive over relaxation
 * SUN RMI version implementing a red-black SOR, based on earlier Orca source.
 * with cluster optimization, and split phase optimization, reusing a thread
 * each Wide-Area send. (All switchable)
 *
 * Rob van Nieuwpoort & Jason Maassen
 *
 */

import ibis.ipl.*;

class SOR {

	private static final double TOLERANCE = 0.00001;         /* termination criterion */
	private static final double LOCAL_STEPS = 0;
	private static final boolean PREV = true;
	private static final boolean NEXT = false;
			
	private final double r;
	private final double omega;
	private final double stopdiff; 
	private final int ncol;
	private final int nrow;        /* number of rows and columns */
	private final int size;
	private final int rank;        /* process ranks */
	private final int lb;
	private final int ub;          /* lower and upper bound of grid stripe [lb ... ub] -> NOTE: ub is inclusive*/

	private final double[][] g;

	private final SendPort leftS;
	private final SendPort rightS;
	private final ReceivePort leftR;
	private final ReceivePort rightR;
	private final SendPort reduceS;
	private final ReceivePort reduceR;
	
	SOR(int nrow, int ncol, int N, int rank, int size, 
	    SendPort leftS, SendPort rightS, 
	    ReceivePort leftR, ReceivePort rightR, 
	    SendPort reduceS, ReceivePort reduceR) {
		
		this.nrow = nrow; 
		this.ncol = ncol; 
		
		this.leftS = leftS;
		this.rightS = rightS;
		this.leftR = leftR;
		this.rightR = rightR;
		this.reduceS = reduceS;
		this.reduceR = reduceR;

		/* ranks of predecessor and successor for row exchanges */
		this.size = size;
		this.rank = rank;
		
		// getBounds 
		int n = N-1;
		int nlarge = n % size;
		int nsmall = size - nlarge;
		
		int size_small = n / size;
		int size_large = size_small + 1;
		
		int temp_lb;

		if (rank < nlarge) {          
			temp_lb = rank * size_large;
			ub = temp_lb + size_large;
		} else {
			temp_lb = nlarge * size_large + (rank - nlarge) * size_small;
			ub = temp_lb + size_small;
		}

		if (temp_lb == 0) { 
			lb = 1; /* row 0 is static */
		} else {
			lb = temp_lb;
		}

		r        = 0.5 * ( Math.cos( Math.PI / (ncol) ) + Math.cos( Math.PI / (nrow) ) );
		double temp_omega = 2.0 / ( 1.0 + Math.sqrt( 1.0 - r * r ) );
		stopdiff = TOLERANCE / ( 2.0 - temp_omega );
		omega    = temp_omega*0.8;                   /* magic factor */
	
		g = createGrid();
	}

	private double [][] createGrid() {
		
		double [][] g = new double[nrow][];  
		
		for (int i = lb-1; i<=ub; i++) { 
			g[i] = new double[ncol]; /* malloc the own range plus one more line */
			/* of overlap on each border */
		}
		
		/* initialize the grid */
		for (int i = lb-1; i <=ub; i++){
			for (int j = 0; j < ncol; j++){
				if (i == 0) g[i][j] = 4.56;
				else if (i == nrow-1) g[i][j] = 9.85;
				else if (j == 0) g[i][j] = 7.32;
				else if (j == ncol-1) g[i][j] = 6.88;
				else g[i][j] = 0.0;      
			}
		}

		return g;
	}
	
	private double stencil (int row, int col) {
		return ( g[row-1][col] + g[row+1][col] + g[row][col-1] + g[row][col+1] ) / 4.0;
	}

	private int even (int i) {
		return ( ( ( i / 2 ) * 2 ) == i ) ? 1 : 0;
	}
	
	private void send(boolean dest, double[] col) throws Exception {
		
		/* Two cases here: sync and async */
		WriteMessage m;

		if (dest == PREV) {
			m = leftS.newMessage();
		} else { 
			m = rightS.newMessage();
		} 

		m.writeArray(col);
		m.send();
		m.finish();
	}

	private void receive(boolean source, double [] col) throws Exception {

		ReadMessage m;

		if (source == PREV) {
			m = leftR.receive();
		} else {
			m = rightR.receive(); 
		}

		m.readArray(col);
		m.finish();
	} 

	private double reduce(double value) throws Exception { 

		if (rank == 0) { 
			for (int i=1;i<size;i++) { 				
				ReadMessage rm = reduceR.receive();
				double temp = rm.readDouble();
				if (value < temp) value = temp;
				rm.finish();
			} 

			WriteMessage wm = reduceS.newMessage();
			wm.writeDouble(value);
			wm.send();
			wm.finish();
		} else { 
			WriteMessage wm = reduceS.newMessage();
			wm.writeDouble(value);
			wm.send();
			wm.finish();
			
			ReadMessage rm = reduceR.receive();
			value = rm.readDouble();
			rm.finish();
		} 
		
		return value;
	} 
	
	public void start () throws Exception {
		
		long t_start,t_end;             /* time values */
		double maxdiff;

		if(rank==0) {
			System.out.println("Problem parameters");
			System.out.println("r       : " + r);
			System.out.println("omega   : " + omega);
			System.out.println("stopdiff: " + stopdiff);
			System.out.println("lb      : " + lb);
			System.out.println("ub      : " + ub);
			System.out.println("");
		} 

		// abuse the reduce as a barrier
		if (size > 1) {
			reduce(42.0);
		}		
		
		if (rank == 0) { 
			System.out.println("... and there off !");
			System.out.flush();
		}
		
		/* now do the "real" computation */
		t_start = System.currentTimeMillis();
		
		int iteration = 0;
		
		do {
			if (even(rank) == 1) { 
				if(rank != 0) send(PREV, g[lb]);
				if(rank != size-1) send(NEXT, g[ub-1]);
				if(rank != 0) receive(PREV, g[lb-1]);
				if(rank != size-1) receive(NEXT, g[ub]);
			} else { 
				if(rank != size-1) receive(NEXT, g[ub]);
				if(rank != 0) receive(PREV, g[lb-1]);
				if(rank != size-1) send(NEXT, g[ub-1]);
				if(rank != 0) send(PREV, g[lb]);
			} 

			maxdiff = 0.0;
						
			for (int phase = 0; phase < 2 ; phase++){
				for (int i = lb ; i < ub ; i++) {
					for (int j = 1 + (even(i) ^ phase); j < ncol-1 ; j += 2) {
						double gNew = stencil(i,j);
						double diff = Math.abs(gNew - g[i][j]);
						
						if ( diff > maxdiff ) {
							maxdiff = diff;
						}
						
						g[i][j] = g[i][j] + omega * (gNew-g[i][j]);		
					}
				}
			}
			
			if (size > 1) {
				maxdiff = reduce(maxdiff);
			} 
			
//			if(rank==0) {
//				System.err.println(iteration + "");			
//			} 
			
			iteration++;
			
		} while (maxdiff > stopdiff);
		
		t_end = System.currentTimeMillis();
		
//	if (size > 1) {
//		maxdiff = global.reduceDiff(maxdiff);
//	}
		
		if (rank == 0){
			System.out.println("SOR " + nrow + " x " + ncol + " took " + ((t_end - t_start)/1000.0) + " sec.");
			System.out.println("using " + iteration + " iterations, diff is " + maxdiff + " (allowed diff " + stopdiff + ")");
		}
	}

}




