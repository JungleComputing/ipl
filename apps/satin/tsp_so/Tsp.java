/* $Id$ */

final class Tsp extends ibis.satin.SatinObject implements TspInterface,
        java.io.Serializable {
    static final int N_TOWNS = 11; /* Default nr of towns */

    static final int INIT_SEED = 1; /* Default random seed */

    static final int MAXX = 100;

    static final int MAXY = 100;

    static int seed = INIT_SEED;

    static boolean verbose = false;

    static final int THRESHOLD = 6;
    
    static int numMinimumUpdates = 0;

    static boolean present(int city, int hops, byte path[]) {
        for (int i = 0; i < hops; i++) {
            if (path[i] == city) {
                return true;
            }
        }

        return false;
    }

    /** Satin version of tsp.
     * We have to return the minimum, even though we use shared objects.
     * There is no consistency guarantee, so the root process might have missed
     * some updates when the calculation is ready.
     * The sequential version does not return the minimum, it uses the static variable
     * instead.
     *
     * The sequential version does not return the minimum, it uses the 
     * shared object instead. We assume read-your-writes consistency
     */
    public int spawn_tsp(int hops, byte[] path, int length, 
			 DistanceTable distance, Minimum minimum) {
        tsp(hops, path, length, distance, minimum);
	return minimum.get();
    }

    /** Search a TSP subtree that starts with initial route "path".
     * If partial route is longer than current best full route
     * then forget about this partial route.
     */
    public void tsp(int hops, byte[] path, int length, 
		    DistanceTable distance, Minimum minimum) {
        int city, dist, me;
        int NTowns = distance.dist.length;
	int mins[] = null;
	int spawned_counter = 0;

        if (length + distance.lowerBound[NTowns - hops] >= minimum.get()) {
            // stop searching, this path is too long...
            return;
        }

        if (hops == NTowns) {
            /* Found a full route better than current best route,
             * update minimum. */
            if (verbose) {
                System.out.print("found path ");
                for (int i = 0; i < NTowns; i++) {
                    System.out.print(path[i]);
                    System.out.print(" ");
                }
                System.out.println(" with length " + length);
            }

	    minimum.set(length);
            numMinimumUpdates++;
        }

	if (hops < THRESHOLD) {
	    mins = new int[NTowns];
	}

        /* "path" really is a partial route.
         * Call tsp recursively for each subtree. */
        me = path[hops - 1]; /* Last city of path */

        /* Try all cities that are not on the initial path,
         * in "nearest-city-first" order. */
	//        for (int i = 0; i < NTowns; i++) {
	if (hops < THRESHOLD && ibis.satin.impl.Satin.getSatin() != null) {
	    //go down if below threshold (satin evaluates jobs in reverse order)
	    for (int i = NTowns-1; i >= 0; i--) {
		city = distance.toCity[me][i];
		if (city != me && !present(city, hops, path)) {
		    dist = distance.dist[me][i];

		    /* For Satin, must copy path, or maybe just last city... */
		    byte[] newpath = (byte[]) path.clone();
		    newpath[hops] = (byte) city;
		    mins[spawned_counter] = spawn_tsp(hops + 1, newpath, 
						      length + dist, distance, minimum);
		    spawned_counter++;
		}
	    }
	    
	} else {
	    //go up if above threshold
	    for (int i = 0; i < NTowns; i++) {
		city = distance.toCity[me][i];
		if (city != me && !present(city, hops, path)) {
		    dist = distance.dist[me][i];

		    path[hops] = (byte) city;
		    tsp(hops + 1, path, length + dist,
			distance, minimum);
		}
	    }
	}

        if (hops < THRESHOLD) {
            sync();
	    /* do this in case some update is lost */
	    for (int i = 0; i < spawned_counter; i++) {
		if (mins[i] < minimum.get()) {
		    minimum.set(mins[i]);
		}
            }	   

        }
    }

    public static void main(String[] args) {
        long start, end;
        double time;
        int option = 0;
        int NTowns = N_TOWNS;
        int global_minimum = Integer.MAX_VALUE;
        Tsp tsp = new Tsp();
        boolean testing = args.length == 0;

        for (int i = 0; i < args.length; i++) {
            if (false) {
            } else if (args[i].equals("-seed")) {
                seed = Integer.parseInt(args[++i]);
            } else if (args[i].equals("-bound")) {
                global_minimum = Integer.parseInt(args[++i]);
            } else if (args[i].equals("-v")) {
                verbose = true;
            } else if (option == 0) {
                NTowns = Integer.parseInt(args[i]);
                option++;
            } else {
                System.err.println("No such option: " + args[i]);
                System.exit(1);
            }
        }

        if (option > 1) {
            System.err.println("Too many options, usage tsp [nr cities]");
            System.exit(1);
        }

        byte[] path = new byte[NTowns];
        path[0] = (byte) 0; /* start with city 0 */
        int length = 0;
	
	DistanceTable distance = new DistanceTable(NTowns,seed);
	distance.exportObject();

        /* When no bound is specified, init minimum to first path. */
        if (global_minimum == Integer.MAX_VALUE) {
            global_minimum = 0;
            for (int i = 0; i < NTowns - 1; i++) {
                global_minimum += distance.dist[i][i + 1];
            }
        }

        System.out.println("tsp " + NTowns + " seed = " + seed + ", bound = "
                + global_minimum + (verbose ? "verbose" : ""));

	Minimum minimum = new Minimum();	
	minimum.set(global_minimum);
	minimum.exportObject();
        start = System.currentTimeMillis();
        int result = tsp.spawn_tsp(1, path, length, distance, minimum);
        tsp.sync();
        end = System.currentTimeMillis();

        time = (double) end - start;
        time /= 1000.0;

        System.out.println("application time tsp (" + NTowns + ") took " + time
                + " s");
        System.out.println("application result tsp (" + NTowns + ") = "
                + result);
        if (testing) {
            if (result != 218) {
                System.out.println("Test failed!");
                System.exit(1);
            }
            System.out.println("Test succeeded!");
        }
        System.out.println("num minimum updates: " + numMinimumUpdates);
    }
}
