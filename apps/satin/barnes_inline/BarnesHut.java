//import javax.swing.*;
import java.util.*;

final class BarnesHut {

    static boolean debug = false;   //use -(no)debug to modify
    static boolean verbose = false; //use -v to turn on
    static final boolean ASSERTS = false;  //also used in other barnes classes

    private static final int IMPL_ITER = 0;  // -iter option
    private static final int IMPL_NTC = 1;   // -ntc option
    private static final int IMPL_TUPLE = 2; // -tuple option
    private static final int IMPL_SEQ = 3;   // -seq option
    private static int impl = IMPL_NTC;

    //recursion depth at which the ntc/tuple impl work sequentially
    private static int spawn_threshold = 4; //use -t <threshold> to modify

    //true: Collect statistics about the various phases in each iteration
    public static boolean phase_timing = true; //use -(no)timing to modify

    private long treeBuildTime = 0, CoMTime = 0, updateTime = 0;
    private long[] forceCalcTimes;

    static final double START_TIME = 0.0;

    //These values are from the barnes version in the splash2 suite:
    static final double DEFAULT_THETA = 1.0; //cell subdivision tolerance
    //static final double DEFAULT_END_TIME = 0.075; //time to stop integration
    static final double DEFAULT_DT = 0.025; //integration time-step

    //we do 7 iterations, instead of 3 (first one isn't measured)
    static final double DEFAULT_END_TIME = 0.175;

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
	//ITERATIONS = 2; //debug mode

