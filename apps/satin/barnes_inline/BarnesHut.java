/* $Id$ */

//import javax.swing.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.JFrame;

final class BarnesHut {

	static boolean viz = true;

	static boolean debug = false; //use -(no)debug to modify

	static boolean verbose = false; //use -v to turn on

	static final boolean ASSERTS = false; //also used in other barnes classes

	private static final int IMPL_NTC = 1; // -ntc option

	private static final int IMPL_TUPLE = 2; // -tuple option

	private static final int IMPL_TUPLE2 = 3; // -tuple2 option

	private static final int IMPL_SEQ = 4; // -seq option

	private static int impl = IMPL_NTC;

	//recursion depth at which the ntc/tuple impl work sequentially
	private static int spawn_threshold = 15; //use -t <threshold> to modify

	//true: Collect statistics about the various phases in each iteration
	public static boolean phase_timing = true; //use -(no)timing to modify

	private long totalTime = 0;

	private long btcomTime = 0, updateTime = 0;

	private long[] forceCalcTimes;

	//Parameters for the BarnesHut algorithm / simulation
	private static final double THETA = 5; //cell subdivision tolerance

	//private static final double DT = 0.025; // default integration time-step
	private static final double DT = 10.5; //integration time-step

	//we do 7 iterations (first one isn't measured)
	static final double START_TIME = 0.0;

	static final double END_TIME = 0.175;

	static int iterations = -1;

	private static Body[] bodyArray;

	private static int maxLeafBodies;

	static BodyTreeNode root; //is accessed by BodyTreeNode

	//Indicates if we are the root divide-and-conquer node
	static boolean I_AM_ROOT = false;

	BarnesHut(int n, int m) {
		I_AM_ROOT = true; //constructor is only called at the root node

		if (impl != IMPL_TUPLE2) {
			initialize(n, m);
		} else {
			/*
			 * the tuple2 impl does the initialization at every node when the
			 * first tuple is broadcast
			 */
			Initializer init = new Initializer(n, m);
			ibis.satin.SatinTupleSpace.add("init", init);
		}

		/*
		 * The RMI version contained magic code equivalent to this: (the
		 * DEFAULT_* variables were different)
		 * 
		 * double scale = Math.pow( nBodies / 16384.0, -0.25 ); DT = DEFAULT_DT *
		 * scale; END_TIME = DEFAULT_END_TIME * scale; THETA = DEFAULT_THETA /
		 * scale;
		 * 
		 * Since Rutger didn't know where it came from, and barnes from splash2
		 * also doesn't include this code, I will omit it. - Maik.
		 */

		if (iterations == -1) {
			iterations = (int) ((END_TIME + 0.1 * DT - START_TIME) / DT);
		}

		forceCalcTimes = new long[iterations];
	}

	static void initialize(int nBodies, int mlb) {
		bodyArray = new Plummer().generate(nBodies);
		maxLeafBodies = mlb;

		//Plummer should make sure that a body with number x also has index x
		for (int i = 0; i < nBodies; i++) {
			if (ASSERTS && bodyArray[i].number != i) {
				System.err.println("EEK! Plummer generated an "
						+ "inconsistent body number");
				System.exit(1);
			}
		}
	}

	/*
	 * Builds the tree with bodies using bodyArray, maxLeafBodies and THETA, and
	 * does the center-of-mass computation for this new tree
	 */
	static void buildTreeAndDoCoM() {
		root = null; //prevent Out-Of-Memory because 2 trees are in mem
		root = new BodyTreeNode(bodyArray, maxLeafBodies, THETA);
		root.computeCentersOfMass();
	}

	static void updateBodies(double[] accs_x, double[] accs_y, double[] accs_z,
			int iteration) {
		int i;

		for (i = 0; i < bodyArray.length; i++) {
			//the updated-bit only gets set at the root node
			//with tuple2, this assertion failed at a non-root-node...
			if (ASSERTS && !bodyArray[i].updated && I_AM_ROOT) {
				System.err.println("EEK! Body " + i + " wasn't updated!");
				System.exit(1);
			}
			if (ASSERTS && accs_x[i] > 1.0E4) { //This shouldn't happen
				System.err.println("EEK! Acc_x too large for body #" + i
						+ " in iteration: " + iteration);
				System.err.println("acc = " + accs_x[i]);
				System.exit(1);
			}

			bodyArray[i].computeNewPosition(iteration != 0, DT, accs_x[i],
					accs_y[i], accs_z[i]);

			if (ASSERTS)
				bodyArray[i].updated = false;
		}
	}

