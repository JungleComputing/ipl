/* $Id$ */

//import javax.swing.*;

import java.util.*;

final class BarnesHut {

    static boolean debug = false; //use -(no)debug to modify

    static boolean verbose = false; //use -v to turn on

    static final boolean ASSERTS = false; //also used in other barnes classes

    private static final int IMPL_NTC = 1; // -ntc option

    private static final int IMPL_TUPLE = 2; // -tuple option

    private static final int IMPL_TUPLE2 = 3; // -tuple2 option

    private static final int IMPL_SEQ = 4; // -seq option

    private static int impl = IMPL_SEQ;

    //recursion depth at which the ntc/tuple impl work sequentially
    private static int spawn_threshold = 4; //use -t <threshold> to modify

    //true: Collect statistics about the various phases in each iteration
    public static boolean phase_timing = true; //use -(no)timing to modify

    private long totalTime = 0;

    private long btcomTime = 0, updateTime = 0;

    private long[] forceCalcTimes;

    //Parameters for the BarnesHut algorithm / simulation
    private static final double THETA = 3.0; //cell subdivision tolerance

    private static final double DT = 0.025; //integration time-step

    //we do 7 iterations (first one isn't measured)
    static final double START_TIME = 0.0;

    static final double END_TIME = 0.175;

    final int ITERATIONS;

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
            /* the tuple2 impl does the initialization at every node when
             the first tuple is broadcast */
            Initializer init = new Initializer(n, m);
            ibis.satin.SatinTupleSpace.add("init", init);
        }

        /* The RMI version contained magic code equivalent to this:
         (the DEFAULT_* variables were different)

         double scale = Math.pow( nBodies / 16384.0, -0.25 );
         DT = DEFAULT_DT * scale;
         END_TIME = DEFAULT_END_TIME * scale;
         THETA = DEFAULT_THETA / scale;

         Since Rutger didn't know where it came from, and barnes from
         splash2 also doesn't include this code, I will omit it. - Maik. */

        ITERATIONS = (int) ((END_TIME + 0.1 * DT - START_TIME) / DT);
        // ITERATIONS = 2; //debug mode

        forceCalcTimes = new long[ITERATIONS];
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

    /* Builds the tree with bodies using bodyArray, maxLeafBodies and THETA,
     and does the center-of-mass computation for this new tree */
    static void buildTreeAndDoCoM() {
        root = null; //prevent Out-Of-Memory because 2 trees are in mem
        root = new BodyTreeNode(bodyArray, maxLeafBodies, THETA);
        root.computeCentersOfMass();
    }

    static void updateBodies(Vec3[] accs, int iteration) {
        int i;

        for (i = 0; i < bodyArray.length; i++) {
            //the updated-bit only gets set at the root node
            //with tuple2, this assertion failed at a non-root-node...
            if (ASSERTS && !bodyArray[i].updated && I_AM_ROOT) {
                System.err.println("EEK! Body " + i + " wasn't updated!");
                System.exit(1);
            }
            if (ASSERTS && accs[i].x > 1.0E4) { //This shouldn't happen
                System.err.println("EEK! Acc_x too large for body #" + i
                        + " in iteration: " + iteration);
                System.err.println("acc = " + accs[i].x);
                System.exit(1);
            }

            bodyArray[i].computeNewPosition(iteration != 0, DT, accs[i]);

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
        Vec3[] accs = new Vec3[bodyArray.length];

        //BodyCanvas bc = visualize();

        ibis.satin.SatinObject.pause(); //turn off satin during sequential parts

        for (iteration = 0; iteration < ITERATIONS; iteration++) {

            //don't measure the first iteration (number 0)
            if (iteration == 1) {
                start = System.currentTimeMillis();
            }

            System.out.println("Starting iteration " + iteration);

            if (phase_timing && iteration > 0) {
                phaseStart = System.currentTimeMillis();
            }

            buildTreeAndDoCoM();

            if (phase_timing) {
                if (iteration > 0) {
                    phaseEnd = System.currentTimeMillis();
                    btcomTime += phaseEnd - phaseStart;
                }
                phaseStart = System.currentTimeMillis();
            }

            //force calculation

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

            processLinkedListResult(result, accs);

            if (phase_timing) {
                phaseEnd = System.currentTimeMillis();
                forceCalcTimes[iteration] = phaseEnd - phaseStart;
                if (iteration > 0) {
                    phaseStart = System.currentTimeMillis();
                }
            }

            updateBodies(accs, iteration);

            if (phase_timing && iteration > 0) {
                phaseEnd = System.currentTimeMillis();
                updateTime += phaseEnd - phaseStart;
            }

            //bc.repaint();
        }

        end = System.currentTimeMillis();
        totalTime = end - start;
    }

    void tuple2RunSim() {
        /* all sequential parts are now replicated to increase the
         efficiency of the broadcast. Now, only the new accs have to
         be broadcast each iteration. */

        int iteration, i;
        LinkedList result;
        BodyTreeNode dummyNode = new BodyTreeNode(); //used to spawn jobs
        Vec3[] accs = new Vec3[bodyArray.length];

        long start = 0, end, phaseStart = 0, phaseEnd;

        TreeUpdater u;
        String key;

        for (iteration = 0; iteration < ITERATIONS; iteration++) {
            System.out.println("Starting iteration " + iteration);

            //don't measure the first iteration
            //the body update phase of the first iteration *is* measured ???
            if (iteration == 1) {
                start = System.currentTimeMillis();
            }

            /* tree construction and CoM computation are done using an
             active tuple */

            if (phase_timing) {
                phaseStart = System.currentTimeMillis();
            }

            if (iteration == 0) {
                /* broadcast an 'empty' tuple, since there is no previous
                 iteration whose updates have to be applied */
                u = new TreeUpdater(null);
            } else {
                //put the result from the previous iteration in tuple space
                u = new TreeUpdater(accs);
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

            processLinkedListResult(result, accs);

            if (phase_timing) {
                phaseEnd = System.currentTimeMillis();
                forceCalcTimes[iteration] = phaseEnd - phaseStart;
            }

            //removing the tuple isn't necessary since it's an active tuple

            //body updates will be done in the next iteration, or below
        }

        //do the final body update phase (this phase is otherwise done at
        //the start of the next iteration)
        //iteration must be decremented because of the for loop above
        updateBodies(accs, iteration - 1);

        end = System.currentTimeMillis();
        totalTime = end - start;
    }

    void processLinkedListResult(LinkedList result, Vec3[] all) {
        Iterator it = result.iterator();
        int[] bodyNumbers;
        Vec3[] tmp;
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

        while (it.hasNext()) {
            bodyNumbers = (int[]) it.next();
            tmp = (Vec3[]) it.next();
            for (i = 0; i < bodyNumbers.length; i++) {
                all[bodyNumbers[i]] = tmp[i];
                if (ASSERTS)
                    bodyArray[bodyNumbers[i]].updated = true;
            }
        }
    }

    void printBodies() {
        Body b;
        int i;

        Body[] sorted = new Body[bodyArray.length]; //copy bodyArray
        for (i = 0; i < bodyArray.length; i++)
            sorted[i] = bodyArray[i];

        Arrays.sort(sorted); //sort the copied bodyArray (by bodyNumber)

        for (i = 0; i < bodyArray.length; i++) {
            b = sorted[i];
            System.out.println("0: Body " + i + ": [ " + b.pos.x + ", "
                    + b.pos.y + ", " + b.pos.z + " ]");
            System.out.println("0:      " + i + ": [ " + b.vel.x + ", "
                    + b.vel.y + ", " + b.vel.z + " ]");
            if (b.acc != null) {
                System.out.println("0:      " + i + ": [ " + b.acc.x + ", "
                        + b.acc.y + ", " + b.acc.z + " ]");
            } else {
                System.out.println("0:      " + i + ": [ 0.0, 0.0, 0.0 ]");
            }
            System.out.println("0:      " + i + ": " + b.number);
        }
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

    void run() {
        int i;

        System.out.println("Iterations: " + ITERATIONS + " (timings DON'T "
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
            if (impl != IMPL_TUPLE2) {
                System.out.println("tree building and CoM computation took: "
                        + btcomTime / 1000.0 + " s");
                System.out.println("                  Updating bodies took: "
                        + updateTime / 1000.0 + " s");
            }
            System.out.println("Force calculation took: ");
            for (i = 0; i < ITERATIONS; i++) {
                System.out.println("  iteration " + i + ": "
                        + forceCalcTimes[i] / 1000.0 + " s");
                if (i >= 1) {
                    total += forceCalcTimes[i];
                }
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
        boolean nBodiesSeen = false, mlbSeen = false;
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
                    nBodies = Integer.parseInt(argv[i]); //nr of bodies to simulate
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