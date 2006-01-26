/* $Id$ */

 /*
 * Successive over relaxation
 * MPI version implementing a red-black SOR, based on earlier Orca source.
 *
 * Written by Rob van Nieuwpoort, 6-oct-2003
 * ported to MPJ by Markus Bornemann, 9-april-2005
 */

import java.text.DecimalFormat;

import ibis.mpj.*;


public class SOR {

	public static double TOLERANCE = 0.00002;        /* termination criterion */
	public static double MAGIC  = 0.8;                /* magic factor */

	static private int even(int i) {
		if ((i % 2) == 0) return 1;
		else return 0;
	}


	static private double stencil (double[][] G, int row, int col) {
		return (G[row-1][col] + G[row+1][col] + G[row][col-1] + G[row][col+1]) / 4.0;
	}


	/* compute lower and upper bound of the grid stripe */
	static private void get_bounds(int[] lb, int[] ub, int n, int size, int rank)
	{
		int nlarge, nsmall;
		int size_large, size_small;

		nlarge = n % size;
		nsmall = size - nlarge;
    
		size_small = n / size;
		size_large = size_small + 1;

		if (rank < nlarge) {            /* I'll  have a large chunk */
			lb[0] = rank * size_large;
			ub[0] = lb[0] + size_large;
		} else {
			lb[0] = nlarge * size_large + (rank - nlarge) * size_small;
			ub[0] = lb[0] + size_small;
		}

		if (lb[0] == 0) lb[0] = 1; /* row 0 is static */
	}


/* Processor 0 allocates complete table, others only their part. */
/*void alloc_grid(double*** Gptr, int nrow, int ncol, int lb, int ub, int rank)
{
	int i;
	double** G = (double**) malloc(nrow*sizeof(double*));
	if ( G == 0 ) {
		fprintf(stderr, "malloc failed\n");
		exit(42);
	}

	if(rank == 0) {
		for (i = 0; i<nrow; i++) { // malloc the own range plus one more line 
			// of overlap on each border 
			G[i] = (double*) malloc(ncol*sizeof(double));
			if ( G[i] == 0 ) {
				fprintf(stderr, "malloc failed\n");
				exit(42);
			}
		}
	} else {
		for (i = lb-1; i<=ub; i++) { // malloc the own range plus one more line 
			// of overlap on each border 
			G[i] = (double*) malloc(ncol*sizeof(double));
			if ( G[i] == 0 ) {
				fprintf(stderr, "malloc failed\n");
				exit(42);
			}
		}
	}

	*Gptr = G;
}*/


/*void free_grid(double** G, int nrow, int ncol, int lb, int ub, int rank) {
	int i;

	if(rank == 0) {
		for (i = 0; i<nrow; i++) {
			free(G[i]);
		}
	} else {
		for (i = lb-1; i<=ub; i++) { // malloc the own range plus one more line 
			free(G[i]);
		}
	}

	free(G);
}*/


	static private void initGrid (double[][] G, int N) {
		int i, j;
		
		for (i = 0; i < N; i++) {
			for (j = 0; j < N; j++) {
				if (i==0) G[i][j] = 4.56;
				else if (i == N-1) G[i][j] = 9.85;
				else if (j == 0) G[i][j] = 7.32;
				else if (j == N-1) G[i][j] = 6.88;
				else G[i][j] = 0.0;
			}
		}
	}


	static private void printGrid(double[][] G, int N) {
		int i, j;
		DecimalFormat ft = new DecimalFormat("0.000");
		for (i = 1; i < N-1; i++) {
			for (j = 1; j < N-1; j++) {
				System.out.print(ft.format(G[i][j]) + " ");
			}
			System.out.println();
		}
	}

