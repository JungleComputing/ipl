//import javax.swing.*;
import java.util.*;

class BarnesHut {

    private static boolean debug = false;   //use -(no)debug to modify
    private static boolean verbose = false; //use -v to turn on
    static final boolean ASSERTS = true;  //also used in other barnes classes

    private static final int IMPL_ITER = 0;  // -iter option
    private static final int IMPL_NTC = 1;   // -ntc option
    private static final int IMPL_TUPLE = 2; // -tuple option
    private static int impl = IMPL_NTC;

    //recursion depth at which the ntc/tuple impl work sequentially
    private static int spawn_threshold = 4; //use -t <threshold> to modify

    //true: Collect statistics about the various phases in each iteration
    public static boolean phase_timing = true; //use -(no)timing to modify

    private long treeBuildTime = 0, CoMTime = 0;
    private long forceCalcTime = 0, updateTime = 0;

    static final double START_TIME = 0.0;

    //These values are from the barnes version in the splash2 suite:
    static final double DEFAULT_THETA = 1.0; //cell subdivision tolerance
    static final double DEFAULT_END_TIME = 0.075; //time to stop integration
    static final double DEFAULT_DT = 0.025; //integration time-step

    Body[] bodyArray;
    //int[] bodyIndices; //used to translate body nrs to indices in bodyArray
    final int maxLeafBodies;

    final double DT;
    final double END_TIME;
    final double THETA;
    final int ITERATIONS;

    BarnesHut(int nBodies, int mlb) {
	bodyArray = new Plummer().generate(nBodies);
	//bodyIndices = new int[nBodies];

	//Plummer makes sure that a body with number x also has index x
	for (int i = 0; i < nBodies; i++) {
	    if (ASSERTS && bodyArray[i].number != i) {
		System.err.println("EEK! Plummer generated an inconsistent " +
				   "body number");
		System.exit(1);
	    }
	    //bodyIndices[i] = i;
	}
	
	maxLeafBodies = mlb;

	/* The RMI version contained magic code equivalent to this:
	   (the DEFAULT_* variables were different)

	   double scale = Math.pow( nBodies / 16384.0, -0.25 );
	   DT = DEFAULT_DT * scale;
	   END_TIME = DEFAULT_END_TIME * scale;
	   THETA = DEFAULT_THETA / scale;

	   Since Rutger didn't know where it came from, and barnes from
	   splash2 also doesn't include this code, I will omit it. - Maik. */

	DT = DEFAULT_DT;
	END_TIME = DEFAULT_END_TIME;
	THETA = DEFAULT_THETA;

	ITERATIONS = (int)( (END_TIME + 0.1*DT - START_TIME) / DT);
    }

    void printBodies() {
	Body b;
	int i;

	Body[] sorted = new Body[bodyArray.length]; //copy bodyArray
	for (i = 0; i < bodyArray.length; i++) sorted[i] = bodyArray[i];

	Arrays.sort(sorted); //sort the copied bodyArray (by bodyNumber)

	for (i = 0; i < bodyArray.length; i++) {
	    b = sorted[i];
	    System.out.println("0: Body " + i + ": [ " + b.pos.x + ", " +
			       b.pos.y + ", " + b.pos.z + " ]" );
	    System.out.println("0:      " + i + ": [ " + b.vel.x + ", " +
			       b.vel.y + ", " + b.vel.z + " ]" );
	    if (b.acc != null) {
		System.out.println("0:      " + i + ": [ " + b.acc.x + ", " +
				   b.acc.y + ", " + b.acc.z + " ]" );
	    } else {
		System.out.println("0:      " + i + ": [ 0.0, 0.0, 0.0 ]");
	    }
	    System.out.println("0:      " + i + ": " + b.number );
 	}
    }


