/* $Id$ */

public final class NQueens extends ibis.satin.SatinObject implements
        NQueensInterface, java.io.Serializable {

    static final int THRESHOLD = 400000;

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
    public void spawn_nqueens(byte[] sofar, int row, int size) throws Abort {
        nqueens(sofar, row, size);
    }

    public void nqueens(byte[] sofar, int row, int size) throws Abort {
        Abort result = null;

        if (row >= size) { // done
            System.out.println("found solution");
            throw new Abort(sofar);
        }

        tryNewRow: for (int q = 0; q < size; q++) {
            // Check if can place queen in column q of next row
            for (int i = 0; i < row; i++) {
                int p = sofar[i] - q;
                if (p == 0 || p == (row - i) || p == -(row - i)) {
                    continue tryNewRow;
                }
            }

            // Fork to explore moves from new configuration
            if (row < THRESHOLD) {
                byte[] next = new byte[size];
                System.arraycopy(sofar, 0, next, 0, row);
                next[row] = (byte) q;
                try {
                    spawn_nqueens(next, row + 1, size);
                } catch (Abort a) {
                    abort();
                    result = a;
                    return;
                }
            } else {
                // don't spawn anymore
                byte[] res;
                sofar[row] = (byte) q;
                nqueens(sofar, row + 1, size);
            }
        }

        if (row < THRESHOLD) {
            sync();
        }

        if (result != null) {
            System.out.println("NQUEENS: rethrowing result");
            throw result;
        }
    }

    public boolean checkResult(byte[] board) {
        boolean found;

        for (int row = 0; row < board.length; row++) {
            found = false;
            tryNewRow: for (int q = 0; q < board.length; q++) {
                // Check if can place queen in column q of next row
                for (int i = 0; i < row; i++) {
                    int p = board[i] - q;
                    if (p == 0 || p == (row - i) || p == -(row - i)) {
                        continue tryNewRow;
                    }
                }
                found = true;
            }

            if (!found)
                return false;
        }

        return true;
    }

    public static void main(String[] args) {
        int option;
        int size = 10;
        long start, end = 0;
        double time = 0.0;
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

        if (option > 1) {
            System.err.println("To many options, usage nqueens [board size]");
            System.exit(1);
        }

        System.out.println("nqueens " + size + " started");

        if (size <= 3) {
            System.out.println("There is no solution for board size <= 3");
            System.exit(66);
        }

        start = System.currentTimeMillis();
        try {
            nq.spawn_nqueens(new byte[size], 0, size);
        } catch (Abort a) {
            nq.abort();
            end = System.currentTimeMillis();

            if (!nq.checkResult(a.result)) {
                System.out.println("application nqueens (" + size
                        + ") gave WRONG RESULT");
                System.exit(1);
            }

            System.out.print("Result:");

            for (int i = 0; i < a.result.length; ++i) {
                System.out.print(" " + a.result[i]);
            }
            System.out.println();

            time = end - start;
            time /= 1000.0;

            System.out.println("application nqueens (" + size + ") took "
                    + time + " seconds");
        }
        nq.sync();
    }
}