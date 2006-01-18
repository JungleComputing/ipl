/* $Id$ */

//import javax.swing.*;

import java.util.*;
import ibis.satin.*;

final class BarnesHut extends SatinObject implements BarnesHutInterface {

    static boolean debug = false; //use -(no)debug to modify

    static boolean verbose = false; //use -v to turn on

    static final boolean ASSERTS = false; //also used in other barnes classes

    //number of bodies at which we work sequentially
    private static int spawn_threshold_bodies = 500; //use -t <threshold> to modify

    //true: Collect statistics about the various phases in each iteration
    public static boolean phase_timing = true; //use -(no)timing to modify

    //use the necessary tree implementation instead of shared objects
    public static boolean ntc_impl = false;

    private static long totalTime = 0;

    private static long btcomTime = 0, updateTime = 0;

    private static long[] forceCalcTimes;

    //Parameters for the BarnesHut algorithm / simulation
    private static final double THETA = 5.0; //cell subdivision tolerance

    static final double DT = 0.025; //integration time-step, used in Bodies

    //we do 7 iterations
    static final double START_TIME = 0.0;

    static final double END_TIME = 0.175;

    static int ITERATIONS = 7;

    private static int numBodies;

    private static int maxLeafBodies;

    BarnesHut() {}

    private static void processLinkedListResult(LinkedList result, double[] all_x,
            double[] all_y, double[] all_z) {
        Iterator it = result.iterator();
        int[] bodyNumbers;
        double[] tmp_x, tmp_y, tmp_z;
        int i;

        while (it.hasNext()) {
            bodyNumbers = (int[]) it.next();
            tmp_x = (double[]) it.next();
            tmp_y = (double[]) it.next();
            tmp_z = (double[]) it.next();

            for (i = 0; i < bodyNumbers.length; i++) {
                all_x[bodyNumbers[i]] = tmp_x[i];
                all_y[bodyNumbers[i]] = tmp_y[i];
                all_z[bodyNumbers[i]] = tmp_z[i];
		/*                if (ASSERTS)
				  bodies.bodyArray[bodyNumbers[i]].updated = true;*/
            }
        }
    }

    /**
     * adds all items in results[x] (x < lastValidIndex) to
     * results[lastValidIndex]
     * @return a reference to results[lastValidIndex], for convenience
     */
    static LinkedList combineResults(LinkedList[] results,
            int lastValidIndex) {

        if (BarnesHut.ASSERTS && lastValidIndex < 0) {
            System.err.println("BodyTreeNode.combineResults: EEK! "
                    + "lvi < 0! All children are null in caller!");
            System.exit(1);
        }

        for (int i = 0; i < lastValidIndex; i++) {
            if (results[i] != null) {
                results[lastValidIndex].addAll(results[i]);
            }
        }

        return results[lastValidIndex];
    }

    /**
     * @param suboptimal A list with (a lot of) bodyNumber and acc arrays
     * @return A list with only one bodyNumber and acc array, containing
     *         all the elements of these arrays in 'suboptimal'
     *         If all arrays in suboptimal are empty, an empty list is returned
     */
    private static LinkedList optimizeList(LinkedList suboptimal) {
        LinkedList optimal;
        Iterator it;
        int totalElements = 0, position;

        int[] allBodyNrs, bodyNrs = null;
        double[] allAccs_x, allAccs_y, allAccs_z;
        double[] accs_x = null, accs_y = null, accs_z = null;

        it = suboptimal.iterator(); //calculate totalElements
        while (it.hasNext()) {
            bodyNrs = (int[]) it.next();
            it.next(); // skip accs_x, accs_y, accs_z
            it.next();
            it.next();
            totalElements += bodyNrs.length;
        }

        if (totalElements == 0)
            return new LinkedList(); //nothing to optimize

        allBodyNrs = new int[totalElements];
        allAccs_x = new double[totalElements];
        allAccs_y = new double[totalElements];
        allAccs_z = new double[totalElements];

        position = 0;
        it = suboptimal.iterator();
        while (it.hasNext()) {
            bodyNrs = (int[]) it.next();
            accs_x = (double[]) it.next();
            accs_y = (double[]) it.next();
            accs_z = (double[]) it.next();

            System.arraycopy(bodyNrs, 0, allBodyNrs, position, bodyNrs.length);
            System.arraycopy(accs_x, 0, allAccs_x, position, accs_x.length);
            System.arraycopy(accs_y, 0, allAccs_y, position, accs_y.length);
            System.arraycopy(accs_z, 0, allAccs_z, position, accs_z.length);

            position += bodyNrs.length;
        }

        optimal = new LinkedList();
        optimal.add(allBodyNrs);
        optimal.add(allAccs_x);
        optimal.add(allAccs_y);
        optimal.add(allAccs_z);

        return optimal;
    }

