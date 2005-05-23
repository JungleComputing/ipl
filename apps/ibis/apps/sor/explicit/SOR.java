/* $Id$ */

/**
 * SOR.java
 * Successive over relaxation
 * Ibis version implementing a red-black SOR, based on earlier Orca source.
 *
 * @author Rob van Nieuwpoort & Jason Maassen
 * @author Rutger Hofman
 * 	   Oct 20 2004
 * 	   	add a O(log n) reduce; move Main into this SOR;
 * 	   "improve" layout
 * 	   Oct 29 2004
 * 	   	Repair crucial bug in algorithm. It is necessary to exchange
 * 	   	border rows between each red and each black. It used to
 * 	   	exchange only between each pair of (red,black). Neighbour
 * 	   	exchange has become twice as expensive.
 */

import java.io.IOException;

import ibis.ipl.Ibis;
import ibis.ipl.PortType;
import ibis.ipl.SendPort;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.ReadMessage;
import ibis.ipl.WriteMessage;
import ibis.ipl.Registry;
import ibis.ipl.StaticProperties;
import ibis.ipl.IbisException;
import ibis.ipl.NoMatchingIbisException;
import ibis.ipl.Upcall;

import ibis.util.PoolInfo;
import ibis.util.Timer;
import ibis.util.TypedProperties;

public class SOR {

    private static final boolean USE_O_N_BROADCAST = TypedProperties
            .booleanProperty("bcast.O_n", false);

    private static final double TOLERANCE = 0.00001; /* termination criterion */

    private static final double LOCAL_STEPS = 0;

    private static final boolean PREV = true;

    private static final boolean NEXT = false;

    private double r;

    private double omega;

    private double stopdiff;

    private int N;

    private int ncol;

    private int nrow; /* number of rows and columns */

    private int lb;

    private int ub; /* lower and upper bound of grid stripe [lb ... ub] -> NOTE: ub is inclusive*/

    private int maxIters;

    private boolean reduceAlways;

    private boolean async;

    private boolean upcall;

    private int itersPerReduce;

    private int size;

    private int rank; /* process ranks */

    private double[][] g;

    private SendPort leftS;

    private SendPort rightS;

    private ReceivePort leftR;

    private ReceivePort rightR;

    private SendPort reduceS;

    private ReceivePort reduceR;

    private SendPort reduceSreduce;

    private ReceivePort[] reduceRreduce;

    private SendPort reduceSbcast;

    private ReceivePort reduceRbcast;

    private Syncer leftSyncer;

    private Syncer rightSyncer;

    final static boolean TIMINGS = TypedProperties.booleanProperty("timing",
            false);

    private Timer t_compute = Timer.createTimer();

    private Timer t_communicate = Timer.createTimer();

    private Timer t_reduce = Timer.createTimer();

    private PoolInfo info;

    private Ibis ibis;

    private Registry registry;

    private boolean finished = false;

    private Reducer reducer;

    SOR(int N, int maxIters, boolean reduceAlways, boolean async,
            boolean upcall, int itersPerReduce, boolean clusterReduce)
            throws IOException, IbisException {

        info = PoolInfo.createPoolInfo();

        if (N < info.size()) {
            /* give each process at least one row */
            if (info.rank() == 0) {
                System.out.println("Problem to small for number of CPU's");
            }
            System.exit(1);
        }
        if (maxIters ==
            0 && !reduceAlways) {
            if (info.rank() == 0) {
                System.out.println("Need to specify maxIters if -Dreduce=off");
            }
            System.exit(1);
        }

        this.N = N;
        nrow = N;
        ncol = N;
        this.maxIters = maxIters;
        this.reduceAlways = reduceAlways;
        this.async = async;
        this.upcall = upcall;
        this.itersPerReduce = itersPerReduce;

        rank = info.rank();
        size = info.size();

        createIbis();

        String name = "SOR" + rank;

        getBounds();

        createNeighbourPorts(name);
        if (clusterReduce) {
            reducer = new ClusterReducer(ibis, info);
        } else if (USE_O_N_BROADCAST) {
            reducer = new Reducer(ibis, info);
        } else {
            reducer = new TreeReducer(ibis, info);
        }

        // System.err.println(rank + ": hi, I'm connected...");

        if (rank == 0) {
            System.out.println("Starting SOR");
            System.out.println("");
            System.out.println("CPUs          : " + size);
            System.out.println("Matrix size   : " + nrow + "x" + ncol);
            System.out.println("Iterations    : "
                    + (maxIters > 0 ? ("" + maxIters) : "dynamic"));
            System.out.println("Reduce        : "
                    + (clusterReduce ? "cluster"
                            : (reduceAlways ? "on" : "off")));
            System.out.println("");
        }
    }

