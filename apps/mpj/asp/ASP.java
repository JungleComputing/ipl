/**************************************************************************
* MPJ version of All pair shortest Path (ASP):                                                          *
* Markus Bornemann                                                        * 
* Vrije Universiteit Amsterdam Department of Computer Science             *
* 26/01/2005                                                              *
**************************************************************************/

import java.text.DecimalFormat;
import java.util.Random;
import ibis.mpj.*;

public class ASP {
	private static int MAX_DISTANCE = 42;
	
	private static void init_tab(int n, int[][] tab) {
		Random rnd = new Random(132L);
		
		
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				if (j==i) tab[i][j] = 0;
				else tab [i][j] = 1 + rnd.nextInt(MAX_DISTANCE);
			}
		}
	}
	
	private static void print_tab(int[][] tab, int n) {
		DecimalFormat dc = new DecimalFormat("00");
		for (int i=0; i<n; i++) {
			for (int j=0; j<n; j++) {
				System.out.print(dc.format(tab[i][j]) + " ");
			}
			System.out.println();
		}
	}
	
	private static int min (int v1, int v2) {
		if(v1 < v2) {
			return v1;
		}
		else {
			return v2;
		}
	}
	
	static private void iterRange(int lowestValue, int highestValue, int numProcs, int rank, int[] startIter, int[] stopIter) {
		int temp1 = (highestValue - lowestValue+1) / numProcs;
		int temp2 = (highestValue - lowestValue+1) % numProcs;
		
		startIter[0] = rank * temp1 + lowestValue + min(rank, temp2);
		stopIter[0] = startIter[0] + temp1 -1;
		if(temp2 > rank) stopIter[0] = stopIter[0] +1;
		
		
	}
	
	private static int getOwner (int k, int n, int size) {
		return k/(n/size);
	}
	
	private static void do_asp(int[][] tab, int n, int rank, int size, int myStart, int myStop) {
		int[] tmpRow = new int[n];
		int[] curRow;
		
		
		for (int k = 0; k < n; k++) {
			int owner = getOwner(k, n, size);
			
			if (rank == owner) {
				curRow = tab[k];
			}
			else {
				curRow = tmpRow;
			}
			
			try {
				MPJ.COMM_WORLD.bcast(curRow, 0, n, MPJ.INT, owner);
			}
			catch(MPJException e) {
				System.err.println(e.getMessage());
				System.exit(-1);
			}
		
			for (int i=myStart; i<=myStop; i++) {
				if (i != k) {
					for (int j = 0; j < n; j++) {
						int tmp = tab[i][k] + curRow[j];
						if (tmp < tab[i][j]) {
							tab[i][j] = tmp;
						}
					}
				}
			}
			
		}
	
	
	}
	
	
	
	public static void main(String[] args) {
		int n = 4000;
		boolean print = false;
		int rank = -1;
		int size = -1;
		
		try {
			MPJ.init(args);
			rank = MPJ.COMM_WORLD.rank();
			size = MPJ.COMM_WORLD.size();
		}
		catch (MPJException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			System.exit(1);
			
		}
		for (int i=0; i<args.length; i++) {
			if ((args[i].compareToIgnoreCase("-print")) == 0) {
				print = true;
			}
			else {
				n = Integer.parseInt(args[i]);
			}
		}
		
		if (rank == 0) {
			System.err.println("Runnning ASP with " + n + " rows");
		}
		int[][] tab = new int[n][n];
		int[][] resTab = new int[n][n];
		init_tab(n, tab);
		init_tab(n, resTab);
		
		int[] myStart = new int[1];
		int[] myStop = new int[1];
		iterRange(0, n-1, size, rank, myStart, myStop);
	
		long begTime = 0;
		long endTime = 0;
		
		if (rank == 0) {
			
			begTime = System.currentTimeMillis();
		}
		do_asp(tab, n, rank, size, myStart[0], myStop[0]);
		if (rank ==  0) {
			endTime = System.currentTimeMillis();
		}
		
		
		
		
		if (rank == 0) {
			double time = (endTime - begTime) / 1000;
		
			DecimalFormat dc = new DecimalFormat("#0.000");
			System.err.println("ASP took " + dc.format(time) + " seconds.");
		}
		
		for (int i = 0; i < n; i++) {
		
			try {
				MPJ.COMM_WORLD.reduce(tab[i], 0, resTab[i], 0, n, MPJ.INT, MPJ.MIN, 0);
			} 
			catch (MPJException e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
			}
		}
		
		
		
		if ((rank==0) && print) print_tab(resTab, n);
		
		
		try {
			MPJ.finish();
		}
		catch (MPJException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}
	
}