	void runSim() {
		int i, iteration;
		long start = 0, end, phaseStart = 0, phaseEnd;

		String rootId = null; //tupleSpace key
		BodyTreeNode dummyNode = new BodyTreeNode(); //used to spawn jobs
		LinkedList result = null;
		double[] accs_x = new double[bodyArray.length];
		double[] accs_y = new double[bodyArray.length];
		double[] accs_z = new double[bodyArray.length];

		BodyCanvas bc = null;

		// print the starting problem
		// printBodies();

		if (viz) {
			bc = visualize();
		}

		ibis.satin.SatinObject.pause(); //turn off satin during sequential
		// parts

		start = System.currentTimeMillis();

		for (iteration = 0; iteration < iterations; iteration++) {
			long btcomTimeTmp = 0, updateTimeTmp = 0;
			long forceCalcTimeTmp = 0;

			//			System.out.println("Starting iteration " + iteration);

			if (phase_timing) {
				phaseStart = System.currentTimeMillis();
			}

			buildTreeAndDoCoM();

			if (phase_timing) {
				phaseEnd = System.currentTimeMillis();
				btcomTime += phaseEnd - phaseStart;
				btcomTimeTmp = phaseEnd - phaseStart;
			}

			//force calculation

			if (phase_timing) {
				phaseStart = System.currentTimeMillis();
			}

			if (impl == IMPL_TUPLE) {
				rootId = "root" + iteration;
				ibis.satin.SatinTupleSpace.add(rootId, root);
			}

			ibis.satin.SatinObject.resume(); //turn ON divide-and-conquer stuff

			switch (impl) {
			case IMPL_NTC:
				result = root.barnesNTC(root, spawn_threshold);
				root.sync();
				break;
			case IMPL_TUPLE:
				result = dummyNode.barnesTuple(null, rootId, spawn_threshold);
				dummyNode.sync();
				break;
			case IMPL_SEQ:
				result = root.barnesSequential(root);
				break;
			}

			ibis.satin.SatinObject.pause(); //killall divide-and-conquer stuff

			if (impl == IMPL_TUPLE) {
				ibis.satin.SatinTupleSpace.remove(rootId);
			}

			processLinkedListResult(result, accs_x, accs_y, accs_z);

			if (phase_timing) {
				phaseEnd = System.currentTimeMillis();
				forceCalcTimes[iteration] = phaseEnd - phaseStart;
				forceCalcTimeTmp = phaseEnd - phaseStart;
				phaseStart = System.currentTimeMillis();
			}

			updateBodies(accs_x, accs_y, accs_z, iteration);

			if (phase_timing) {
				phaseEnd = System.currentTimeMillis();
				updateTime += phaseEnd - phaseStart;
				updateTimeTmp = phaseEnd - phaseStart;
			}

			if (viz) {
				bc.repaint();
			}

			System.err.println("Iteration " + iteration + " done, tree build = "
					+ btcomTimeTmp + ", update = " + updateTimeTmp
					+ ", force = " + forceCalcTimeTmp);
		}

		end = System.currentTimeMillis();
		totalTime = end - start;
	}

	void tuple2RunSim() {
		/*
		 * all sequential parts are now replicated to increase the efficiency of
		 * the broadcast. Now, only the new accs have to be broadcast each
		 * iteration.
		 */

		int iteration, i;
		LinkedList result;
		BodyTreeNode dummyNode = new BodyTreeNode(); //used to spawn jobs
		double[] accs_x = new double[bodyArray.length];
		double[] accs_y = new double[bodyArray.length];
		double[] accs_z = new double[bodyArray.length];

		long start = 0, end, phaseStart = 0, phaseEnd;

		TreeUpdater u;
		String key;

		start = System.currentTimeMillis();

		for (iteration = 0; iteration < iterations; iteration++) {
			//			System.out.println("Starting iteration " + iteration);

			//don't measure the first iteration
			//the body update phase of the first iteration *is* measured ???
			//if (iteration == 1) {
			//}

			/*
			 * tree construction and CoM computation are done using an active
			 * tuple
			 */

			if (phase_timing) {
				phaseStart = System.currentTimeMillis();
			}

			if (iteration == 0) {
				/*
				 * broadcast an 'empty' tuple, since there is no previous
				 * iteration whose updates have to be applied
				 */
				u = new TreeUpdater(null, null, null);
			} else {
				//put the result from the previous iteration in tuple space
				u = new TreeUpdater(accs_x, accs_y, accs_z);
			}
			key = Integer.toString(iteration);
			ibis.satin.SatinTupleSpace.add(key, u);

			if (phase_timing) {
				phaseEnd = System.currentTimeMillis();
				btcomTime += phaseEnd - phaseStart;
			}

			//force calculation
			if (phase_timing) {
				phaseStart = System.currentTimeMillis();
			}

			ibis.satin.SatinObject.resume();
			result = dummyNode.barnesTuple2(null, spawn_threshold);
			dummyNode.sync();
			ibis.satin.SatinObject.pause();

			processLinkedListResult(result, accs_x, accs_y, accs_z);

			if (phase_timing) {
				phaseEnd = System.currentTimeMillis();
				forceCalcTimes[iteration] = phaseEnd - phaseStart;
			}

			//removing the tuple isn't necessary since it's an active tuple

			//body updates will be done in the next iteration, or below
		}

		//do the final body update phase (this phase is otherwise done at
		//the start of the next iteration)
		if (phase_timing) {
			phaseStart = System.currentTimeMillis();
		}
		//iteration must be decremented because of the for loop above
		updateBodies(accs_x, accs_y, accs_z, iteration - 1);
		if (phase_timing) {
			phaseEnd = System.currentTimeMillis();
			btcomTime += phaseEnd - phaseStart;
		}

		end = System.currentTimeMillis();
		totalTime = end - start;
	}

