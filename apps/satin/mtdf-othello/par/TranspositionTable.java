import ibis.util.TypedProperties;

/* $Id$ */

final class TranspositionTable {

    private static final boolean SUPPORT_TT = TypedProperties.booleanProperty(
        "mtdf.tt", true);

    static long lookups = 0, hits = 0, sorts = 0, stores = 0, used = 0,
    overwrites = 0, visited = 0, scoreImprovements = 0, cutOffs = 0;

    final int SIZE;

    final int tagSize;

    final int[] tags;

    final short[] values;

    final short[] bestChildren;

    final byte[] depths;

    final boolean[] lowerBounds;

    final boolean[] valid;

    TranspositionTable(int tagSize) {
        this.tagSize = tagSize;
        int elementSize = 7 + (4 * tagSize);

        Runtime r = Runtime.getRuntime();
        int procs = r.availableProcessors();
        long free = r.freeMemory();
        long max = r.maxMemory();
        long total = r.totalMemory();
        System.err.println("TT: " + procs + " processor(s), mem: free = " + free + " max = " + max + " total = " + total);
        
        long AppMem = 64*1024*1024;
        long toUse = (max / procs) - AppMem;
        long elts = toUse / elementSize;
        if(elts < 0) {
            System.err.println("No room to allocate a transposition table! Use the -Xmx option to the JVM");
            System.exit(1);
        }
        
        System.err.println("TT: we can use at most " + elts + " elements of size " + elementSize);
        
        int size = 1;
        int power = 0;
        while(true) {
            size <<= 1;
            power++;
            if(size > elts) {
                size >>= 1;
                power--;
                break;
            }
        }
        SIZE = size;
        int totalMem = (SIZE * elementSize) / (1024 * 1024);
        
        System.err.println("TT: table has " + SIZE + " entries (2^" + power + "), total mem used by table is " + totalMem + " MByte");

        values = new short[SIZE];
        bestChildren = new short[SIZE];
        depths = new byte[SIZE];
        lowerBounds = new boolean[SIZE];
        valid = new boolean[SIZE];
        tags = new int[SIZE * tagSize];
    }
    
    int lookup(Tag tag) {
        if (!SUPPORT_TT)
          return -1;

        lookups++;

        int index = tag.hashCode() & (SIZE - 1);

        if (valid[index] && tag.equals(tags, index * tagSize))
          return index;

        return -1;
    }

    void store(Tag tag, short value, short bestChild, byte depth,
            boolean lowerBound) {
        if (!SUPPORT_TT)
            return;

        stores++;

        int index = tag.hashCode() & (SIZE - 1);

        if (valid[index] && depth < depths[index])
            return;

        if (!valid[index]) used++;
        else overwrites++;

        tag.store(tags, index * tagSize);
        values[index] = value;
        bestChildren[index] = bestChild;
        depths[index] = depth;
        lowerBounds[index] = lowerBound;
        valid[index] = true;
    }

    static void stats() {
        System.out.println("TT: lookups: " + lookups + ", hits: " + hits
                + ", sorts: " + sorts + ", stores: " + stores + ", used: "
                + used + ", overwrites: " + overwrites + ", score incs: "
                + scoreImprovements + ", cutoffs: " + cutOffs + ", visited: "
                + visited);
    }
}
