public final class NQueens extends ibis.satin.SatinObject implements NQueensInterface, java.io.Serializable  {

	// Boards are represented as arrays where each cell 
	// holds the column number of the queen in that row
	// performance hack: use arrays of size 'size' only, so the gc can cache them all.
	
	public void nqueens(byte[] sofar, int row, int size) throws Abort {
		if (row >= size) { // done
			throw new Abort(sofar);
		}

	tryNewRow:  for (int q = 0; q < size; q++) {
			// Check if can place queen in column q of next row
			for (int i = 0; i < row; i++) {
				int p = sofar[i];
				if (q == p || q == p - (row - i) || q == p + (row - i)) {
					continue tryNewRow;
				}
			}

			// Fork to explore moves from new configuration
			byte[] next = new byte[size];
			System.arraycopy(sofar, 0, next, 0, row);
			next[row] = (byte) q;
			nqueens(next, row+1, size);
		}

		sync();
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
		
		byte[] board = null;
		start = System.currentTimeMillis();
		try {
			nq.nqueens(new byte[size], 0, size);
			nq.sync();
		} catch (Abort a) {
			board = a.result;
		}
		end = System.currentTimeMillis();
		
		System.out.print("Result:");
		
		for (int i = 0; i < board.length; ++i) {
			System.out.print(" " + board[i]);
		}
		System.out.println();

		time = end - start;
		time /= 1000.0;

		System.out.println("application nqueens (" + size + ") took " + time + " seconds");
	}
}