    void run() {
	Body b;
	int i;

	long time = runSim();

	System.out.println("application barnes took " +
			   (double)(time/1000.0) + " s");

	if (phase_timing) {
	    System.out.println("    tree building took: " +
			       treeBuildTime/1000.0 + " s");
	    System.out.println("  CoM computation took: " +
			       CoMTime/1000.0 + " s");
	    System.out.println("Force calculation took: " +
			       forceCalcTime/1000.0 + " s");
	    System.out.println("  Updating bodies took: " +
			       updateTime/1000.0 + " s");
	}
	
	if (verbose) {
	    //System.out.println("application result: ");
	    System.out.println();
	    printBodies();
	}
    }	

    long runSim() {
	int i, iteration;
	BodyTreeNode btRoot;
	long start, end;
	long phaseStart = 0, phaseEnd;

	LinkedList result = null;
	Iterator it;

	Body b;

	//BodyCanvas bc = visualize();

	System.out.println("BarnesHut: doing " + ITERATIONS +
			   " iterations with " + bodyArray.length +
			   " bodies, " + maxLeafBodies + " bodies/leaf node");
			   
	switch(impl) {
	case IMPL_ITER:
	    if (debug) {
		System.out.println("Using iterative debug impl");
	    } else {
		System.out.println("Using iterative spawn-for-each-body impl");
	    }
	    break;
	case IMPL_NTC:
	    System.out.println("Using necessary tree impl");
	    break;
	case IMPL_TUPLE:
	    System.out.println("Using satin tuple impl");
	    break;
	}

	start = System.currentTimeMillis();
		
	for (iteration = 0; iteration < ITERATIONS; iteration++) {
	//for (iteration = 0; iteration < 1; iteration++) {
	    if (debug) System.out.println("Starting iteration " + iteration);

	    if (phase_timing) phaseStart = System.currentTimeMillis();

	    //build tree
	    btRoot = new BodyTreeNode(bodyArray, maxLeafBodies, THETA);

	    if (phase_timing) {
		phaseEnd = System.currentTimeMillis();
		treeBuildTime += phaseEnd - phaseStart;
		phaseStart = System.currentTimeMillis();
	    }

	    //compute centers of mass
	    btRoot.computeCentersOfMass();

	    if (phase_timing) {
		phaseEnd = System.currentTimeMillis();
		CoMTime += phaseEnd - phaseStart;
		phaseStart = System.currentTimeMillis();
	    }

	    //force calculation

	    switch(impl) {
	    case IMPL_ITER:
		if (debug) {
		    btRoot.barnesDbg(bodyArray, iteration);
		} else {
		    btRoot.barnes(bodyArray);
		}
		break;
	    case IMPL_NTC:
		result = btRoot.barnes(btRoot, spawn_threshold);
		btRoot.sync();
		break;
	    case IMPL_TUPLE:
		BodyTreeNode dummyNode = new BodyTreeNode();

		String rootId = "root" + iteration;
		ibis.satin.SatinTupleSpace.add(rootId, btRoot);

		result = dummyNode.barnes(null, rootId, spawn_threshold);
		dummyNode.sync();
		break;
	    }

	    if (impl == IMPL_NTC || impl == IMPL_TUPLE) {
		/* these implementations return a list with bodyNumbers
		   and corresponding accs, which has to be processed */

		it = result.iterator();

		/* I tried putting bodies computed by the same leaf job
		   together in the array of bodies, by creating a new
		   bodyArray every time (to find the current position of
		   a body an extra int[] lookup table was used, which
		   of course had to be updated every iteration)

		   I thought this would improve locality during the next
		   tree building phase, and indeed, the tree building phase
		   was shorter with a sequential run with ibm 1.4.1 with jitc
		   (with 4000 bodies/10 maxleafbodies: 0.377 s vs 0.476 s)
		   (the CoM and update phases were also slightly shorter)

		   but the force calc phase was longer, in the end the
		   total run time was longer ( 18.24 s vs 17.66 s ) */

		int[] bodyNumbers;
		Vec3[] accs;

		//Body[] newArray = new Body[bodyArray.length];
		//int newIndex = 0, oldIndex;

		while(it.hasNext()) {
		    bodyNumbers = (int []) it.next();
		    accs = (Vec3 []) it.next();
		    for (i = 0; i < bodyNumbers.length; i++) {
			//oldIndex = bodyIndices[bodyNumbers[i]];
			//bodyIndices[bodyNumbers[i]] = newIndex;

			//newArray[newIndex] = bodyArray[oldIndex];
			//newArray[newIndex].acc = accs[i];
			//if (ASSERTS) newArray[newIndex].updated = true;

			//newIndex++;

			bodyArray[bodyNumbers[i]].acc = accs[i];
			if (ASSERTS) bodyArray[bodyNumbers[i]].updated = true;
		    }
		}
		//bodyArray = newArray;

	    }
	    
	    if (phase_timing) {
		phaseEnd = System.currentTimeMillis();
		forceCalcTime += phaseEnd - phaseStart;
		phaseStart = System.currentTimeMillis();
	    }

	    //update bodies
	    for (i = 0; i < bodyArray.length; i++) {
		b = bodyArray[i];
		if (ASSERTS && !b.updated) {
		    System.err.println("EEK! Body " + i + " wasn't updated!");
		    System.exit(1);
		}
		if (ASSERTS && b.acc.x > 1.0E4) { //This shouldn't happen
		    System.err.println("EEK! Acc too large for body #" +
				       b.number + " in iteration: " +
				       iteration);
		    System.err.println("acc = " + b.acc);
		    System.exit(1);
		}

		b.computeNewPosition(iteration != 0, DT, b.acc);
		if (ASSERTS) b.updated = false;
		//??? max + min posities hier berekenen ipv bij boom bouwen
	    }

	    if (phase_timing) {
		phaseEnd = System.currentTimeMillis();
		updateTime += phaseEnd - phaseStart;
	    }
	    
	    //bc.repaint();
	}

	end = System.currentTimeMillis();
	return end - start;
    }

