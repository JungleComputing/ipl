/* $Id$ */

final class Ida extends ibis.satin.SatinObject implements IdaInterface,
        java.io.Serializable {

    static final int NSQRT = 4, NPUZZLE = NSQRT * NSQRT - 1, BRANCH_FACTOR = 4, // first move may be in four directions!
            THRESHOLD = 12; // Spawn the top n levels. Higher is more spawns.

    static void Move(Job j, int dx, int dy, Game puzzle) {
        int x, y, v;

        x = j.blankX + dx;
        y = j.blankY + dy;
        v = j.getVal(x, y);

        j.bound--;
        j.distance += -puzzle.distance(v, x, y)
                + puzzle.distance(v, j.blankX, j.blankY);

        j.setVal(j.blankX, j.blankY, v);
        j.prevDx = dx;
        j.prevDy = dy;
        j.blankX = x;
        j.blankY = y;
    }

    static int MakeMoves(Job[] jobs, Game puzzle) {
        /* Optimization: do not generate (cyclic) moves that undo the last move. */
        int n = 0;

        Job j = jobs[0];
        if (j.blankX > 1 && j.prevDx != 1) {
            n++;
            jobs[n] = new Job(jobs[0]);
            Move(jobs[n], -1, 0, puzzle);
        }

        if (j.blankX < NSQRT && j.prevDx != -1) {
            n++;
            jobs[n] = new Job(jobs[0]);
            Move(jobs[n], 1, 0, puzzle);
        }

        if (j.blankY > 1 && j.prevDy != 1) {
            n++;
            jobs[n] = new Job(jobs[0]);
            Move(jobs[n], 0, -1, puzzle);
        }

        if (j.blankY < NSQRT && j.prevDy != -1) {
            n++;
            jobs[n] = new Job(jobs[0]);
            Move(jobs[n], 0, 1, puzzle);
        }

        return n;
    }

    public int spawn_Expand(Job job, Game puzzle) {
        return Expand(job, puzzle);
    }

    public int Expand(Job job, Game puzzle) {
        int mine, child;
        int solutions = 0;

        /* found a solution? */
        if (job.distance == 0)
            return 1;

        /* Prune paths with too high estimates. */
        if (job.distance > job.bound)
            return 0;

        Job[] jobs = new Job[BRANCH_FACTOR + 1];
        jobs[0] = job;
        child = MakeMoves(jobs, puzzle);

        if (job.origBound - job.bound > THRESHOLD) {
            for (int i = 1; i <= child; i++) {
                solutions += Expand(jobs[i], puzzle);
            }
        } else {
            int[] solutionsArray = new int[child];

            for (int i = 1; i <= child; i++) {
                solutionsArray[i - 1] = spawn_Expand(jobs[i], puzzle);
            }

            sync();

            for (int i = 0; i < child; i++) {
                solutions += solutionsArray[i];
            }
        }

        return solutions;
    }

    public static void main(String args[]) {
        /* Use a suitable default value. */
        int length = 54;
        long start, stop;
        int v;
        int bound;
        int solutions = 0;
        double time;
        Job j = new Job();
        Game puzzle = new Game();
        Ida ida = new Ida();
        int option = 0;
        String fileName = null;
        int maxDepth = -1;

        for (int i = 0; i < args.length; i++) {
            if (false) {
            } else if (args[i].equals("-f")) {
                fileName = args[++i];
            } else if (args[i].equals("-max")) {
                maxDepth = Integer.parseInt(args[++i]);
            } else if (option == 0) {
                length = Integer.parseInt(args[i]);
                option++;
            } else {
                System.err.println("No such option: " + args[i]);
                System.exit(1);
            }
        }

        if (option > 1) {
            System.err
                    .println("To many options, usage Ida [-f <file>] [length]");
            System.exit(1);
        }

        if (fileName == null) {
            puzzle.init(length);
        } else {
            puzzle.init(fileName);
        }

        /* Initialize starting position. */
        j.prevDx = 0;
        j.prevDy = 0;
        j.distance = 0;
        for (int y = 1; y <= NSQRT; y++) {
            for (int x = 1; x <= NSQRT; x++) {
                v = puzzle.value(x, y);
                j.setVal(x, y, v);
                j.distance += puzzle.distance(v, x, y);
                if (v == 0) {
                    j.blankX = x;
                    j.blankY = y;
                }
            }
        }

        if (fileName == null) {
            System.out.println("Running ida " + length);
        } else {
            System.out.println("Running ida on " + fileName);
        }

        start = System.currentTimeMillis();
        bound = j.distance;
        j.print();
        System.out.print("Try bound ");
        System.out.flush();
        do {
            System.out.print(bound + " ");
            System.out.flush();
            j.bound = bound;
            j.origBound = bound;
            solutions = ida.Expand(j, puzzle);
            ida.sync();

            bound += 2; /* Property of 15-puzzle and Manhattan distance */
            if (maxDepth != -1 && bound > maxDepth)
                break;
        } while (solutions == 0);
        stop = System.currentTimeMillis();

        time = (double) stop - start;
        time /= 1000.0;

        System.out.println("\napplication time ida (" + fileName + ","
                + maxDepth + ") took " + time + " s");
        System.out.println("\napplication result ida (" + fileName + ","
                + maxDepth + ") = " + solutions + " solutions of " + j.bound
                + " steps");
        if (args.length == 0) {
            if (j.bound != 54 || solutions != 1) {
                System.out.println("Test failed!");
                System.exit(1);
            } else {
                System.out.println("Test succeeded!");
            }
        }
    }
}