	forceCalcTimes = new long[ITERATIONS];
    }

    void printBodies() {
	Body b;
	int i;

	Body[] sorted = new Body[bodyArray.length]; //copy bodyArray
	for (i = 0; i < bodyArray.length; i++) sorted[i] = bodyArray[i];

	Arrays.sort(sorted); //sort the copied bodyArray (by bodyNumber)

	for (i = 0; i < bodyArray.length; i++) {
	    b = sorted[i];
	    System.out.println("0: Body " + i + ": [ " + b.pos_x + ", " +
			       b.pos_y + ", " + b.pos_z + " ]" );
	    System.out.println("0:      " + i + ": [ " + b.vel_x + ", " +
			       b.vel_y + ", " + b.vel_z + " ]" );
	    System.out.println("0:      " + i + ": [ " + b.acc_x + ", " +
			       b.acc_y + ", " + b.acc_z + " ]" );
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
	    long total = 0;
	    System.out.println("  tree building took: " +
			       treeBuildTime/1000.0 + " s");
	    System.out.println("CoM computation took: " +
			       CoMTime/1000.0 + " s");
	    System.out.println("Updating bodies took: " +
			       updateTime/1000.0 + " s");
	    System.out.println("Force calculation took: ");
	    for (i = 1; i < ITERATIONS; i++) {
		System.out.println("  iteration " + i + ": " +
				   forceCalcTimes[i]/1000.0 + " s");
		total += forceCalcTimes[i];
	    }
	    System.out.println("               total: " +
			       total/1000.0 + " s");
	}
	
	if (verbose) {
	    System.out.println();
	    printBodies();
	}
    }	

    long runSim() {
	int i, iteration;
	BodyTreeNode btRoot;
	String rootId = null; //tupleSpace key

	BodyTreeNode dummyNode = new BodyTreeNode(); //used to spawn jobs

	long start = 0, end;
	long phaseStart = 0, phaseEnd;

	LinkedList result = null;

	Body b;

	//BodyCanvas bc = visualize();

	System.out.println("BarnesHut: doing " + ITERATIONS +
			   " iterations with " + bodyArray.length +
			   " bodies, " + maxLeafBodies + " bodies/leaf node");
	System.out.println("           (measurements DON'T include the " +
			   "first iteration!)");

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
	case IMPL_SEQ:
	    System.out.println("Using hierarchical sequential impl");
	    break;
	}

	ibis.satin.Satin.pause(); //turn off satin during sequential parts

	for (iteration = 0; iteration < ITERATIONS; iteration++) {

	    //don't measure the first iteration (number 0)
	    if (iteration == 1) {
		start = System.currentTimeMillis();
	    }

	    System.out.println("Starting iteration " + iteration);

	    if (phase_timing && iteration > 0)
		phaseStart = System.currentTimeMillis();

	    //build tree
	    btRoot = null; //prevent Out-Of-Memory because 2 trees are in mem
	    btRoot = new BodyTreeNode(bodyArray, maxLeafBodies, THETA);

	    if (phase_timing && iteration > 0) {
		phaseEnd = System.currentTimeMillis();
		treeBuildTime += phaseEnd - phaseStart;
		phaseStart = System.currentTimeMillis();
	    }

	    //compute centers of mass
	    btRoot.computeCentersOfMass();

	    if (phase_timing && iteration > 0) {
		phaseEnd = System.currentTimeMillis();
		CoMTime += phaseEnd - phaseStart;
		phaseStart = System.currentTimeMillis();
	    }

	    //force calculation

	    if (impl == IMPL_ITER || impl == IMPL_TUPLE) {
		rootId = "root" + iteration;
		ibis.satin.SatinTupleSpace.add(rootId, btRoot);
	    }		

	    ibis.satin.Satin.resume(); //turn ON divide-and-conquer stuff

	    switch(impl) {
	    case IMPL_ITER:
		double[][] accs = new double[bodyArray.length][];

		if (debug) { //sequential debug version
		    double[] acc;
		    for (i = 0; i < bodyArray.length; i++) {
			acc = btRoot.barnesBodyDbg(bodyArray[i].pos_x,
						   bodyArray[i].pos_y,
						   bodyArray[i].pos_z, i == 0);
			//acc = barnesBodyDbg(bodyArray[i].pos, false);
			bodyArray[i].acc_x = acc[0];
			bodyArray[i].acc_y = acc[1];
			bodyArray[i].acc_z = acc[2];
			if (BarnesHut.ASSERTS) bodyArray[i].updated = true;
		    }
		} else { //normal parallel iterative version
		    for (i = 0; i < bodyArray.length; i++) {
			accs[i] = dummyNode.barnesBodyTuple
			    (bodyArray[i].pos_x, bodyArray[i].pos_y,
			     bodyArray[i].pos_z, rootId);
		    }
		    dummyNode.sync();

		    for (i = 0; i < bodyArray.length; i++) {
			bodyArray[i].acc_x = accs[i][0];
			bodyArray[i].acc_y = accs[i][1];
			bodyArray[i].acc_z = accs[i][2];
			if (ASSERTS) bodyArray[i].updated = true;
		    }
		}
		break;
	    case IMPL_NTC:
		result = btRoot.barnes(btRoot, spawn_threshold);
		btRoot.sync();
		break;
	    case IMPL_TUPLE:
		result = dummyNode.barnesTuple(null, rootId, spawn_threshold);
		dummyNode.sync();
		break;
	    case IMPL_SEQ:
		result = btRoot.barnesSequential(btRoot);
		break;
	    }

	    ibis.satin.Satin.pause(); //killall divide-and-conquer stuff

	    if (impl == IMPL_ITER || impl == IMPL_TUPLE) {
		try {
		    ibis.satin.SatinTupleSpace.remove(rootId);
		} catch (java.io.IOException e) {
		    System.err.println("EEK! " +e);
		    System.exit(1);
		}
	    }

	    if (impl == IMPL_NTC || impl == IMPL_TUPLE || impl == IMPL_SEQ) {
		/* these implementations return a list with bodyNumbers
		   and corresponding accs, which has to be processed */
		processLinkedListResult(result);
	    }

	    if (phase_timing && iteration > 0) {
		phaseEnd = System.currentTimeMillis();
		forceCalcTimes[iteration] = phaseEnd - phaseStart;
		phaseStart = System.currentTimeMillis();
	    }

	    //update bodies
	    for (i = 0; i < bodyArray.length; i++) {
		updateBody(bodyArray[i], iteration);
	    }

	    if (phase_timing && iteration > 0) {
		phaseEnd = System.currentTimeMillis();
		updateTime += phaseEnd - phaseStart;
	    }
	    
	    //bc.repaint();
	}

	end = System.currentTimeMillis();

	ibis.satin.Satin.resume();

	return end - start;
    }

    void processLinkedListResult(LinkedList result) {
	Iterator it = result.iterator();
	int[] bodyNumbers;
	double[] accs_x, accs_y, accs_z;
	int i;

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

	//Body[] newArray = new Body[bodyArray.length];
	//int newIndex = 0, oldIndex;

	while(it.hasNext()) {
	    bodyNumbers = (int []) it.next();
	    accs_x = (double []) it.next();
	    accs_y = (double []) it.next();
	    accs_z = (double []) it.next();

	    for (i = 0; i < bodyNumbers.length; i++) {
		//oldIndex = bodyIndices[bodyNumbers[i]];
		//bodyIndices[bodyNumbers[i]] = newIndex;

		//newArray[newIndex] = bodyArray[oldIndex];
		//newArray[newIndex].acc = accs[i];
		//if (ASSERTS) newArray[newIndex].updated = true;

		//newIndex++;

		bodyArray[bodyNumbers[i]].acc_x = accs_x[i];
		bodyArray[bodyNumbers[i]].acc_y = accs_y[i];
		bodyArray[bodyNumbers[i]].acc_z = accs_z[i];
		if (ASSERTS) bodyArray[bodyNumbers[i]].updated = true;
	    }
	}
	//bodyArray = newArray;
    }
	    
    void updateBody(Body b, int iteration) {
	if (ASSERTS && !b.updated) {
	    System.err.println("EEK! Body " + b.number + " wasn't updated!");
	    System.exit(1);
	}
	if (ASSERTS && b.acc_x > 1.0E4) { //This shouldn't happen
	    System.err.println("EEK! Acc_x too large for body #" +
			       b.number + " in iteration: " +
			       iteration);
	    System.err.println("acc = " + b.acc_x);
	    System.exit(1);
	}

	b.computeNewPosition(iteration != 0, DT, b.acc_x,
			     b.acc_y, b.acc_z);
	if (ASSERTS) b.updated = false;
	//??? max + min posities hier berekenen ipv bij boom bouwen
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
	    } else if (argv[i].equals("-seq")) {
		impl = IMPL_SEQ;

	    } else if (argv[i].equals("-t")) {
		spawn_threshold = Integer.parseInt(argv[++i]);
		if (spawn_threshold < 0) throw new IllegalArgumentException("Illegal argument to -t: Spawn threshold must be >= 0 !");

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