    /*private BodyCanvas visualize() {
      JFrame.setDefaultLookAndFeelDecorated(true);

      //Create and set up the window.
      JFrame frame = new JFrame("Bodies");
      //frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      BodyCanvas bc = new BodyCanvas(500, 500, bodies);
      frame.getContentPane().add(bc);

      //Display the window.
      frame.pack();
      frame.setVisible(true);

      return bc;
      }*/

    void wait4key() {
	System.out.print("Press enter..");
	try {
	    System.in.read();
	} catch (Exception e) {
	    System.out.println("EEK: " + e);
	}
    }

    public static void main(String argv[]) {
	int nBodies = 0, mlb = 0;
	int i;

	//parse arguments
	for (i = 0; i < argv.length; i++) {
	    //options
	    if (argv[i].equals("-debug")) {
		debug = true;
	    } else if (argv[i].equals("-nodebug")) {
		debug = false;
	    } else if (argv[i].equals("-v")) {
		verbose = true;

	    } else if (argv[i].equals("-iter")) {
		impl = IMPL_ITER;
	    } else if (argv[i].equals("-ntc")) {
		impl = IMPL_NTC;
	    } else if (argv[i].equals("-tuple")) {
		impl = IMPL_TUPLE;

	    } else if (argv[i].equals("-t")) {
		spawn_threshold = Integer.parseInt(argv[i+1]);
		if (spawn_threshold < 0) throw new IllegalArgumentException("Illegal argument to -t: Spawn threshold must be > 0 !");

	    } else if (argv[i].equals("-timing")) {
		phase_timing = true;
	    } else if (argv[i].equals("-no_timing")) {
		phase_timing = false;

	    } else { //final arguments
		nBodies = Integer.parseInt(argv[i]); //nr of bodies to simulate
		mlb = Integer.parseInt(argv[i+1]);   //max bodies per leaf node
		break;
	    }
	}
	if (nBodies < 1) {
	    System.err.println("Invalid body count, generating 100 bodies...");
	    nBodies = 100;
	}

	try {  
	    new BarnesHut(nBodies, mlb).run();
	} catch (StackOverflowError e) {
	    System.err.println("EEK!" + e + ":");
	    e.printStackTrace();
	}
    }
}
