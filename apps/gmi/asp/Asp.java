/*
 * Asp.java:
 * 	All-pairs shortest path implementation based on Floyd's
 * 	algorithms. The distance matrix's rows are block-striped
 * 	across all processors. This allows for pipelining, but leads
 * 	to flow-control problems.
 *
 */

import ibis.gmi.GroupMember;

class Asp extends GroupMember implements i_Asp {

    int[][] tab;
    int nodes, nrClusters, rank, cluster, n;
    int lb, ub;

    boolean print_result;

    i_Asp group;

    Asp(int n, int rank, int nodes, boolean print_result) {
	
	this.n = n;
	
	this.nodes = nodes;
	this.rank  = rank;
	this.print_result    = print_result;
	
	get_bounds();
	init_tab();
    }
    
    public void init(i_Asp group) { 
	this.group = group;
    } 

    public synchronized void transfer(int[] row, int k) {
	tab[k] = row;
	notifyAll();
    }

    public void done() { 
	// dummy function -> use a combined call as a barrier
    } 
    
    void get_bounds() {
    
	int nlarge, nsmall;
	int size_large, size_small;
	
	nlarge = n % nodes;
	nsmall = nodes - nlarge;
	
	size_small = n / nodes;
	size_large = size_small + 1;
	
	if (rank < nlarge) {            /* I'll  have a large chunk */
	    lb = rank * size_large;
	    ub = lb + size_large;
	} else {
	    lb = nlarge * size_large + (rank - nlarge) * size_small;
	    ub = lb + size_small;
	}
    }
    
    int owner(int k) {
	int nlarge, nsmall;
	int size_large, size_small;
	
	nlarge = n % nodes;
	nsmall = nodes - nlarge;
	size_small = n / nodes;
	size_large = size_small + 1;
	
	if ( k < (size_large * nlarge) ){
	    return (k / size_large);
	} else {
	    return (nlarge + (k-(size_large * nlarge))/size_small);
	}
    }
    
    
    void init_tab() {
	int i, j;
	
	tab = new int[n][n];
	OrcaRandom r = new OrcaRandom();
	
	for (i = 0; i < n; i++) {
	    if (lb <= i && i < ub) {   /* my partition */
		for (j = 0; j < n; j++) {
		    tab[i][j] = (i == j ? 0 : r.val() % 1000);
		}
	    } else {
		tab[i] = null;
	    }
	}
    }
    
    void bcast(int k, int owner) { 
	if (rank == owner) { 
	    group.transfer(tab[k], k);
	} else { 
	    synchronized (this) { 
		while (tab[k] == null) { 
		    try { 
			wait();
		    } catch (InterruptedException e) { 
			e.printStackTrace();
		    } 
		}		   
	    } 
	} 
    } 

    void do_asp() {
	int i, j, k, tmp, nrows;
	
	nrows = ub - lb;
	
	for (k = 0; k < n; k++) {
//		if (rank == 0&& (k%100 == 0)) { 
//			System.out.println("" + k);	
//		} 

	    bcast(k, owner(k)); // Owner of k sends tab[k] to al others, 
	                        // which receive it in tab[k].
	    for (i = lb; i < ub; i++) {
		if (i != k) {
		    for (j = 0; j < n; j++) {
			tmp = tab[i][k] + tab[k][j];
			if (tmp < tab[i][j]) {
			    tab[i][j] = tmp;
			}
		    }
		}
	    }
	}
    }
    
    
    void print_table() {
	for(int i=0; i<n; i++) {
	    for(int j=0; j<n; j++) {
		System.out.print(tab[i][j] + " ");
	    }
	    System.out.println();
	}
    }
    
    public void start() {
	long start,end;
	
	if (rank == 0 ) {
	    System.out.println("Asp started, n = " + n);
	}
	
	start = System.currentTimeMillis();
	
	do_asp();
	group.done();
	
	end = System.currentTimeMillis();

	double time = end - start;
	
	if (rank == 0) {
	    System.out.println("\nAsp/gmi took " + (time/1000.0) + " secs.");
	    
//	    if(print_result) print_table();
//	    info.printTime("Asp, " + n + "x" + n, end-start);
	}

    }
}
