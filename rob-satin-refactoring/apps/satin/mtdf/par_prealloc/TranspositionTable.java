/* $Id$ */

final class TranspositionTable {

    static final boolean SUPPORT_TT = true;

    static final int SIZE = 1 << 22;

    static int lookups = 0, hits = 0, sorts = 0, stores = 0, overwrites = 0,
            visited = 0, scoreImprovements = 0, cutOffs = 0;

    TranspositionTableEntry[] entries = new TranspositionTableEntry[SIZE];

    TranspositionTable() {
        for (int i = 0; i < SIZE; i++) {
            entries[i] = new TranspositionTableEntry();
        }
    }

    TranspositionTableEntry lookup(long signature) {
        lookups++;
        return entries[(int) signature & (SIZE - 1)];
    }

    void store(long tag, short value, short bestChild, byte depth,
            boolean lowerBound) {
        stores++;

        if (!SUPPORT_TT)
            return;

        int index = (int) tag & (SIZE - 1);
        TranspositionTableEntry e = entries[index];

        if (depth < e.depth) {
            return;
        }
        overwrites++;

        e.tag = tag;
        e.value = value;
        e.bestChild = bestChild;
        e.depth = depth;
        e.lowerBound = lowerBound;
        entries[index] = e;
    }

    protected void finalize() throws Throwable {
        System.out.println("tt: lookups: " + lookups + ", hits: " + hits
                + ", sorts: " + sorts + ", stores: " + stores
                + ", overwrites: " + overwrites + ", score incs: "
                + scoreImprovements + ", cutoffs: " + cutOffs + ", visited: "
                + visited);
    }
}