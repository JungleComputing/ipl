import ibis.satin.so.SharedObject;

final class TranspositionTable extends SharedObject implements
        TranspositionTableIntr {

    static final boolean SUPPORT_TT = true;

    static final int SIZE = 1 << 23;

    static final int BUF_SIZE = 10000;

    int lookups, hits, sorts, stores, used, overwrites,
            visited, scoreImprovements, cutOffs;

    int poolSize;

    int rank;

    int tagSize;

    int[] tags;

    short[] values = new short[SIZE];

    short[] bestChildren = new short[SIZE];

    byte[] depths = new byte[SIZE];

    boolean[] lowerBounds = new boolean[SIZE];

    boolean[] valid = new boolean[SIZE];

    int bufCount;

    TranspositionTable(int tagSize) {
        this.tagSize = tagSize;
        this.tags = new int[SIZE * tagSize];
    }

    synchronized int lookup(Tag tag) {
        lookups++;

        if (!SUPPORT_TT) return -1;

        int index = tag.hashCode() & (SIZE - 1);

        if (valid[index] && tag.equals(tags, index * tagSize)) return index;

        return -1;
    }

    void store(Tag tag, short value, short bestChild, byte depth,
            boolean lowerBound) {
        if (!SUPPORT_TT) return;

        int index = tag.hashCode() & (SIZE - 1);

        if (valid[index] && depth < depths[index]) return;

        sharedStore(index, tag, value, bestChild, depth, lowerBound);
    }

    synchronized void localStore(int index, Tag tag, short value,
            short bestChild, byte depth, boolean lowerBound) {
        stores++;

        if (!valid[index])
            used++;
        else
            overwrites++;

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
            + scoreImprovements 
            +  ", cutoffs: " + cutOffs
            + ", visited: " + visited);
    }
}