    public boolean guard_computeForces(byte[] nodeId, int iteration,
				       int threshold, Bodies bodies) {
	if (bodies.iteration+1 != iteration) {
	    return false;
	} else {
	    return true;
	}
    }
    

    /*spawnable*/
    public LinkedList computeForces(byte[] nodeId, int iteration,
				    int threshold, Bodies bodies) {
	int lastValidChild = -1;
	LinkedList result, result1;
	BodyTreeNode treeNode; 
	boolean spawned = false;

	treeNode = bodies.findTreeNode(nodeId);
	if (treeNode.children == null || treeNode.bodyCount < threshold) {
	    /*it is a leaf node, do sequential computation*/
	    return treeNode.computeForcesSequentially(bodies.bodyTreeRoot);
	} 

	LinkedList res[] = new LinkedList[8];
	for (int i = 0; i < 8; i++) {
	    BodyTreeNode ch = treeNode.children[i];
	    if (ch != null) {
		if (ch.children == null) {
		    res[i] = ch.computeForcesSequentially(bodies.bodyTreeRoot);
		} else {
		    /*spawn child jobs*/
		    byte[] newNodeId = new byte[nodeId.length + 1];
		    System.arraycopy(nodeId, 0, newNodeId, 0, nodeId.length);
		    newNodeId[nodeId.length] = (byte) i;
		    res[i] = /*spawn*/ computeForces(newNodeId, iteration,
						     threshold, bodies);
		    spawned = true;
		}
		lastValidChild = i;
	    }
	}
	if (spawned) {
	    /*if we spawned, we have to sync*/
	    sync();
	    return combineResults(res, lastValidChild);
	} else {
	    return optimizeList(combineResults(res, lastValidChild));
	}
    }	

    /*spawnable*/
    public LinkedList computeForcesNoSO(BodyTreeNode treeNode, BodyTreeNode interactTree, 
					int threshold) {

	int lastValidChild = -1;
	LinkedList result;
	boolean spawned = false;
	
	if(treeNode.children == null || treeNode.bodyCount < threshold) {
	    return treeNode.computeForcesSequentially(interactTree);
	} 

	LinkedList[] res = new LinkedList[8];

	for (int i = 0; i < 8; i++) {
	    BodyTreeNode ch = treeNode.children[i];
	    if (ch != null) {
		if (ch.children == null) {
		    res[i] = ch.computeForcesSequentially(interactTree);
		} else {
		    //necessaryTree creation
		    BodyTreeNode necessaryTree = (ch == interactTree) ? interactTree
			: new BodyTreeNode(interactTree, ch);
		    res[i] = /*spawn*/ computeForcesNoSO(ch, necessaryTree, threshold);
		    spawned = true;
		}
		lastValidChild = i;
	    }
	}
	if (spawned) {
	    sync();
	    result = combineResults(res, lastValidChild);
	} else {
	    //this was a sequential job, optimize!
	    result = optimizeList(combineResults(res, lastValidChild));
	}

	return result;
    }


