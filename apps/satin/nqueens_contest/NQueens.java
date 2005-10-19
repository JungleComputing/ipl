
import java.io.Serializable;
import ibis.satin.SatinObject;

final class NQueens extends SatinObject implements NQueensInterface, Serializable {

    static final long [] solutions = { 0, 
				       0, 
				       0, 
				       0, 
				       2, 
				       10, 
				       4, 
				       40, 
				       92, 
				       352,
				       724,
				       2680,
				       14200,
				       73712,
				       365596,
				       2279184L,
				       14772512L,
				       95815104L,
				       666090624L, 
                                       4968057848L,
				       39029188884L,
				       314666222712L,
				       2691008701644L, 
				       24233937684440L,
				       227514171973736L }; 

    public long spawn_QueenNotInCorner(int[] board, int N, int spawnLevel, int y,
            int left, int down, int right, int mask, int lastmask,
            int sidemask, int bound1, int bound2, int topbit, int endbit) {
        return QueenNotInCorner(board, N, spawnLevel, y, left, down, right, mask,
                lastmask, sidemask, bound1, bound2, topbit, endbit);
    }

    public long spawn_QueenInCorner(int y, int spawnLevel, int left, int down,
            int right, int bound1, int mask, int sizee) {
        return QueenInCorner(y, spawnLevel, left, down, right, bound1, mask, sizee);
    }

    private static final long seq_QueenInCorner(final int y, final int left, final int down,
            final int right, final int bound1, final int mask, final int sizee) {

        int bitmap = mask & ~(left | down | right);

        if (y == sizee) {
            if (bitmap != 0) {
                return 8;
            }
            return 0;
        }

        if (y < bound1) {
            bitmap |= 2;
            bitmap ^= 2;
        }

        long lnsol = 0;

        while (bitmap != 0) {
	    int bit = -bitmap & bitmap;
            bitmap ^= bit;
            lnsol += seq_QueenInCorner(y + 1, (left | bit) * 2, down | bit,
                    (right | bit) / 2, bound1, mask, sizee);
        }

        return lnsol;
    }

    private long QueenInCorner(final int y, final int spawnLevel, final int left,
			       final int down, final int right, final int bound1, final int mask,
			       final int sizee) {

        int bitmap = mask & ~(left | down | right);

        if (y == sizee) {
            if (bitmap != 0) {
                return 8;
            }
            return 0;
        }

        if (y < bound1) {
            bitmap |= 2;
            bitmap ^= 2;
        }

	// Check if we've gone deep enough into the recursion to 
	// have generated a decent number of jobs. If so, stop spawning
	// and switch to a sequential algorithm...
        if (y > spawnLevel) {

	    long lnsol = 0;

            while (bitmap != 0) {
		final int bit = -bitmap & bitmap;
                bitmap ^= bit;
                lnsol += seq_QueenInCorner(y + 1, (left | bit) * 2, down | bit,
					   (right | bit) / 2, bound1, mask, sizee);
            }

            return lnsol;
        }

	// If where not deep enough, we keep spawning.
        long[] lnsols = new long[(sizee + 1) * (sizee + 1)];
        int it = 0;

        while (bitmap != 0) {
	    final int bit = -bitmap & bitmap;
            bitmap ^= bit;
            lnsols[it] = spawn_QueenInCorner(y + 1, spawnLevel, (left | bit) * 2,
					       down | bit, (right | bit) / 2, bound1, mask, sizee);
	    it++;
	}

	// Wait for all the result to be returned.
        sync();

	// Determine the sum of the solutions.
        long lnsol = 0;

        for (int i = 0; i< it; i++) { 
            lnsol += lnsols[i];
        }

        return lnsol;
    }