    private static void usage(String[] args) {

        PoolInfo info = PoolInfo.createPoolInfo();
        if (info.rank() == 0) {

            System.out.println("Usage: sor {<N> {<ITERATIONS>}}");
            System.out.println("");
            System.out.println("N x N   : (int, int). Problem matrix size");
            System.out
                    .println("ITERATIONS    : (int). Number of iterations to calculate. 0 means dynamic termination detection.");
            System.out.println("");

            for (int i = 0; i < args.length; i++) {
                System.out.println(i + " : " + args[i]);
            }
        }
    }

    private synchronized void cleanup() {
        if (finished) {
            return;
        }

        finished = true;

        try {
            if (leftS != null) {
                leftS.close();
                leftS = null;
            }

            if (rightS != null) {
                rightS.close();
                rightS = null;
            }

            if (leftR != null) {
                leftR.close();
                leftR = null;
            }

            if (rightR != null) {
                rightR.close();
                rightR = null;
            }

            reducer.end();

        } catch (Exception e) {
            System.out.println("Oops " + e);
            e.printStackTrace();

        } finally {
            try {
                ibis.end();
            } catch (IOException e) {
                // give up anyway
            }
        }
    }

    private void createIbis() throws IOException, IbisException {
        StaticProperties reqprops = new StaticProperties();

        reqprops.add("serialization", "data");
        reqprops.add("worldmodel", "closed");
        reqprops.add("communication",
                "OneToMany, OneToOne, ManyToOne, Reliable, ExplicitReceipt");
        // reqprops.add("communication", "OneToOne, Reliable, ExplicitReceipt");

        try {
            ibis = Ibis.createIbis(reqprops, null);
        } catch (NoMatchingIbisException e) {
            System.err
                    .println("Could not find an Ibis that can run this GMI implementation");
            System.exit(1);
        }

        Runtime.getRuntime().addShutdownHook(
                new Thread("SOR/Explicit ShutdownHook") {
                    public void run() {
                        cleanup();
                    }
                });

        registry = ibis.registry();
    }

    private void createNeighbourPorts(String name) throws IOException,
            IbisException {

        StaticProperties reqprops = new StaticProperties();

        reqprops.add("serialization", "data");
        reqprops.add("communication", "OneToOne, Reliable, ExplicitReceipt");

        PortType portTypeNeighbour = ibis.createPortType("SOR Neigbour",
                reqprops);

        if (rank != 0) {
            if (upcall) {
                leftSyncer = new Syncer(g[lb - 1]);
            }
            leftR = portTypeNeighbour.createReceivePort(name + "leftR",
                    leftSyncer);
            leftS = portTypeNeighbour.createSendPort(name + "leftS");
            leftR.enableConnections();

            // System.out.println(rank + " created " + name + "leftR and " + name + "leftS");
        }

        if (rank != size - 1) {
            if (upcall) {
                rightSyncer = new Syncer(g[ub]);
            }
            rightR = portTypeNeighbour.createReceivePort(name + "rightR",
                    rightSyncer);
            rightS = portTypeNeighbour.createSendPort(name + "rightS");
            rightR.enableConnections();

            // System.out.println(rank + " created " + name + "rightR and " + name + "rightS");
        }

        if (rank != 0) {
            // System.out.println(rank + " looking up " + "SOR" + (rank-1) + "rightR");

            ReceivePortIdentifier id = registry.lookupReceivePort("SOR"
                    + (rank - 1) + "rightR");
            // System.out.println(rank + " leftS = " + leftS + " id = " + id);
            leftS.connect(id);
        }

        if (rank != size - 1) {
            // System.out.println(rank + " looking up " + "SOR" + (rank+1) + "rightR");

            ReceivePortIdentifier id = registry.lookupReceivePort("SOR"
                    + (rank + 1) + "leftR");
            // System.out.println(rank + " rightS = " + rightS + " id = " + id);
            rightS.connect(id);
        }
    }

    private void getBounds() {
        // getBounds
        int n = N - 1;
        int nlarge = n % size;
        int nsmall = size - nlarge;

        int size_small = n / size;
        int size_large = size_small + 1;

        int temp_lb;

        if (rank < nlarge) {
            temp_lb = rank * size_large;
            ub = temp_lb + size_large;
        } else {
            temp_lb = nlarge * size_large + (rank - nlarge) * size_small;
            ub = temp_lb + size_small;
        }

        if (temp_lb == 0) {
            lb = 1; /* row 0 is static */
        } else {
            lb = temp_lb;
        }
        // System.err.println(rank + ": my slice [" + lb + "," + ub + ">");

        r = 0.5 * (Math.cos(Math.PI / (ncol)) + Math.cos(Math.PI / (nrow)));
        double temp_omega = 2.0 / (1.0 + Math.sqrt(1.0 - r * r));
        stopdiff = TOLERANCE / (2.0 - temp_omega);
        omega = temp_omega * 0.8; /* magic factor */

        g = createGrid();

        if (rank == 0) {
            System.out.println("Problem parameters");
            System.out.println("r       : " + r);
            System.out.println("omega   : " + omega);
            System.out.println("stopdiff: " + stopdiff);
            System.out.println("lb      : " + lb);
            System.out.println("ub      : " + ub);
            System.out.println("");
        }
    }