    private static void printBodies(Object bod) {
        Body b;
        int i;

        Body[] sorted = new Body[numBodies];
	if (!ntc_impl) {
	    Bodies bodies = (Bodies) bod;
	    System.arraycopy(bodies.bodyArray, 0, sorted, 0, numBodies);
	} else {
	    BodiesNoSO bodiesNoSO = (BodiesNoSO) bod;
	    System.arraycopy(bodiesNoSO.bodyArray, 0, sorted, 0, numBodies);
	}

        Arrays.sort(sorted); //sort the copied bodyArray (by bodyNumber)

        for (i = 0; i < numBodies; i++) {
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

    private static void parseArguments(String[] argv) {
        boolean numBodiesSeen = false;
        boolean mlbSeen = false;
     
        for (int i = 0; i < argv.length; i++) {
            //options
            if (argv[i].equals("-debug")) {
                debug = true;
            } else if (argv[i].equals("-nodebug")) {
                debug = false;
            } else if (argv[i].equals("-v")) {
                verbose = true;
            } else if (argv[i].equals("-t")) {
                spawn_threshold_bodies = Integer.parseInt(argv[++i]);
                if (spawn_threshold_bodies < 0)
                    throw new IllegalArgumentException(
                            "Illegal argument to -t: Spawn threshold must be >= 0 !");
            } else if (argv[i].equals("-iter")) {
                ITERATIONS = Integer.parseInt(argv[++i]);
                if (ITERATIONS < 0)
                    throw new IllegalArgumentException(
                            "Illegal argument to -iter: must be >= 0 !");
            } else if (argv[i].equals("-timing")) {
                phase_timing = true;
            } else if (argv[i].equals("-no_timing")) {
                phase_timing = false;
	    } else if (argv[i].equals("-ntc_impl")) {
		ntc_impl = true;
            } else if (!numBodiesSeen) {
                try {
                    numBodies = Integer.parseInt(argv[i]); //nr of bodies to simulate
                    numBodiesSeen = true;
                } catch (NumberFormatException e) {
                    System.err.println("Illegal argument: " + argv[i]);
                    System.exit(1);
                }
            } else if (!mlbSeen) {
                try {
                    maxLeafBodies = Integer.parseInt(argv[i]); //max bodies per leaf node
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

        if (numBodies < 1) {
            System.err.println("Invalid body count, generating 100 bodies...");
            numBodies = 100;
        }
    }

    public static void main(String[] argv) {

	BarnesHut barnesHut = new BarnesHut();
	Bodies bodies = null; //for shared objects implementation
	BodiesNoSO bodiesNoSO = null; //for necessary tree implementation
        long realStart, realEnd;
        long start = 0, end, phaseStart = 0, phaseEnd;
	byte[] rootNodeIdentifier = new byte[0];
        double[] accs_x;
        double[] accs_y;
        double[] accs_z;
	LinkedList result; 

        realStart = System.currentTimeMillis();
	parseArguments(argv);

        System.out.println("BarnesHut: simulating " + numBodies + " bodies, "
			   + maxLeafBodies + " bodies/leaf node, " + "theta = " + THETA
			   + ", spawn-threshold-bodies = " + spawn_threshold_bodies);
	
	ibis.satin.SatinObject.pause();
	if (!ntc_impl) {
	    bodies = new Bodies(numBodies, maxLeafBodies, THETA);
	    bodies.exportObject();
	} else {
	    bodiesNoSO = new BodiesNoSO(numBodies, maxLeafBodies, THETA);
	}

	
	accs_x = new double[numBodies];
	accs_y = new double[numBodies];
	accs_z = new double[numBodies];

	forceCalcTimes = new long[ITERATIONS];

        System.out.println("Iterations: " + ITERATIONS);

	ibis.satin.SatinObject.resume();

        start = System.currentTimeMillis();

	for (int iteration = 0; iteration < ITERATIONS; iteration++) {

            System.out.println("Starting iteration " + iteration);

            if (phase_timing) {
                phaseStart = System.currentTimeMillis();
            }

	    if (!ntc_impl) {
		result = /*spawn*/ barnesHut.computeForces(rootNodeIdentifier, 
							   iteration, spawn_threshold_bodies, 
							   bodies);
	    } else {
		result = /*spawn*/ barnesHut.computeForcesNoSO(bodiesNoSO.bodyTreeRoot,
							       bodiesNoSO.bodyTreeRoot,
							       spawn_threshold_bodies);
	    }
	    barnesHut.sync();

            processLinkedListResult(result, accs_x, accs_y, accs_z);
	    ibis.satin.SatinObject.pause();

            if (phase_timing) {
                phaseEnd = System.currentTimeMillis();
                forceCalcTimes[iteration] = phaseEnd - phaseStart;
            }

	    if (phase_timing) {
		phaseStart = System.currentTimeMillis();
	    }
	    
	    if (!ntc_impl) {
		if (iteration < ITERATIONS-1) {
		    bodies.updateBodies(accs_x, accs_y, accs_z, iteration);
		} else {
		    bodies.updateBodiesLocally(accs_x, accs_y, accs_z, iteration);
		}
	    } else {
		bodiesNoSO.updateBodies(accs_x, accs_y, accs_z, iteration);
	    }

            if (phase_timing) {
                phaseEnd = System.currentTimeMillis();
                btcomTime += phaseEnd - phaseStart;
	    }
	    ibis.satin.SatinObject.resume();
	    
	}

        end = System.currentTimeMillis();
        totalTime = end - start;

        System.out.println("application barnes took "
                + (double) (totalTime / 1000.0) + " s");

        if (phase_timing) {
            long total = 0;
            System.out.println("tree building and CoM computation took: "
                    + btcomTime / 1000.0 + " s");
	    /*   if (impl != IMPL_TUPLE2) {
                System.out.println("                  Updating bodies took: "
                        + updateTime / 1000.0 + " s");
			}*/
            System.out.println("Force calculation took: ");
            for (int i = 0; i < ITERATIONS; i++) {
                System.out.println("  iteration " + i + ": "
                        + forceCalcTimes[i] / 1000.0 + " s");
                total += forceCalcTimes[i];
            }
            System.out
                    .println("               total: " + total / 1000.0 + " s");
        }

	if (verbose) {
            System.out.println();
	    if (!ntc_impl) {
		printBodies(bodies);
	    } else {
		printBodies(bodiesNoSO);
	    }
	}

        realEnd = System.currentTimeMillis();
	        ibis.satin.SatinObject.resume(); //allow satin to exit cleanly

        System.out.println("Real run time = " + (realEnd - realStart) / 1000.0
                + " s");
    }
}
