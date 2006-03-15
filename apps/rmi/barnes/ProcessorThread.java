/* $Id$ */

import ibis.util.PoolInfo;

public class ProcessorThread extends Thread {

    GlobalData g;

    PoolInfo d;

    int myProc, numProcs;

    OrbTree orbTree;

    Procs p;

    private void GenerateBodiesPlummer() {
        int i;

        for (i = 0; i < g.gdTotNumBodies; i++) {
            g.gdBodies[i] = new Body();
            g.gdBodies[i].bWeight = 100;
        }
        g.gdPlummer.Generate(g.gdBodies, g.gdTotNumBodies);
    }

    private void PrintBody(int i) {
        g.debugStr("Body " + i + ": [ " + g.gdBodies[i].bPos.x + ", "
            + g.gdBodies[i].bPos.y + ", " + g.gdBodies[i].bPos.z + " ]");
        g.debugStr("     " + i + ": [ " + g.gdBodies[i].bVel.x + ", "
            + g.gdBodies[i].bVel.y + ", " + g.gdBodies[i].bVel.z + " ]");
        g.debugStr("     " + i + ": [ " + g.gdBodies[i].bAcc.x + ", "
            + g.gdBodies[i].bAcc.y + ", " + g.gdBodies[i].bAcc.z + " ]");
        g.debugStr("     " + i + ": [ " + g.gdBodies[i].bMass + " ]");
        //        g.debugStr("     " + i + ": " + g.gdBodies[i].bNumber);
    }

    private void PrintBodies() {

        int i;

        for (i = 0; i < g.gdNumBodies; i++)
            PrintBody(i);
    }

    void MainSetup() {

        g.InitializeBodies();

        g.debugStr("initializing Orb Tree");

        orbTree = new OrbTree(g);

        g.debugStr("initialized Orb Tree");

        try {

            g.gdNumBodies = 0;

            if (myProc == 0) {

                // generate bodies

                GenerateBodiesPlummer();

                g.debugStr("broadcasting bodies\n");

                g.Proc.broadcastBodies();

            } else {

                g.debugStr("receiving bodies\n");

                g.Proc.receiveBodies();
            }

        } catch (Exception e) {
            System.out
                .println(myProc + ": Exception caught, terminating! " + e);
            e.printStackTrace();
            System.exit(-1);
        }

        g.debugStr("totNumBodies: " + g.gdTotNumBodies + ", numBodies "
            + g.gdNumBodies);

    }