	private BodyCanvas visualize() {
		JFrame.setDefaultLookAndFeelDecorated(true);

		//Create and set up the window.
		JFrame frame = new JFrame("Bodies");
		//frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		BodyCanvas bc = new BodyCanvas(500, 500, bodyArray);
		frame.getContentPane().add(bc);

		//Display the window.
		frame.pack();
		frame.setVisible(true);

		return bc;
	}

	void processLinkedListResult(LinkedList result, double[] all_x,
			double[] all_y, double[] all_z) {
		Iterator it = result.iterator();
		int[] bodyNumbers;
		double[] tmp_x, tmp_y, tmp_z;
		int i;

		/*
		 * I tried putting bodies computed by the same leaf job together in the
		 * array of bodies, by creating a new bodyArray every time (to find the
		 * current position of a body an extra int[] lookup table was used,
		 * which of course had to be updated every iteration)
		 * 
		 * I thought this would improve locality during the next tree building
		 * phase, and indeed, the tree building phase was shorter with a
		 * sequential run with ibm 1.4.1 with jitc (with 4000 bodies/10
		 * maxleafbodies: 0.377 s vs 0.476 s) (the CoM and update phases were
		 * also slightly shorter)
		 * 
		 * but the force calc phase was longer, in the end the total run time
		 * was longer ( 18.24 s vs 17.66 s )
		 */

		while (it.hasNext()) {
			bodyNumbers = (int[]) it.next();
			tmp_x = (double[]) it.next();
			tmp_y = (double[]) it.next();
			tmp_z = (double[]) it.next();

			for (i = 0; i < bodyNumbers.length; i++) {
				all_x[bodyNumbers[i]] = tmp_x[i];
				all_y[bodyNumbers[i]] = tmp_y[i];
				all_z[bodyNumbers[i]] = tmp_z[i];
				if (ASSERTS)
					bodyArray[bodyNumbers[i]].updated = true;
			}
		}
	}

	void printBodies() {
		Body b;
		int i;

		Body[] sorted = new Body[bodyArray.length];
		System.arraycopy(bodyArray, 0, sorted, 0, bodyArray.length);

		Arrays.sort(sorted); //sort the copied bodyArray (by bodyNumber)

		for (i = 0; i < bodyArray.length; i++) {
			b = sorted[i];
			System.out.println("0: Body " + i + ": [ " + b.pos_x + ", "
					+ b.pos_y + ", " + b.pos_z + " ]");
			System.out.println("0:      " + i + ": [ " + b.vel_x + ", "
					+ b.vel_y + ", " + b.vel_z + " ]");
			System.out.println("0:      " + i + ": [ " + b.acc_x + ", "
					+ b.acc_y + ", " + b.acc_z + " ]");
			System.out.println("0:      " + i + ": " + b.number);
		}
	}