    private double[][] createGrid() {

        double[][] g = new double[nrow][];

        for (int i = lb - 1; i <= ub; i++) {
            // malloc the own range plus one more line
            // of overlap on each border
            g[i] = new double[ncol];
        }

        return g;
    }

    private void initGrid() {
        /* initialize the grid */
        for (int i = lb - 1; i <= ub; i++) {
            for (int j = 0; j < ncol; j++) {
                if (i == 0)
                    g[i][j] = 4.56;
                else if (i == nrow - 1)
                    g[i][j] = 9.85;
                else if (j == 0)
                    g[i][j] = 7.32;
                else if (j == ncol - 1)
                    g[i][j] = 6.88;
                else
                    g[i][j] = 0.0;
            }
        }
    }

    private double stencil(int row, int col) {
        return (g[row - 1][col] + g[row + 1][col] + g[row][col - 1] + g[row][col + 1]) / 4.0;
    }

    private boolean even(int i) {
        return i % 2 == 0;
    }

    private void reportTimings() {
        if (!TIMINGS) {
            return;
        }

        System.err.println(rank + ": t_compute " + t_compute.nrTimes()
                + " av.time " + t_compute.averageTime());
        System.err.println(rank + ": t_communicate " + t_communicate.nrTimes()
                + " av.time " + t_communicate.averageTime());
        System.err.println(rank + ": t_reduce " + t_reduce.nrTimes()
                + " av.time " + t_reduce.averageTime());
    }

    private void send(boolean dest, double[] col) throws IOException {

        /* Two cases here: sync and async */
        WriteMessage m;

        if (dest == PREV) {
            m = leftS.newMessage();
        } else {
            m = rightS.newMessage();
        }

        // System.err.print("Write col " + col); for (int i = 0; i < col.length; i++) { System.err.print(col[i] + " "); } System.err.println();
        m.writeArray(col);
        m.finish();
    }

    private static class Syncer implements Upcall {

        private boolean arrived = false;

        private boolean consumed = true;

        private double[] col;

        Syncer(double[] col) {
            this.col = col;
        }

