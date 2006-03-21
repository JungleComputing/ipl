/* $Id$ */

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/* strictfp */final class BarnesHut {

    static boolean remoteViz = false;

    static boolean viz = false;

    static boolean debug = false; //use -(no)debug to modify

    static boolean verbose = false; //use -v to turn on

    static final boolean ASSERTS = false; //also used in other barnes classes

    static final int IMPL_NTC = 1; // -ntc option

    static final int IMPL_SEQ = 4; // -seq option

    static int impl = IMPL_NTC;

    //number of bodies at which the ntc impl work sequentially
    private static int spawn_min = 500; //use -min <threshold> to modify

    //number of bodies above which the ntc impl work sequentially
    private static int spawn_max = -1; //use -max <threshold> to modify

    private long totalTime = 0, comTime = 0, btTime = 0, trimTime = 0,
            updateTime = 0, totalForceCalcTime = 0;

    //Parameters for the BarnesHut algorithm / simulation
    private static double THETA = 2.0; // cell subdivision tolerance

    static double DT = 0.025; // default integration time-step

    static double DT_HALF = DT / 2.0; // default integration time-step

    //we do 7 iterations (first one isn't measured)
    static double START_TIME = 0.0;

    static double END_TIME = 0.175;

    static int iterations = -1;

    static Body[] bodyArray;

    static int maxLeafBodies;

    static BodyTreeNode root; //is accessed by BodyTreeNode

    //Indicates if we are the root divide-and-conquer node
    static boolean I_AM_ROOT = false;

    BarnesHut(int n, int m) {
        I_AM_ROOT = true; //constructor is only called at the root node

        initialize(n, m);

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
    }

    private double readDouble(StreamTokenizer tk, String err)
        throws IOException {
        int tok = tk.nextToken();
        if (tok != StreamTokenizer.TT_WORD) {
            throw new IOException(err);
        }
        return Double.parseDouble(tk.sval);
    }

    BarnesHut(Reader r, int m) throws IOException {
        BufferedReader br = new BufferedReader(r);
        StreamTokenizer tokenizer = new StreamTokenizer(br);

        I_AM_ROOT = true; //constructor is only called at the root node
        maxLeafBodies = m;

        tokenizer.resetSyntax();
        tokenizer.wordChars('0', '9');
        tokenizer.wordChars('.', '.');
        tokenizer.wordChars('+', '+');
        tokenizer.wordChars('E', 'E');
        tokenizer.wordChars('-', '-');
        tokenizer.eolIsSignificant(false);
        tokenizer.whitespaceChars('\t', ' ');

        int nBodies = (int) readDouble(tokenizer,
            "number expected for number of bodies");

        spawn_max = nBodies;

        // Ignore number of dimensions.
        readDouble(tokenizer, "number expected for dimensions");

        // Ignore start time.
        readDouble(tokenizer, "number expected for start time");

        // Allocate bodies and read mass.
        bodyArray = new Body[nBodies];
        for (int i = 0; i < nBodies; i++) {
            Body body = new Body();
            bodyArray[i] = body;
            body.number = i;
            body.mass = readDouble(tokenizer,
                "number expected for mass of body");
        }

        // Read positions
        for (int i = 0; i < nBodies; i++) {
            Body body = bodyArray[i];
            body.pos_x = readDouble(tokenizer, "x coordinate expected for body");
            body.pos_y = readDouble(tokenizer, "y coordinate expected for body");
            body.pos_z = readDouble(tokenizer, "z coordinate expected for body");
        }

        // Read velocities
        for (int i = 0; i < nBodies; i++) {
            Body body = bodyArray[i];
            body.vel_x = readDouble(tokenizer, "x velocity expected for body");
            body.vel_y = readDouble(tokenizer, "y velocity expected for body");
            body.vel_z = readDouble(tokenizer, "z velocity expected for body");
        }

        if (iterations == -1) {
            iterations = (int) ((END_TIME + 0.1 * DT - START_TIME) / DT);
        }
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

    static void updateBodies(double[] accs_x, double[] accs_y, double[] accs_z,
        int iteration) {
        int i;

        for (i = 0; i < bodyArray.length; i++) {
            //the updated-bit only gets set at the root node
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

            bodyArray[i].computeNewPosition(iteration != 0, accs_x[i],
                accs_y[i], accs_z[i]);

            if (ASSERTS) bodyArray[i].updated = false;
        }
    }

    void runSim() {
        long start = 0, end, phaseStart = 0;

        ArrayList result = null;
        double[] accs_x = new double[bodyArray.length];
        double[] accs_y = new double[bodyArray.length];
        double[] accs_z = new double[bodyArray.length];

        BodyCanvas bc = null;

        RemoteVisualization rv = null;

        // print the starting problem
        if (verbose) {
            printBodies();
        }

        if (viz) {
            bc = BodyCanvas.visualize(bodyArray);
        }

        if (remoteViz) {
            rv = new RemoteVisualization();

            try {
                Thread.sleep(10000);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }

        ibis.satin.SatinObject.pause(); //turn off satin during sequential
        // parts

        start = System.currentTimeMillis();

        for (int iteration = 0; iteration < iterations; iteration++) {
            long comTimeTmp = 0, btTimeTmp = 0, trimTimeTmp = 0, updateTimeTmp = 0, forceCalcTimeTmp = 0;

            // System.out.println("Starting iteration " + iteration);

            phaseStart = System.currentTimeMillis();

            /*
             * Builds the tree with bodies using bodyArray, maxLeafBodies and THETA, and
             * does the center-of-mass computation for this new tree
             */
            root = null; //prevent Out-Of-Memory because 2 trees are in mem
            root = new BodyTreeNode(bodyArray, maxLeafBodies, THETA);

            btTimeTmp = System.currentTimeMillis() - phaseStart;
            btTime += btTimeTmp;

            phaseStart = System.currentTimeMillis();

            root.trim();

            trimTimeTmp = System.currentTimeMillis() - phaseStart;
            trimTime += trimTimeTmp;

            phaseStart = System.currentTimeMillis();

            root.computeCentersOfMass();

            comTimeTmp = System.currentTimeMillis() - phaseStart;
            comTime += comTimeTmp;

            //force calculation

            phaseStart = System.currentTimeMillis();

            ibis.satin.SatinObject.resume(); //turn ON divide-and-conquer stuff

            switch (impl) {
            case IMPL_NTC:
                result = root.doBarnes(root, spawn_min, spawn_max);
                break;
            case IMPL_SEQ:
                result = new ArrayList();
                root.barnesSequential(root, result);
                break;
            }

            ibis.satin.SatinObject.pause(); //killall divide-and-conquer stuff

            processLinkedListResult(result, accs_x, accs_y, accs_z);

            forceCalcTimeTmp = System.currentTimeMillis() - phaseStart;
            totalForceCalcTime += forceCalcTimeTmp;

            phaseStart = System.currentTimeMillis();

            updateBodies(accs_x, accs_y, accs_z, iteration);

            updateTimeTmp = System.currentTimeMillis() - phaseStart;
            updateTime += updateTimeTmp;

            if (viz) {
                bc.repaint();
            }

            if (remoteViz) {
                rv.showBodies(bodyArray);
            }

            System.err.println("Iteration " + iteration
                + " done, tree build = " + btTimeTmp + ", trim time = "
                + trimTimeTmp + ", com = " + comTimeTmp + ", update = "
                + updateTimeTmp + ", force = " + forceCalcTimeTmp);
        }

        end = System.currentTimeMillis();
        totalTime = end - start;
    }

    void processLinkedListResult(List result, double[] all_x, double[] all_y,
        double[] all_z) {
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
                if (ASSERTS) bodyArray[bodyNumbers[i]].updated = true;
            }
        }
    }

    void printBodies() {
        Body b;
        int i;

        Body[] sorted = new Body[bodyArray.length];
        System.arraycopy(bodyArray, 0, sorted, 0, bodyArray.length);

        Arrays.sort(sorted); // sort the copied bodyArray (by bodyNumber)

        for (i = 0; i < bodyArray.length; i++) {
            b = sorted[i];
            System.out.println("0: Body " + i + ": [ " + b.pos_x + ", "
                + b.pos_y + ", " + b.pos_z + " ]");
            System.out.println("0:      " + i + ": [ " + b.vel_x + ", "
                + b.vel_y + ", " + b.vel_z + " ]");
            System.out.println("0:      " + i + ": [ " + b.oldAcc_x + ", "
                + b.oldAcc_y + ", " + b.oldAcc_z + " ]");
            System.out.println("0:      " + i + ": [ " + b.mass + " ]");
            // System.out.println("0:      " + i + ": " + b.number);
        }
    }

    void run() {
        System.out.println("Iterations: " + iterations + " (timings DO "
            + "include the first iteration!)");

        switch (impl) {
        case IMPL_NTC:
            System.out.println("Using necessary tree impl");
            runSim();
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

        System.out.println("tree building took      " + btTime / 1000.0 + " s");
        System.out.println("trim took               " + trimTime / 1000.0 + " s");
        System.out
            .println("CoM computation took    " + comTime / 1000.0 + " s");
        System.out.println("Force calculation took  " + totalForceCalcTime
            / 1000.0 + " s");
        System.out.println("application barnes took "
            + (double) (totalTime / 1000.0) + " s");

        if (verbose) {
            System.out.println();
            printBodies();
        }
    }

    public static void main(String argv[]) {
        int nBodies = 0, mlb = 3;
        boolean nBodiesSeen = false;
        boolean mlbSeen = false;
        FileReader rdr = null;
        int i;

        //parse arguments
        for (i = 0; i < argv.length; i++) {
            //options
            if (argv[i].equals("-debug")) {
                debug = true;
            } else if (argv[i].equals("-no-debug")) {
                debug = false;
            } else if (argv[i].equals("-v")) {
                verbose = true;
            } else if (argv[i].equals("-no-v")) {
                verbose = false;
            } else if (argv[i].equals("-viz")) {
                viz = true;
            } else if (argv[i].equals("-remote-viz")) {
                remoteViz = true;
            } else if (argv[i].equals("-no-viz")) {
                viz = false;
            } else if (argv[i].equals("-ntc")) {
                impl = IMPL_NTC;
            } else if (argv[i].equals("-seq")) {
                impl = IMPL_SEQ;
            } else if (argv[i].equals("-it")) {
                iterations = Integer.parseInt(argv[++i]);
                if (iterations < 0) {
                    throw new IllegalArgumentException(
                        "Illegal argument to -it: number of iterations must be >= 0 !");
                }
            } else if (argv[i].equals("-theta")) {
                THETA = Double.parseDouble(argv[++i]);
            } else if (argv[i].equals("-starttime")) {
                START_TIME = Double.parseDouble(argv[++i]);
            } else if (argv[i].equals("-endtime")) {
                END_TIME = Double.parseDouble(argv[++i]);
            } else if (argv[i].equals("-dt")) {
                DT = Double.parseDouble(argv[++i]);
                DT_HALF = DT / 2.0;
            } else if (argv[i].equals("-input")) {
                try {
                    rdr = new FileReader(argv[++i]);
                } catch (IOException e) {
                    throw new IllegalArgumentException(
                        "Could not open input file " + argv[i]);
                }
            } else if (argv[i].equals("-min")) {
                spawn_min = Integer.parseInt(argv[++i]);
                if (spawn_min < 0) {
                    throw new IllegalArgumentException(
                        "Illegal argument to -min: Spawn min threshold must be >= 0 !");
                }
            } else if (argv[i].equals("-max")) {
                spawn_max = Integer.parseInt(argv[++i]);
                if (spawn_max < 0) {
                    throw new IllegalArgumentException(
                        "Illegal argument to -max: Spawn max threshold must be >= 0 !");
                }
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
            System.err.println("Invalid body count, generating 300 bodies...");
            nBodies = 300;
        }

        if (spawn_max < 0) {
            spawn_max = nBodies;
        }

        System.out.println("BarnesHut: simulating " + nBodies + " bodies, "
            + mlb + " bodies/leaf node, " + "theta = " + THETA
            + ", spawn-min-threshold = " + spawn_min
            + ", spawn-max-threshold = " + spawn_max);

        if (rdr != null) {
            if (nBodiesSeen) {
                System.out
                    .println("Warning: nBodies as seen in argument list ignored!");
            }
            try {
                new BarnesHut(rdr, mlb).run();
            } catch (IOException e) {
                throw new NumberFormatException(e.getMessage());
            }
        } else {
            new BarnesHut(nBodies, mlb).run();
        }

        ibis.satin.SatinObject.resume(); // allow satin to exit cleanly
    }
}