	/*
	 * private BodyCanvas visualize() {
	 * JFrame.setDefaultLookAndFeelDecorated(true);
	 * 
	 * //Create and set up the window. JFrame frame = new JFrame("Bodies");
	 * //frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	 * frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	 * 
	 * BodyCanvas bc = new BodyCanvas(500, 500, bodies);
	 * frame.getContentPane().add(bc);
	 * 
	 * //Display the window. frame.pack(); frame.setVisible(true);
	 * 
	 * return bc; }
	 */
	void run() {
		int i;

		System.out.println("Iterations: " + iterations + " (timings DO "
				+ "include the first iteration!)");

		switch (impl) {
		case IMPL_NTC:
			System.out.println("Using necessary tree impl");
			runSim();
			break;
		case IMPL_TUPLE:
			System.out.println("Using old satin tuple impl");
			runSim();
			break;
		case IMPL_TUPLE2:
			System.out.println("Using new satin tuple impl");
			tuple2RunSim();
			break;
		case IMPL_SEQ:
			System.out.println("Using hierarchical sequential impl");
			runSim();
			break;
		default:
			System.out.println("EEK! Using unknown implementation #" + impl);
			System.exit(1);
			break; //blah
		}

		System.out.println("application barnes took "
				+ (double) (totalTime / 1000.0) + " s");

		if (phase_timing) {
			long total = 0;
			System.out.println("tree building and CoM computation took: "
					+ btcomTime / 1000.0 + " s");
			if (impl != IMPL_TUPLE2) {
				System.out.println("                  Updating bodies took: "
						+ updateTime / 1000.0 + " s");
			}
			System.out.println("Force calculation took: ");
			for (i = 0; i < iterations; i++) {
				System.out.println("  iteration " + i + ": "
						+ forceCalcTimes[i] / 1000.0 + " s");
				total += forceCalcTimes[i];
			}
			System.out
					.println("               total: " + total / 1000.0 + " s");
		}

		if (verbose) {
			System.out.println();
			printBodies();
		}
	}

	public static void main(String argv[]) {
		int nBodies = 0, mlb = 0;
		boolean nBodiesSeen = false;
		boolean mlbSeen = false;
		int i;

		long realStart, realEnd;

		realStart = System.currentTimeMillis();

		//parse arguments
		for (i = 0; i < argv.length; i++) {
			//options
			if (argv[i].equals("-debug")) {
				debug = true;
			} else if (argv[i].equals("-nodebug")) {
				debug = false;
			} else if (argv[i].equals("-v")) {
				verbose = true;
			} else if (argv[i].equals("-ntc")) {
				impl = IMPL_NTC;
			} else if (argv[i].equals("-tuple")) {
				impl = IMPL_TUPLE;
			} else if (argv[i].equals("-tuple2")) {
				impl = IMPL_TUPLE2;
			} else if (argv[i].equals("-seq")) {
				impl = IMPL_SEQ;

			} else if (argv[i].equals("-it")) {
				iterations = Integer.parseInt(argv[++i]);
				if (iterations < 0)
					throw new IllegalArgumentException(
							"Illegal argument to -t: Spawn threshold must be >= 0 !");

			} else if (argv[i].equals("-t")) {
				spawn_threshold = Integer.parseInt(argv[++i]);
				if (spawn_threshold < 0)
					throw new IllegalArgumentException(
							"Illegal argument to -t: Spawn threshold must be >= 0 !");

			} else if (argv[i].equals("-timing")) {
				phase_timing = true;
			} else if (argv[i].equals("-no_timing")) {
				phase_timing = false;

			} else if (!nBodiesSeen) {
				try {
					nBodies = Integer.parseInt(argv[i]); //nr of bodies to
					// simulate
					nBodiesSeen = true;
				} catch (NumberFormatException e) {
					System.err.println("Illegal argument: " + argv[i]);
					System.exit(1);
				}
			} else if (!mlbSeen) {
				try {
					mlb = Integer.parseInt(argv[i]); //max bodies per leaf node
					mlbSeen = true;
				} catch (NumberFormatException e) {
					System.err.println("Illegal argument: " + argv[i]);
					System.exit(1);
				}
			} else {
				System.err.println("Illegal argument: " + argv[i]);
				System.exit(1);
			}
		}

		if (nBodies < 1) {
			System.err.println("Invalid body count, generating 100 bodies...");
			nBodies = 100;
		}

		System.out.println("BarnesHut: simulating " + nBodies + " bodies, "
				+ mlb + " bodies/leaf node, " + "theta = " + THETA
				+ ", spawn-threshold = " + spawn_threshold);
		try {
			new BarnesHut(nBodies, mlb).run();
		} catch (StackOverflowError e) {
			System.err.println("EEK!" + e + ":");
			e.printStackTrace();
		}

		realEnd = System.currentTimeMillis();
		ibis.satin.SatinObject.resume(); //allow satin to exit cleanly

		System.out.println("Real run time = " + (realEnd - realStart) / 1000.0
				+ " s");
	}
}