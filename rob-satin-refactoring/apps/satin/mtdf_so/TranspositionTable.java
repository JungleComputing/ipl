/* $Id$ */


import java.util.*;
import java.io.*;
import java.net.*;

import ibis.util.PoolInfo;
import ibis.satin.*;

final class TranspositionTable extends SharedObject
    implements TranspositionTableInterface, Serializable {
    static final boolean SUPPORT_TT = true;

    static final int TT_BITS = 24;

    static final int SIZE = 1 << TT_BITS;

    static final int BUF_SIZE = 3000;

    static final int REPLICATED_DEPTH = 7; // @@@ experiment with this

    static long lookups = 0, hits = 0, sorts = 0, stores = 0, overwrites = 0,
            visited = 0, scoreImprovements = 0, cutOffs = 0, bcasts = 0;

    static final byte LOWER_BOUND = 1;

    static final byte UPPER_BOUND = 2;

    static final byte EXACT_BOUND = 3;

    int rank;

    int poolSize;
    
    Map table = new Hashtable();

    transient long initialMemory;

    static final double MEMORY_USAGE = 0.8;

    class TTShutdownHook extends Thread {
        public void run() {
            System.err.println("tt: lookups: " + lookups + ", hits: " + hits
                    + ", sorts: " + sorts + ", stores: " + stores
                    + ", overwrites: " + overwrites + ", score incs: "
                    + scoreImprovements + ", bcasts: " + bcasts + ", cutoffs: "
                    + cutOffs + ", visited: " + visited);
            System.err.println("Transposition table size was " + TT_BITS
                    + " bits = " + SIZE + " entries");
            System.err.println("Replicated depth >= " + REPLICATED_DEPTH);
        }
    }

    TranspositionTable() {
        Random random = new Random();
	PoolInfo info = null;

        try {
            info = PoolInfo.createPoolInfo();
        } catch (Exception e) {
            System.err.println("Error creating pool info: " + e);
            System.exit(1);
        }

        rank = info.rank();
        poolSize = info.size();

        Runtime.getRuntime().addShutdownHook(new TTShutdownHook());
	//	initialMemory = Runtime.getRuntime().freeMemory();
    }

    synchronized TTEntry lookup(long signature) {
        lookups++;
        if (SUPPORT_TT) {
            int index = (int) signature & (SIZE - 1);
            String key = "" + index;
//            TTEntry val = (TTEntry) SatinTupleSpace.peek(key);
	    Object val = table.get(key);
	    if (val != null && !(val instanceof TTEntry)) {
		System.err.println("bim bam:" + val.getClass().getName());
		}
	    
            return (TTEntry) val;
        } else {
            return null;
        }
    }

    void store(long tag, short value, short bestChild, byte depth,
            boolean lowerBound) {
        if (!SUPPORT_TT)
            return;

        stores++;

        int index = (int) tag & (SIZE - 1);
        String key = "" + index;
        //TTEntry oldVal = (TTEntry) SatinTupleSpace.peek(key);
	TTEntry oldVal = (TTEntry) table.get(key);

        // no need to store if the depth is smaller
        if (oldVal != null && depth < oldVal.depth) {
            return;
        }

        overwrites++;

        if (depth >= REPLICATED_DEPTH) {
            TTEntry val = new TTEntry(tag, value, bestChild, depth, lowerBound);
            //SatinTupleSpace.add(key, val);
	    broadcast_store(key, val);
	    bcasts++;
        }
    }
    
    /* write method */
    public void broadcast_store(String key, TTEntry val) {
	//	long free_mem = Runtime.getRuntime().freeMemory();
	//	double memory_usage = (initialMemory - free_mem) / initialMemory;
	//	if (memory_usage < MEMORY_USAGE) {
	    table.put(key, val);
	    //	} else {
	    //	    System.err.println("dropping entry");
	    //	}
    }

    /*    private void readObject(java.io.ObjectInputStream in) 
    throws java.io.IOException, ClassNotFoundException {
	in.defaultReadObject();
	initialMemory = Runtime.getRuntime().freeMemory();
	}*/
}
