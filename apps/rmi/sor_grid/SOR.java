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

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import ibis.util.PoolInfo;

class SOR extends UnicastRemoteObject implements i_SOR {

	static final double TOLERANCE = 0.001;         /* termination criterion */
	static final double MAGIC = 1.00;               /* magic factor */
	static final double LOCAL_STEPS = 0;

	static final boolean PREV = true;
	static final boolean NEXT = false;

	static final int    SYNC_SEND = 0;
	static final int   ASYNC_SEND = 1;

	i_GlobalData global;

	double[][] g;

	int ncol,nrow;                    /* number of rows and columns */

	double r,omega,                   /* some float values */
		stopdiff,maxdiff,diff;     /* differences btw grid points in iters */

	int nodes, rank;                   /* process ranks */

	int lb,ub;                        /* lower and upper bound of grid stripe [lb ... ub] -> NOTE: ub is inclusive*/

	i_SOR[] table;                      /* a table of all SOR threads */

	int waiting = 0;                  /* make sure we don't loose notifyAll calls */

	boolean receivedPrev=false, receivedNext=false;
	double[] prevCol, nextCol;

	int prevIndex, nextIndex;
	int nit;
	int sync;
	boolean visualization;

	WaitingSendThread prevSender, nextSender;

	private double[] nodeSpeed;	/* Speed of node[i] */

	private long t_start,t_end;     /* time values */

	PoolInfo info;

	SOR(int nrow, int ncol, int nit, int sync, i_GlobalData global, boolean visualization, PoolInfo info) throws RemoteException {
		this.nrow = nrow; // Add two rows to borders.
		this.ncol = ncol; // Add two columns to borders.
		this.nit  = nit;
		this.visualization = visualization;

		this.sync       = sync;
		this.info = info;

		this.global = global;

		/* ranks of predecessor and successor for row exchanges */
    
		nodes = info.size();
		rank = info.rank();

		this.nodeSpeed = new double[nodes];
		for (int i = 0; i < nodes; i++) {
		    this.nodeSpeed[i] = 1.0;
		}
    
		if (sync == ASYNC_SEND) {
			prevSender = new WaitingSendThread();
			nextSender = new WaitingSendThread();
			prevSender.start();
			nextSender.start();
		}
	}

	long getElapsedTime() {
	    return t_end - t_start;
	}

	void setNodeSpeed(double[] nodeSpeed) {
	    this.nodeSpeed = nodeSpeed;
	}
	public void setTable(i_SOR[] table) {
		this.table = table;
	}

	private void createGrid() {

		g = new double[nrow][0];  
  
		for (int i = lb; i<=ub; i++){ 
			g[i] = new double[ncol];
		}


		// initialize the grid 
		for (int i = lb; i <=ub; i++){
			for (int j = 0; j < ncol; j++){
				if (i == 0) g[i][j] = 4.56;
				else if (i == nrow-1) g[i][j] = 9.85;
				else if (j == 0) g[i][j] = 7.32;
				else if (j == ncol-1) g[i][j] = 6.88;
				else g[i][j] = 0.0;
			}
		}
	}

	private double abs(double d) {

		if (d > 0.0) {
			return d;
		} else {
			return -d;
		}
	}

	private double stencil (int row, int col) {
		return ( g[row-1][col] + g[row+1][col] + g[row][col-1] + g[row][col+1] ) / 4.0;
	}


        /* compute lower and upper bound of the grid stripe */
	private void get_bounds() {
  
	    // Very lame, but ensures same init as orca.
	
	    int llb      = 0;
	    int grain    = 0; 
	
	    double speed_avg = 0;
	    for (int i=0;i<nodes;i++) {
		speed_avg += nodeSpeed[i];
	    }
	    speed_avg /= nodes;
	    boolean homogeneous = true;
	    for (int i = 0; i < nodes; i++) {
		if (nodeSpeed[i] != speed_avg) {
		    homogeneous = false;
		    break;
		}
	    }
System.out.println("in get_bounds(); nodes " + nodes + " speed_avg " + speed_avg);
	
	    if (homogeneous) {
		for (int i=0;i<nodes;i++) {
			grain = (nrow-2-llb) / (nodes-i);
		
			if (i == rank) {
				lb = llb;
				ub = llb+grain+1;
				break;
			}
		
			llb += grain;
		}
	    } else {
		for (int i=0;i<nodes;i++) {
		    if (i == nodes - 1) {
			grain = nrow - 2 - llb;
		    } else {
			grain = (int)(((nrow-2) * nodeSpeed[i]) / (nodes * speed_avg));
		    }

		    if (i == rank) {
			    lb = llb;
			    ub = llb+grain+1;
			    break;
		    }

		    llb += grain;
		}
	    }
	
		// System.out.println(rank + " [" + lb + " ... " + ub + "]");
	}