        synchronized void consume() {
            while (!arrived) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // ignore
                }
                arrived = false;
                consumed = true;
                notify();
            }
        }

        synchronized public void upcall(ReadMessage m) throws IOException {
            while (!consumed) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // ignore
                }
                consumed = false;
                arrived = true;
            }
            m.readArray(col);
        }
    }

    private void receive(boolean source, double[] col) throws IOException {

        if (upcall) {
            Syncer syncer;
            if (source == PREV) {
                syncer = leftSyncer;
            } else {
                syncer = rightSyncer;
            }
            syncer.consume();

        } else {
            ReadMessage m;

            if (source == PREV) {
                m = leftR.receive();
            } else {
                m = rightR.receive();
            }

            m.readArray(col);
            // System.err.print("Read col " + col); for (int i = 0; i < col.length; i++) { System.err.print(col[i] + " "); } System.err.println();
            m.finish();
        }
    }

    private void send() throws IOException {
        if (TIMINGS)
            t_communicate.start();

        if (rank != 0) {
            send(PREV, g[lb]);
            // System.err.println(rank + ": S[" + lb + "]");
        }
        if (rank != size - 1) {
            send(NEXT, g[ub - 1]);
            // System.err.println(rank + ": S[" + (ub - 1) + "]");
        }

        if (TIMINGS)
            t_communicate.stop();
    }

    private void receive() throws IOException {
        if (TIMINGS)
            t_communicate.start();

        if (rank != size - 1) {
            receive(NEXT, g[ub]);
            // System.err.println(rank + ": R[" + ub + "]");
        }
        if (rank != 0) {
            receive(PREV, g[lb - 1]);
            // System.err.println(rank + ": R[" + (lb - 1) + "]");
        }

        if (TIMINGS)
            t_communicate.stop();
    }

    private void sendReceive() throws IOException {
        if (TIMINGS)
            t_communicate.start();

        if (even(rank)) {
            send();
            receive();
        } else {
            receive();
            send();
        }

        if (TIMINGS)
            t_communicate.stop();
    }

    private double compute(int color, int lb, int ub) {
        if (TIMINGS)
            t_compute.start();

        double maxdiff = 0.0;

        for (int i = lb; i < ub; i++) {
            // int d = (even(i) ^ phase) ? 1 : 0;
            int d = (i + color) & 1;
            for (int j = 1 + d; j < ncol - 1; j += 2) {
                double gNew = stencil(i, j);
                double diff = Math.abs(gNew - g[i][j]);

                if (diff > maxdiff) {
                    maxdiff = diff;
                }

                g[i][j] += omega * (gNew - g[i][j]);
            }
        }

        if (TIMINGS)
            t_compute.stop();

        return maxdiff;
    }

    public void start(String runName) throws IOException {

        long t_start, t_end; /* time values */
        double maxdiff;

        initGrid();

        // abuse the reduce as a barrier
        if (size > 1) {
            reducer.reduce(42.0);
        }

        if (rank == 0) {
            System.out.println("... and they're off !");
            System.out.flush();
        }

        if (TIMINGS) {
            t_compute.reset();
            t_communicate.reset();
            t_reduce.reset();
        }

        /* now do the "real" computation */
        t_start = System.currentTimeMillis();

        int iteration = 0;

        maxdiff = Double.MAX_VALUE;
        do {
            double diff = Double.MAX_VALUE;

            for (int color = 0; color < 2; color++) {
                if (async) {
                    send();
                } else {
                    sendReceive();
                }

                if (async) {
                    diff = compute(color, lb + 1, ub - 1);

                    receive();

                    diff = Math.max(diff, compute(color, lb, lb + 1));
                    diff = Math.max(diff, compute(color, ub - 1, ub));

                } else {
                    diff = compute(color, lb, ub);
                }
            }
            // System.err.print(rank + " ");

            if (size > 1 && (maxIters == 0 || reduceAlways)
                    && ((iteration + 1) % itersPerReduce == 0)) {

                if (TIMINGS)
                    t_reduce.start();
                maxdiff = reducer.reduce(diff);
                if (TIMINGS)
                    t_reduce.stop();
            } else {
                maxdiff = diff;
            }

            if (rank == 0) {
                // System.err.println(iteration + "");
                // System.err.print(".");
                System.out.print(".");
                System.out.flush();
            }

            iteration++;

        } while ((maxIters > 0) ? (iteration < maxIters)
                : (maxdiff > stopdiff));

        // Another barrier for simultaneous finish
        if (size > 1) {
            reducer.reduce(42.0);
        }

        t_end = System.currentTimeMillis();

        if (rank == 0) {
            System.out.println("application " + runName + " " + nrow + " x "
                    + ncol + " took " + ((t_end - t_start) / 1000.0) + " sec.");
            System.out.println("using " + iteration + " iterations,"
                    + " diff is " + maxdiff + " (allowed diff " + stopdiff
                    + ")");
        }

        if (!runName.equals("warmup")) {
            reportTimings();
        }
    }

    public static void main(String[] args) {
        try {
            /* set up problem size */

            int N = 1026;
            // int N = 200;
            int maxIters = -1;
            boolean warmup = false;
            boolean reduce = true;
            boolean async = false;
            boolean upcall = false;
            int itersPerReduce = 1;
            boolean clusterReduce = false;

            int options = 0;
            for (int i = 0; i < args.length; i++) {
                if (false) {
                } else if (args[i].equals("-warmup")) {
                    warmup = true;
                } else if (args[i].equals("-no-warmup")) {
                    warmup = false;
                } else if (args[i].equals("-reduce")) {
                    reduce = true;
                } else if (args[i].equals("-no-reduce")) {
                    reduce = false;
                } else if (args[i].equals("-async")) {
                    async = true;
                } else if (args[i].equals("-no-sync")) {
                    async = false;
                } else if (args[i].equals("-sync")) {
                    async = false;
                } else if (args[i].equals("-upcall")) {
                    upcall = false;
                } else if (args[i].equals("-reduce-fac")) {
                    ++i;
                    itersPerReduce = Integer.parseInt(args[i]);
                } else if (args[i].equals("-reduce-cluster")) {
                    clusterReduce = true;
                } else if (options == 0) {
                    N = Integer.parseInt(args[i]);
                    N += 2;
                    options++;
                } else if (options == 1) {
                    maxIters = Integer.parseInt(args[i]);
                    options++;
                } else {
                    usage(args);
                    System.exit(33);
                }
            }

            SOR sor = new SOR(N, maxIters, reduce, async, upcall,
                    itersPerReduce, clusterReduce);
            if (warmup) {
                sor.start("warmup");
            }
            sor.start("SOR");

            sor.cleanup();

        } catch (Exception e) {
            System.out.println("Oops " + e);
            e.printStackTrace();
        }
    }

}