    private static final long seq_QueenNotInCorner(final int[] board, final int N, final int y,
						   final int left, final int down, final int right, final int mask,
						   final int lastmask, final int sidemask, final int bound1,
						   final int bound2, final int topbit, final int endbit) {
	long lnsol = 0;

        int bitmap = mask & ~(left | down | right);

	// Check if we have reached the end of the board. If so, 
	// we check the number of solution this board represents.
        if (y == N - 1) {
	    if (bitmap != 0) {
                if ((bitmap & lastmask) == 0) {
                    board[y] = bitmap;
                    lnsol += Check(board, N, board[bound1], board[bound2], topbit, endbit);
                    board[y] = 0;
                }
            }

            return lnsol;
        }

        if (y < bound1) {
            bitmap |= sidemask;
            bitmap ^= sidemask;
        } else if (y == bound2) {
            if ((down & sidemask) == 0)
                return 0; //lnsol;
            if ((down & sidemask) != sidemask)
                bitmap &= sidemask;
        }

	// Where not done, so recursively compute the rest of the solutions...
	while (bitmap != 0) {
	    final int bit = -bitmap & bitmap;
	    board[y] = bit;
	    bitmap ^= bit;
	    lnsol += seq_QueenNotInCorner(board, N, y + 1, (left | bit) << 1, down
				    | bit, (right | bit) >> 1, mask, lastmask, sidemask, bound1,
				    bound2, topbit, endbit);
	}

	return lnsol;
    }

    private long QueenNotInCorner(final int[] board, final int N,
				  final int spawnLevel, final int y, final int left, final int down,
				  final int right, final int mask, final int lastmask,
				  final int sidemask, final int bound1, final int bound2,
				  final int topbit, final int endbit) {

        int bitmap = mask & ~(left | down | right);

        if (y < bound1) {
            bitmap |= sidemask;
            bitmap ^= sidemask;
        } else if (y == bound2) {
            if ((down & sidemask) == 0)
                return 0; //lnsol;
            if ((down & sidemask) != sidemask)
                bitmap &= sidemask;
        }

	// Check if we've gone deep enough into the recursion to 
	// have generated a decent number of jobs. If so, stop spawining
	// and switch to a sequential algorithm...
        if (y > spawnLevel) {
	    long lnsol = 0;

            while (bitmap != 0) {
                int bit = -bitmap & bitmap;
                board[y] = bit;
                bitmap ^= bit;
                lnsol += seq_QueenNotInCorner(board, N, y + 1, (left | bit) << 1, down
                        | bit, (right | bit) >> 1, mask, lastmask, sidemask,
                        bound1, bound2, topbit, endbit);
            }

            return lnsol;
        }

	// If where not deep enough, we keep spawning.
        int it = 0;
        long [] lnsols = new long[N * N];
	
        while (bitmap != 0) {
            int [] boardClone = (int[]) board.clone();
            int bit = -bitmap & bitmap;
            boardClone[y] = bit;
            bitmap ^= bit;
            lnsols[it] = spawn_QueenNotInCorner(boardClone, N, spawnLevel, y + 1,
						  (left | bit) * 2, down | bit, (right | bit) / 2, mask,
						  lastmask, sidemask, bound1, bound2, topbit, endbit);
	    it++;
        }

	// Wait for all the result to be returned.
        sync();

	// Determine the sum of the solutions
        long lnsol = 0;

        for (int i=0;i<it;i++) { 
            lnsol += lnsols[i];
        }

        return lnsol;
    }

    private static final long Check(final int[] board, final int N, final int board1,
            final int board2, final int topbit, final int endbit) {

        // 90-degree rotation
        if (board2 == 1) {
	    int own = 1;

            for (int ptn = 2, bit = 1; own < N; own++, ptn <<= 1, bit = 1) {

                for (int you = N - 1; board[you] != ptn && board[own] >= bit; you--, bit <<= 1) { } 
               
                if (board[own] > bit)
                    return 0;
                if (board[own] < bit)
                    break;
            }
            if (own > N - 1) {
                return 2;
            }
        }

        // 180-degree rotation
        if (board[N - 1] == endbit) {

	    int own = 1;

            for (int you = N - 2, bit = 1; own < N; own++, you--, bit = 1) {

                for (int ptn = topbit; ptn != board[you] && board[own] >= bit; ptn >>= 1, bit <<= 1) { }
                  
                if (board[own] > bit)
                    return 0;
                if (board[own] < bit)
                    break;
            }
            if (own > N - 1) {
                return 4;
            }
        }

        // 270-degree rotation 
        if (board1 == topbit) {
            for (int ptn = topbit >> 1, own = 1, bit = 1; own < N; own++, ptn >>= 1, bit = 1) {

                for (int you = 0; board[you] != ptn && board[own] >= bit; you++, bit <<= 1) { } 
                    
                if (board[own] > bit)
                    return 0;
                if (board[own] < bit)
                    break;
            }
        }

        return 8;
    }

