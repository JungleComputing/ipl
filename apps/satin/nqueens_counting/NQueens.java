public final class NQueens extends ibis.satin.SatinObject implements NQueensInterface, java.io.Serializable  {

	static final int THRESHOLD = 4;

	
	/** 
	 * We need a global variable holding the result of search.
	 * Check this to see if it is nonnull,
	 * if so, returning early because a result has been found.
	 * In a more serious program, we might use a fancier scheme
	 * to reduce read/write pressure on this variable.
	 **/
	
	
	// Boards are represented as arrays where each cell 
	// holds the column number of the queen in that row
	// performance hack: use arrays of size 'size' only, so the gc can cache them all.
        // return value is numer of solutions found.
	
	public long spawn_nqueens(byte[] sofar, int row, int size) {
		return nqueens(sofar, row, size);
	}

	public long nqueens(byte[] sofar, int row, int size) {
	    long solutions = 0;
	    long[] results = null;

		if (row >= size) { // done
			return 1;
		}

                if (row < THRESHOLD) {
                        results = new long[size];
                }

	        tryNewRow: for (int q = 0; q < size; q++) {
			// Check if can place queen in column q of next row
			for (int i = 0; i < row; i++) {
				int p = sofar[i]-q;
				if (p == 0 || p == (row - i) || p == -(row - i)) {
					continue tryNewRow;
				}
			}

			// Fork to explore moves from new configuration
			if (row < THRESHOLD) {
//				byte[] next = new byte[size];
//				System.arraycopy(sofar, 0, next, 0, row);
			    byte[] next = (byte[])sofar.clone();
				next[row] = (byte) q;
				results[q] = spawn_nqueens(next, row+1, size);
			} else {
				// don't spawn anymore
				byte[] res;
				sofar[row] = (byte) q;
				solutions += nqueens(sofar, row+1, size);
			}
		}

		if(row < THRESHOLD) {
			sync();
			for (int q = 0; q < size; q++) {
				solutions += results[q];
			}
		}

		return solutions;
	}

	public boolean checkResult(byte[] board) {
		boolean found;

		for(int row=0; row<board.length; row++) {
			found = false;
                        tryNewRow: for (int q = 0; q <board.length; q++) {
				// Check if can place queen in column q of next row
				for (int i = 0; i < row; i++) {
					int p = board[i]-q;
					if (p == 0 || p == (row - i) || p == -(row - i)) {
						continue tryNewRow;
					}
				}
				found = true;
			}

			if(!found) return false;
		}

		return true;
	}

	public static void main(String[] args) {
		
		int option;
		int size = 10;
		long start, end;
		double time;
		NQueens nq = new NQueens();

		option = 0;
		for (int i = 0; i < args.length; i++) {
			if (false) {
			} else if (option == 0) {
				size = Integer.parseInt(args[i]);
				option++;
			} else {
				System.err.println("No such option: " + args[i]);
				System.exit(1);
			}
		}
		
		if(option > 1) {
			System.err.println("To many options, usage nqueens [board size]");
			System.exit(1);
		}
		
		System.out.println("nqueens " + size + " started");
		
		if (size <= 3) {
			System.out.println("There is no solution for board size <= 3");
			System.exit(66);
		}
		
		long sols = 0;
		start = System.currentTimeMillis();
		sols = nq.spawn_nqueens(new byte[size], 0, size);
		nq.sync();
		end = System.currentTimeMillis();
		
		System.out.println("application result nqueens (" + size + ") = " + sols);

		time = end - start;
		time /= 1000.0;

		System.out.println("application time nqueens (" + size + ") took " + time + " s");
	}
}
