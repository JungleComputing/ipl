final class TranspositionTable {

    static final boolean SUPPORT_TT = true;

    static final int SIZE = 1 << 23;

    static long lookups = 0, hits = 0, sorts = 0, stores = 0, used = 0,
            overwrites = 0, visited = 0, scoreImprovements = 0, cutOffs = 0;

    int tagSize;

    int[] tags;

    short[] values = new short[SIZE];

    short[] bestChildren = new short[SIZE];

    byte[] depths = new byte[SIZE];

    boolean[] lowerBounds = new boolean[SIZE];

    boolean[] valid = new boolean[SIZE];

    TranspositionTable(int tagSize) {
        this.tagSize = tagSize;
        this.tags = new int[SIZE * tagSize];
    }

    int lookup(Tag tag) {
        lookups++;

        if (!SUPPORT_TT)
          return -1;

        int index = tag.hashCode() & (SIZE - 1);

        if (valid[index] && tag.equals(tags, index * tagSize))
          return index;

        return -1;
    }

    void store(Tag tag, short value, short bestChild, byte depth,
            boolean lowerBound) {
        stores++;

        if (!SUPPORT_TT)
            return;

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
        System.out.println("tt: lookups: " + lookups + ", hits: " + hits
                + ", sorts: " + sorts + ", stores: " + stores + ", used: "
                + used + ", overwrites: " + overwrites + ", score incs: "
                + scoreImprovements + ", cutoffs: " + cutOffs + ", visited: "
                + visited);
    }
}
