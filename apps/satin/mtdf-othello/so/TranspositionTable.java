import ibis.satin.so.SharedObject;

final class TranspositionTable extends SharedObject implements
        TranspositionTableIntr {

    private static final boolean SUPPORT_TT = true;

    private static final int SIZE = 1 << 23;

    transient int lookups, hits, sorts, stores, used, overwrites, visited,
            scoreImprovements, cutOffs;

    transient short[] values;
    transient short[] bestChildren;
    transient byte[] depths;
    transient boolean[] lowerBounds;

    transient private int tagSize;
    transient private int[] tags;
    transient private boolean[] valid;

    transient boolean inited;

    private void init() {
        values = new short[SIZE];
        bestChildren = new short[SIZE];
        depths = new byte[SIZE];
        lowerBounds = new boolean[SIZE];
        valid = new boolean[SIZE];

        tagSize = OthelloBoard.getTagSize();
        tags = new int[SIZE * tagSize];

        inited = true;
    }

    int lookup(Tag tag) {
        if (!SUPPORT_TT) return -1;

        if (!inited) init();

        lookups++;

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

    void localStore(int index, Tag tag, short value,
            short bestChild, byte depth, boolean lowerBound) {
        if (!inited) init();

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
            + scoreImprovements + ", cutoffs: " + cutOffs + ", visited: "
            + visited);
    }
}