	static public void main (String[] args) throws MPJException {

		int N;                            /* problem size */
		int ncol,nrow;                    /* number of rows and columns */
		double Gnew,r,omega,              /* some float values */
		   	   stopdiff;			  	  /* differences btw grid points in iters */
		double[] maxdiff = new double[1];
		double[] diff = new double[1];
		double[][] G;                 	  /* the grid */
		int i,j,phase,iteration;          /* counters */
		double t_start,t_end;             /* time values */
		int size,rank,pred,succ;          /* process ranks */
		int[] lb = new int[1];	          /* lower and upper bound of grid stripe */
		int[] ub = new int[1];
		Status status;		              /* dummy for MPI_Recv */
		int[] remoteLb = new int[1];
		int[] remoteUb = new int[1];
		boolean print = false;

		MPJ.init(args);
		size = MPJ.COMM_WORLD.size();
		rank = MPJ.COMM_WORLD.rank();

		/* ranks of predecessor and successor for row exchanges */
		pred = (rank == 0) ? MPJ.PROC_NULL : rank - 1;
		succ = (rank == size-1) ? MPJ.PROC_NULL : rank + 1;

		/* set up problem size */
		N = 1000;

		for (int l=0; l<args.length; l++) {
			if ((args[l].compareToIgnoreCase("-print")) == 0) {
				print = true;
			}
			else {
				N = Integer.parseInt(args[l]);
			}
		}
		
		if (rank == 0) {
			System.out.println("Running " + N + " x " + N + " SOR");
		}

		N += 2; /* add the two border lines */
		/* finally N*N (from argv) array points will be computed */

		/* set up a quadratic grid */
		ncol = nrow = N;
		r        = 0.5 * ( Math.cos( Math.PI / ncol ) + Math.cos( Math.PI / nrow ) );
		omega    = 2.0 / ( 1 + Math.sqrt( 1 - r * r ) );
		stopdiff = TOLERANCE / ( 2.0 - omega );
		omega   *= MAGIC;

		/* get my stripe bounds and malloc the grid accordingly */
		get_bounds(lb, ub, N-1, size, rank);
	
		G = new double[N][N];

		if(rank == 0) {
			initGrid(G, N);
		}

		/* distribute the data */
		if(size > 1) {
			if(rank == 0) {
				for(i=1; i<size; i++) {
					get_bounds(remoteLb, remoteUb, N-1, size, i);
				
					for(j=remoteLb[0]-1; j<=remoteUb[0]; j++) {
						MPJ.COMM_WORLD.send(G[j], 0, ncol, MPJ.DOUBLE, i, j);
					}
				}
			} else {
				for(i=lb[0]-1; i<=ub[0]; i++) {
					status = MPJ.COMM_WORLD.recv(G[i] , 0, ncol, MPJ.DOUBLE, 0, i);
				}
			}
		}
	
	
		iteration = 0;
		maxdiff[0] = 999.0;

		/* synchronize first for reproducible timings: */
		MPJ.COMM_WORLD.barrier();
		t_start = MPJ.wtime();

		do {
			maxdiff[0] = 0.0;
			for ( phase = 0; phase < 2 ; phase++){
				/* this, and ( (send rec send rec) / (rec send rec send) ) are the *only*
				   correct versions. both sides sending at the same time can lead to deadlock.
				*/
				if(even(rank) == 0) {
					MPJ.COMM_WORLD.send(G[lb[0]], 0, ncol, MPJ.DOUBLE, pred, 42);
					MPJ.COMM_WORLD.send(G[ub[0]-1], 0, ncol, MPJ.DOUBLE, succ, 43);
					status = MPJ.COMM_WORLD.recv(G[ub[0]], 0, ncol, MPJ.DOUBLE, succ, 44);
					status = MPJ.COMM_WORLD.recv(G[lb[0]-1], 0, ncol, MPJ.DOUBLE, pred, 45);
				} else {
					status = MPJ.COMM_WORLD.recv(G[ub[0]], 0, ncol, MPJ.DOUBLE, succ, 42);
					status = MPJ.COMM_WORLD.recv(G[lb[0]-1], 0 , ncol, MPJ.DOUBLE, pred, 43);
					MPJ.COMM_WORLD.send(G[lb[0]], 0, ncol, MPJ.DOUBLE, pred, 44);
					MPJ.COMM_WORLD.send(G[ub[0]-1], 0, ncol, MPJ.DOUBLE, succ,45);
				}
			
				for ( i = lb[0] ; i < ub[0] ; i++ ){
					for ( j = 1 + (even(i) ^ phase); j < ncol-1 ; j += 2 ){
						Gnew = stencil(G,i,j);
						diff[0] = Math.abs(Gnew - G[i][j]);
						if ( diff[0] > maxdiff[0] )
							maxdiff[0] = diff[0];
						G[i][j] = G[i][j] + omega * (Gnew-G[i][j]);
					}
				}
			}
			diff[0] = maxdiff[0];
			MPJ.COMM_WORLD.allreduce(diff, 0, maxdiff, 0, 1, MPJ.DOUBLE, MPJ.MAX);
			
			iteration++;
		} while (maxdiff[0] > stopdiff);

		t_end = MPJ.wtime();

		/* gather the data again */
		if(size > 1) {
			if(rank == 0) {
				for(i=1; i<size; i++) {
					get_bounds(remoteLb, remoteUb, N-1, size, i);
				
					for(j=remoteLb[0]; j<remoteUb[0]; j++) {
						status = MPJ.COMM_WORLD.recv(G[j], 0, ncol, MPJ.DOUBLE, i, j);

					}
				}
			} else {
				for(i=lb[0]; i<ub[0]; i++) {
					MPJ.COMM_WORLD.send(G[i], 0, ncol, MPJ.DOUBLE, 0, i);
				}
			}
		}

		if (rank == 0) {
		
			double diffTime = (t_end - t_start);
			DecimalFormat dc = new DecimalFormat("#####0.000");
		
			System.out.println("SOR took " + dc.format(diffTime) + " seconds");
	
				
			DecimalFormat ft = new DecimalFormat("0.000000");
		
			System.out.println("Used " + iteration + " iterations, diff is " + ft.format(maxdiff[0]) + ", allowed diff is " + ft.format(stopdiff));
		
			if (print) {
				printGrid(G, N);
			}
		}
	
	

		MPJ.finish();
	
	}
}