	private double compute(int color, double maxdiff, int lb, int ub) {

		double diff;
		double gNew;
	
		int r, c;

		for (r = lb+1 ; r <= ub-1 ; r++) {
			c = 1 + ((r+color) & 1);
		
			while (c < ncol-1) {
			
				gNew = stencil(r,c);
				diff = abs(gNew - g[r][c]);
			
				if ( diff > maxdiff ) {
					maxdiff = diff;
				}
		    
				g[r][c] = g[r][c] + omega * (gNew-g[r][c]);
			
				c += 2;
			}
		}

		return maxdiff;
	}


	public synchronized void putCol(boolean sender, int index, double[] col) throws RemoteException {

		if(sender == PREV) {

			while(receivedPrev) {
				try {
					wait();
				} catch (Exception e) {
					System.err.println("putCol while waiting during receivedPrev:");
					e.printStackTrace();
				}
			}

			receivedPrev = true;
			prevCol = col;
			prevIndex = index;

		} else {
	    
			while(receivedNext) {
				try {
					wait();
				} catch (Exception e) {
					System.err.println("putCol while waiting during receivedNext:");
					e.printStackTrace();
				}
			}
	    
			receivedNext = true;
			nextCol = col;
			nextIndex = index;
		}

		notifyAll();
	}


	public synchronized void recCol(boolean sender) {

		if(sender == PREV) {

			while(!receivedPrev) {
				try {
					wait();
				} catch (Exception e) {
					System.err.println("recCol while waiting during !receivedPrev:");
					e.printStackTrace();
				}
			}

			receivedPrev = false;
			g[prevIndex] = prevCol;
		} else {

			while(!receivedNext) {

				try {
					wait();
				} catch (Exception e) {
					System.err.println("recCol while waiting during !receivedNext:");
					e.printStackTrace();
				}
			}

			receivedNext = false;
			g[nextIndex] = nextCol;
		}

		notifyAll();
	}

	private void send(boolean i_am, int index, double[] col) throws RemoteException {
		/* Two cases here: sync and async */
		if (i_am == NEXT) {
			switch(sync) {
			case SYNC_SEND:
				// System.out.println("sending row to " + (rank-1));
				table[rank-1].putCol(NEXT, index, col);
				break;
			case ASYNC_SEND:
				nextSender.put(table[rank-1], NEXT, index, col);
				break;
			default:
				System.err.println(rank + " Check out send!");
			} 
		} else {
			switch(sync) {
			case SYNC_SEND:
				// System.out.println("sending row to " + (rank+1));
				table[rank+1].putCol(PREV, index, col);
				break;
			case ASYNC_SEND:
				prevSender.put(table[rank+1], PREV, index, col);				
				break;
			default:
				System.err.println(rank + " Check out send!");
			} 
		}	
	}