    void MainLoop() {

        BodyTree tree;
        long tStart, tTree, tCOFM, tAccels, tNewPos, tUpdateCOFM, tEssentialTree, tLoadBalance, totalMillis, interactions, totEssentialTree, totTree, totAccels, totNewPos, totUpdateCOFM, totCOFM, totLoadBalance, tGC, totGC;
        int level;

        if (g.GD_PRINT_BODIES) PrintBodies();

        totalMillis = 0;
        totEssentialTree = 0;
        totTree = 0;
        totAccels = 0;
        totNewPos = 0;
        totUpdateCOFM = 0;
        totCOFM = 0;
        totLoadBalance = 0;
        totGC = 0;

        try {

            for (g.gdIteration = 0; g.gdIteration < g.gdIterations; g.gdIteration++) {

                if (g.Proc.myProc == 0)
                    System.out.println("Computing iteration: " + g.gdIteration);

                g.debugStr("Computing iteration: " + g.gdIteration);

                tStart = System.currentTimeMillis();

                //	g.debugStr( "iteratie " + g.gdIteration + ", voor update, bodies: " + g.gdNumBodies );

                orbTree.Update();

                //	g.debugStr( "iteratie " + g.gdIteration + ", na update, bodies: " + g.gdNumBodies );

                // Balance the load

                if (g.gdIteration == 0) {
                    level = g.gdLogProcs;
                } else {
                    level = orbTree.determineLoadBalanceLevel();
                }

                g.debugStr("load balancing, level = " + level);

                orbTree.LoadBalance(level);

                //        g.debugStr("finished loadbalance (level: " + level + ")" );

                tLoadBalance = System.currentTimeMillis();

                // Load balancing finished, build the local tree...

                //        g.debugStr("Building tree");

                tree = new BodyTree(g, orbTree.getGlobalMin(), orbTree
                    .getGlobalMax());

                tTree = System.currentTimeMillis();

                tree.ComputeCenterOfMass();

                //	g.debugStr("CenterOfMass computed!");

                tCOFM = System.currentTimeMillis();
                /*
                 g.debugStr( "bodies: " + tree.dumpTree( 0, 10 ));
                 g.debugStr( "\n\n\n\n\n" );
                 */
                orbTree.ExchangeEssentialTree(tree);

                //      	g.debugStr("Essential trees updated");

                tEssentialTree = System.currentTimeMillis();

                tree.ComputeCenterOfMass();

                //	g.debugStr("CenterOfMass updated!");
                /*
                 g.debugStr( "bodies: " + tree.dumpTree( 0, 10 ));
                 g.debugStr( "\n\n\n\n\n" );
                 */
                tUpdateCOFM = System.currentTimeMillis();

                if (g.gdComputeAccelerationsDirect) interactions = tree
                    .ComputeAccelerationsDirect();
                else interactions = tree.ComputeAccelerationsBarnes();

                tAccels = System.currentTimeMillis();

                tree.ComputeNewPositions();

                tNewPos = System.currentTimeMillis();

                if ((g.gdGCInterval > 0)
                    && ((g.gdIteration % g.gdGCInterval) == 0)) {
                    tree = null;
                    System.gc();
                }

                tGC = System.currentTimeMillis();

                g.debugStr("\nIteration " + g.gdIteration + ":");
                g.debugStr("Load balancing: " + (tLoadBalance - tStart)
                    + " ms (level " + level + ")");
                g.debugStr("Tree construction: " + (tTree - tLoadBalance)
                    + " ms");
                g.debugStr("Center of mass computation: " + (tCOFM - tTree)
                    + " ms");
                g.debugStr("Essential tree transmission: "
                    + (tEssentialTree - tCOFM) + " ms ");
                g.debugStr("Center of mass update: "
                    + (tUpdateCOFM - tEssentialTree) + " ms");
                g.debugStr("Acceleration computation: "
                    + (tAccels - tUpdateCOFM) + " ms (" + interactions
                    + " interactions) ");
                g.debugStr("New position computation: " + (tNewPos - tAccels)
                    + " ms");
                g.debugStr("Explicit Garbage Collection: " + (tGC - tNewPos)
                    + " ms");

                g.debugStr("Total: " + (tGC - tStart) + " ms\n");

                if (g.gdIteration > 2) {
                    totalMillis += (tGC - tStart);
                    totLoadBalance += (tLoadBalance - tStart);
                    totTree += (tTree - tLoadBalance);
                    totCOFM += (tCOFM - tTree);
                    totEssentialTree += (tEssentialTree - tCOFM);
                    totUpdateCOFM += (tUpdateCOFM - tEssentialTree);
                    totAccels += (tAccels - tUpdateCOFM);
                    totNewPos += (tNewPos - tAccels);
                    totGC += (tGC - tNewPos);
                }
            }
        } catch (NullPointerException n) {

            System.out
                .println("Nullpointer exception caught (tree inconsistent),"
                    + " during iteration " + g.gdIteration + ": "
                    + n.getMessage());
            n.printStackTrace();
        }

        g.debugStr("stats; Bodies: " + g.gdTotNumBodies + ", Bodies per Leaf: "
            + g.gdMaxBodiesPerNode + ", Processors: " + g.gdNumProcs + "");

        g.debugStr("stats; Total time: " + totalMillis + " ms, average: "
            + (totalMillis / (g.gdIterations - 3)) + " ms per iteration");

        g.debugStr("stats; time spent in load balancing:              "
            + totLoadBalance + " ms ("
            + ((totLoadBalance * 1.0) / (totalMillis * 1.0) * 100.0)
            + " percent)");

        g.debugStr("stats; time spent in tree construction:           "
            + totTree + " ms ("
            + ((totTree * 1.0) / (totalMillis * 1.0) * 100.0) + " percent)");

        g.debugStr("stats; time spent in center of mass computation:  "
            + totCOFM + " ms ("
            + ((totCOFM * 1.0) / (totalMillis * 1.0) * 100.0) + " percent)");

        g.debugStr("stats; time spent in essential tree exchange:     "
            + totEssentialTree + " ms ("
            + ((totEssentialTree * 1.0) / (totalMillis * 1.0) * 100.0)
            + " percent)");

        g.debugStr("stats; time spent in center of mass update:  "
            + totUpdateCOFM + " ms ("
            + ((totUpdateCOFM * 1.0) / (totalMillis * 1.0) * 100.0)
            + " percent)");

        g.debugStr("stats; time spent in force computation:           "
            + totAccels + " ms ("
            + ((totAccels * 1.0) / (totalMillis * 1.0) * 100.0) + " percent)");

        g.debugStr("stats; time spent in position update:             "
            + totNewPos + " ms ("
            + ((totNewPos * 1.0) / (totalMillis * 1.0) * 100.0) + " percent)");

        g.debugStr("stats; time spent in explicit garbage collection: " + totGC
            + " ms (" + ((totGC * 1.0) / (totalMillis * 1.0) * 100.0)
            + " percent)");

        if (g.GD_PRINT_BODIES) PrintBodies();

        if (g.gdMyProc == 0) {
            System.err.println("Barnes, " + g.gdTotNumBodies + " bodies took "
                + totalMillis + " ms");
        }
    }

    // used for multithread runs
    ProcessorThread(GlobalData g, int numProcs, int myProc, Procs p) {

        this.g = g;
        this.p = p;
        this.myProc = myProc;
        this.numProcs = numProcs;

        g.gdMyProc = myProc;
        g.debugStr("Logfile for processor " + myProc + " of " + numProcs);

        try {
            g.Proc = new ProcessorImpl(g, numProcs, myProc, p);
        } catch (Exception e) {
            System.err.println("Exception: " + e);
        }
    }

    ProcessorThread(GlobalData g, PoolInfo d, Procs p) {

        this.g = g;
        this.p = p;
        this.d = d;
        this.myProc = d.rank();
        this.numProcs = d.size();

        g.gdMyProc = myProc;
        g.debugStr("Logfile for processor " + myProc + " of " + numProcs);

        //    g.debugStr("Creating Processor Implementation");

        if (ProcessorImpl.VERBOSE) {
            System.out.println("creating ProcImpl");
        }

        try {
            g.Proc = new ProcessorImpl(g, d, p);
        } catch (Exception e) {
            g.debugStr("exceptie gevangen!!!!!!!: " + e);
            e.printStackTrace();
            System.exit(-1);
        }
        if (ProcessorImpl.VERBOSE) {
            System.out.println("created ProcImpl");
        }

        //    g.debugStr("Created Processor Implementation");
    }

    public void run() {

        try {

            g.debugStr("ProcessorThread: running");

            g.Proc.register();

            g.debugStr("ProcessorThread: calling MainSetup");

            MainSetup();

            g.debugStr("ProcessorThread: starting MainLoop");

            MainLoop();

            g.debugStr("ProcessorThread: finished mainloop");

            g.Proc.cleanup();

        } catch (Exception e) {

            System.out.println("caught exception: " + e);
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