    private final long calculate(final long [] results, final int [] bounds, final int size, final int spawnLevel) { 

	final int SIZEE = size - 1;
	final int TOPBIT = 1 << SIZEE;
	final int MASK = (1 << size) - 1;

	int nextResult = 0;

	long start = System.currentTimeMillis();

	for (int i=0;i<bounds.length;i++) {
	    
	    final int SELECTED_BOUND = bounds[i];

	    if (SELECTED_BOUND == 0) { 
		
		for (int BOUND1=2; BOUND1 < SIZEE; BOUND1++) {
		    
		    int bit = 1 << BOUND1;
		    results[nextResult++] = spawn_QueenInCorner(2, spawnLevel,
							       (2 | bit) * 2, 1 | bit, bit / 2, BOUND1, MASK,
							       SIZEE);
		}

	    } else { 
		if (SELECTED_BOUND > 0 && SELECTED_BOUND <= SIZEE/2 && SELECTED_BOUND <= SIZEE-SELECTED_BOUND) { 
		
		    final int BOUND1 = SELECTED_BOUND;
		    final int BOUND2 = SIZEE-BOUND1;

		    final int bit = 1 << BOUND1;
	    
		    int LASTMASK = TOPBIT | 1;
		    final int SIDEMASK = LASTMASK;
		    int ENDBIT = TOPBIT / 2;
		
		    final int [] board = new int[size];
		    board[0] = bit;
		
		    for (int b1 = 1; b1 < BOUND1; b1++) {
			LASTMASK |= LASTMASK >> 1 | LASTMASK << 1;
			ENDBIT /= 2;
		    } 
		
		    results[nextResult++] = spawn_QueenNotInCorner(board, size,
								   spawnLevel, 1, bit * 2, bit, bit / 2, MASK,
								   LASTMASK, SIDEMASK, BOUND1, BOUND2, TOPBIT, ENDBIT);
		} else { 
		    System.out.println("WARNING: skipped " + SELECTED_BOUND);
		}
	    } 
	}

	sync();

	long end = System.currentTimeMillis();

	//printResults(results, nextResult, size, (end-start) / 1000.0);

	return (end-start);
    }

    private static void printResults(long [] results, int size, double time, boolean verbose) { 

        long nsol = 0;

	for (int i = 0; i < results.length; i++) {
	    nsol += results[i];
	}

	time = time / 1000.0;

        System.out.println(" application result nqueens (" + size + ") = " + nsol + "  time = " + time + " s. ");

	if (verbose) { 
	    for (int i = 0; i < (size-2 + (size-1)/2 - 1); i++) {
		System.out.println("result[ " + i + "] = " + results[i] + " (" + ((100*results[i])/solutions[size]) + "%)");
	    }
	   
	    System.out.println(" application result nqueens (" + size + ") = " + nsol);

	    if (size < solutions.length) { 
		if (nsol == solutions[size]) { 
		    System.out.println(" application result is OK");
		} else { 
		    System.out.println(" application result is NOT COMPLETE OR WRONG!");
		} 
	    } 
	}
    } 

    public static void main(String[] args) {

	if (args.length < 2) { 
            System.out.println(" usage: nqueens repeat size spawnLevel [ list of bounds to compute ]");
            System.exit(1);
	}

	/* description of initial configuration */
	int repeat = Integer.parseInt(args[0]);
	int size = Integer.parseInt(args[1]);
	int spawnLevel = Integer.parseInt(args[2]);
	
	int [] bounds = null;

	if (args.length > 3) { 
	    bounds = new int[args.length-3];
	    
	    for (int i=0;i<args.length-3;i++) { 
		bounds[i] = Integer.parseInt(args[i+3]);

		if (bounds[i] > (size/2)) { 
		    System.out.println("Illegal bounds " + bounds[i]);
		    System.exit(1);
		} 
	    }
	} else { 
	    bounds = new int[size/2];

	    for (int i=0;i<bounds.length;i++) { 
		bounds[i] = i;
	    } 
	} 

	System.out.print(" nqueens " + size + " started with spawn level " + spawnLevel);

	if (size < solutions.length) { 
	    System.out.println(" (solution should be " + solutions[size] + ")");
	} else { 
	    System.out.println(" (solution is unknown)");
	}

	NQueens nq = new NQueens();
	final long results [] = new long[size*3];

	for (int i=0;i<repeat;i++) { 
	    double time = nq.calculate(results, bounds, size, spawnLevel);
	    printResults(results, size, time, i == repeat-1); 	    
	} 
    } 

}
