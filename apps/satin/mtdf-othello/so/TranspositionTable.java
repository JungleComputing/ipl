/* $Id$ */

import ibis.satin.SharedObject;

class Shutdown extends Thread {
    TranspositionTable tt;

    Shutdown(TranspositionTable tt) {
        this.tt = tt;
    }

    public void run() {
        tt.stats();
    }
}

final class TranspositionTable extends SharedObject implements
        TranspositionTableIntr {

    transient int lookups, hits, sorts, stores, used, overwrites, visited,
            scoreImprovements, cutOffs;

    private int SIZE;

    transient short[] values;

    transient short[] bestChildren;

    transient byte[] depths;

    transient boolean[] lowerBounds;

    transient private int tagSize;

    transient private int[] tags;

    transient private boolean[] valid;

    transient boolean inited;

    private void init() {
        if (!Mtdf.SUPPORT_TT) {
            SIZE = 0;
            this.tagSize = 0;
            tags = null;
            values = null;
            bestChildren = null;
            depths = null;
            lowerBounds = null;
            valid = null;
            System.err.println("TT: disabled");
            return;
        }

        this.tagSize = OthelloTag.SIZE;
        int elementSize = 7 + (4 * tagSize);

        Runtime r = Runtime.getRuntime();
        int procs = r.availableProcessors();
        long free = r.freeMemory();
        long max = r.maxMemory();
        long total = r.totalMemory();
        System.err.println("TT: " + procs + " processor(s), mem: free = "
            + free + " max = " + max + " total = " + total);

        long AppMem = 64 * 1024 * 1024;
        long toUse = (max / procs) - AppMem;
        long elts = toUse / elementSize;
        if (elts < 0) {
            System.err
                .println("No room to allocate a transposition table! Use the -Xmx option to the JVM");
            System.exit(1);
        }

        System.err.println("TT: we can use at most " + elts
            + " elements of size " + elementSize);

        int size = 1;
        int power = 0;
        while (true) {
            size <<= 1;
            power++;
            if (size > elts) {
                size >>= 1;
                power--;
                break;
            }
        }
        SIZE = size;
        int totalMem = (SIZE * elementSize) / (1024 * 1024);

        System.err.println("TT: table has " + SIZE + " entries (2^" + power
            + "), total mem used by table is " + totalMem + " MByte");

        values = new short[SIZE];
        bestChildren = new short[SIZE];
        depths = new byte[SIZE];
        lowerBounds = new boolean[SIZE];
        valid = new boolean[SIZE];
        tags = new int[SIZE * tagSize];

        inited = true;

        Runtime.getRuntime().addShutdownHook(new Shutdown(this));
    }

    int lookup(Tag tag) {
        if (!Mtdf.SUPPORT_TT) return -1;

        if (!inited) init();

        lookups++;

        int index = tag.hashCode() & (SIZE - 1);

        if (valid[index] && tag.equals(tags, index * tagSize)) return index;

        return -1;
    }

    void store(Tag tag, short value, short bestChild, byte depth,
        boolean lowerBound) {
        if (!Mtdf.SUPPORT_TT) return;

        int index = tag.hashCode() & (SIZE - 1);

        if (valid[index] && depth < depths[index]) return;

        if (depth >= Mtdf.REPLICATED_DEPTH) {
            sharedStore(index, tag, value, bestChild, depth, lowerBound);
        } else {
            localStore(index, tag, value, bestChild, depth, lowerBound);
        }
    }

    void localStore(int index, Tag tag, short value, short bestChild,
        byte depth, boolean lowerBound) {
        if (!inited) init();

        stores++;

        if (!valid[index]) used++;
        else overwrites++;

        tag.store(tags, index * tagSize);
        values[index] = value;
        bestChildren[index] = bestChild;
        depths[index] = depth;
        lowerBounds[index] = lowerBound;
        valid[index] = true;
    }

    /* shared object write method, is broadcast */
    public void sharedStore(int index, Tag tag, short value, short bestChild,
        byte depth, boolean lowerBound) {
        localStore(index, tag, value, bestChild, depth, lowerBound);
    }

    void stats() {
        System.err.println("tt: lookups: " + lookups + ", hits: " + hits
            + ", sorts: " + sorts + ", stores: " + stores + ", used: " + used
            + ", overwrites: " + overwrites + ", score incs: "
            + scoreImprovements + ", cutoffs: " + cutOffs + ", visited: "
            + visited);
    }
}
