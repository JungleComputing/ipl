final class Ida extends ibis.satin.SatinObject implements IdaInterface, java.io.Serializable  {

	static final int
		NSQRT		= 4,
		NPUZZLE		= NSQRT * NSQRT - 1,
	        BRANCH_FACTOR	= 3;

	static void Move(Job j, int dx, int dy, Game puzzle) {
		int x, y, v;

		x = j.blankX + dx;
		y = j.blankY + dy;
		v = j.getVal(x, y);

		j.bound--;
		j.distance += -puzzle.Distance(v, x, y) + puzzle.Distance(v, j.blankX, j.blankY);

		j.setVal(j.blankX, j.blankY, v);
		j.prevDx = dx;
		j.prevDy = dy;
		j.blankX = x;
		j.blankY = y;
	}


	static int MakeMoves(Job[] jobs, Game puzzle) {
		/* Optimization: do not generate (cyclic) moves that undo the last move. */
		int n = 0;

		if(jobs[0].blankX > 1 && jobs[0].prevDx != 1) {
			n++;
			jobs[n] = new Job(jobs[0]);
			Move(jobs[n], -1, 0, puzzle);
		}

		if(jobs[0].blankX < NSQRT && jobs[0].prevDx != -1) {
			n++;
			jobs[n] = new Job(jobs[0]);
			Move(jobs[n], 1, 0, puzzle);
		}

		if(jobs[0].blankY > 1 && jobs[0].prevDy != 1) {
			n++;
			jobs[n] = new Job(jobs[0]);
			Move(jobs[n], 0, -1, puzzle);
		}

		if(jobs[0].blankY < NSQRT && jobs[0].prevDy != -1) {
			n++;
			jobs[n] = new Job(jobs[0]);
			Move(jobs[n], 0, 1, puzzle);
		}

		return n;
	}


	public int Expand(Job job, Game puzzle) {
		int mine, child;
		int solutions = 0;

		/* found a solution? */
		if(job.distance == 0) return 1;

		/* Prune paths with too high estimates. */
		if(job.distance > job.bound) return 0;

		Job[] jobs = new Job[BRANCH_FACTOR+1];
		jobs[0] = job;
		child = MakeMoves(jobs, puzzle);

		int[] solutionsArray = new int[child];

		for(int i=1; i<=child; i++) {
			solutionsArray[i-1] = Expand(jobs[i], puzzle);
		}

		sync();

		for(int i=0; i<child; i++) {
			solutions += solutionsArray[i];
		}

		return solutions;
	}


	public static void main(String argv[]) {
		/* Use a suitable default value. */
		int length = 58;
		long start, stop;
		int v;
		int bound;
		int solutions = 0;
		double time;
		Job j = new Job();
		Game puzzle = new Game();
		Ida ida = new Ida();

		if(argv.length == 1) {
			length = Integer.parseInt(argv[0]);
		} else if(argv.length != 0) {
			System.out.println( "Usage: java Ida [length]");
			System.exit(1);
		}

		puzzle.Init(length);

		/* Initialize starting position. */
		j.prevDx = 0;
		j.prevDy = 0;
		j.distance = 0;
		for(int y=1; y <= NSQRT; y++) {
			for(int x=1; x <= NSQRT; x++) {
				v = puzzle.Value(x, y);
				j.setVal(x, y, v);
				j.distance += puzzle.Distance(v, x, y);
				if(v == 0) {
					j.blankX = x;
					j.blankY = y;
				}

				// if(v < 10) System.out.print("0");
				// System.out.print(v + " ");
			}

			// System.out.println();
		}

		System.out.println("Running ida " + length);

		start = System.currentTimeMillis();
		bound = j.distance;
		System.out.print("Try bound ");
		System.out.flush();
		do {
			System.out.print(bound + " ");
			System.out.flush();
			j.bound = bound;
			solutions = ida.Expand(j, puzzle);
			ida.sync();

			bound += 2; /* Property of 15-puzzle and Manhattan distance */
		} while(solutions == 0);
		stop = System.currentTimeMillis();

		time = (double) stop - start;
		time /= 1000.0;

		System.out.println("\napplication ida (" + length + ") took " + time + 
			" seconds, result = " + solutions + " solutions of " + j.bound + " steps");
	}
}