	public void start (boolean doIO, String runName) throws RemoteException {
		int phase;
		int iteration;               /* counters */

		r        = 0.5 * ( Math.cos( Math.PI / (ncol) ) + Math.cos( Math.PI / (nrow) ) );
		omega    = 2.0 / ( 1.0 + Math.sqrt( 1.0 - r * r ) );
		stopdiff = TOLERANCE / ( 2.0 - omega );
		omega   *= MAGIC;                   /* magic factor */
	
		/* get my stripe bounds and malloc the grid accordingly */
		get_bounds();
		createGrid();
	
		if( doIO && rank==0 && runName.equals("SOR")) {
			System.out.println("Problem parameters");
			System.out.println("r       : " + r);
			System.out.println("omega   : " + omega);
			System.out.println("stopdiff: " + stopdiff);
			System.out.println("lb      : " + lb);
			System.out.println("ub      : " + ub);
			System.out.println("");
		} 

		int width = 0;
		int height = 0;
		float[][] canvas = null;
		if(visualization) {
		    width = global.getRawDataWidth();
		    height = global.getRawDataHeight();
		    canvas = GlobalData.createDownsampledCanves(g, width, height);
		}

		/* now do the "real" computation */
		t_start = System.currentTimeMillis();

		iteration = 0;

		// ibis.util.Timer rdtsc = null;
		// if (rank == 0) {
		//     rdtsc = newTimer("ibis.util.nativeCode.Rdtsc");
		// }

		do {
			/*
			  // insert a source
			for(int i=0; i<50; i++) {
				for(int j=0; j<50; j++) {
					g[i+50][j+50] = 10.0;
				}
			}
			

                        
			  // insert a "black hole"
			for(int i=0; i<50; i++) {
				for(int j=0; j<50; j++) {
					g[g.length-i-50][g[0].length-j-50] = 0.0;
				}
			}
			*/

			if(visualization) {
				GlobalData.downsample(g, canvas, width, height);
				global.putMatrix(canvas);
			}

			diff = 0.0;

			// if (rank == 0) {
			//     rdtsc.reset();
			//     rdtsc.start();
			// }
		
			for (phase = 0; phase < 2 ; phase++){
				// send to prev
			
				if(rank != 0) {
					send(NEXT, lb+1, g[lb+1]);
				} 

				if(rank != nodes-1) {
					send(PREV, ub-1, g[ub-1]);
				}

				switch(sync) {
				case SYNC_SEND:
					if(rank != 0) {
						// System.out.println("Getting row from " + (rank-1));
						recCol(PREV);
						// System.out.println("Got row from " + (rank-1));
					}
					
					if(rank != nodes-1) {
						// System.out.println("Getting row from " + (rank+1));
						recCol(NEXT);
						// System.out.println("Got row from " + (rank+1));
					}
					
					diff = compute(phase, diff, lb, ub);
					break;
				case ASYNC_SEND:
					diff = compute(phase, diff, lb+1, ub-1);

					if(rank != 0) {
						recCol(PREV);
						nextSender.finish_send();
					}
					
					if(rank != nodes-1) {
						recCol(NEXT);
						prevSender.finish_send();
					}
				
					diff = compute(phase, diff, lb, lb+2);
					diff = compute(phase, diff, ub-2, ub);
					break;
				}
			}

			if (nit == 0 && nodes > 1) {
				maxdiff = global.reduceDiff(diff);
			} else {
				// System.out.println("diff = " + diff);
				maxdiff = diff;
			}
		
			iteration++;
	
			if(doIO && rank==0) {
				System.err.print(".");			

				// rdtsc.stop();
				// long t = (long)(rdtsc.totalTimeVal() * 10);
				// System.out.println("" + (t / 10000000.0));

				//			System.out.flush();
				//System.out.println("" + maxdiff);
			}

		} while ((nit!=0 && iteration < nit) || (nit==0 && maxdiff > stopdiff));

		t_end = System.currentTimeMillis();

		if (nodes > 1) {
			maxdiff = 0.0;
			maxdiff = global.reduceDiff(diff);
		} else {
			maxdiff = diff;
		}

		if (doIO && (rank == 0 || nodes == 1)){
			System.out.println(runName + " " + nrow + " x " + ncol + " took " + ((t_end - t_start)/1000.0) + " sec.");
			System.out.println("using " + iteration + " iterations, diff is " + maxdiff + " (allowed diff " + stopdiff + ")");
			if (! runName.equals("warmup")) {
			    System.out.println("Application: " + "SOR " + nrow + " x " + ncol + "; Ncpus: " + info.size() +
					       "; time: " + (t_end-t_start)/1000.0 + " seconds\n");
			}
		}
	}
}
