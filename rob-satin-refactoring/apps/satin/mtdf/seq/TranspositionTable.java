/* $Id$ */

final class TranspositionTable {
    static final boolean SUPPORT_TT = true;

    static final int SIZE = 1 << 24;

    static long lookups = 0, stores = 0, overwrites = 0;

    static long hits = 0, visited = 0, sorts = 0, aborts = 0,
            scoreImprovements = 0, cutOffs = 0;

    TranspositionTableEntry[] entries = new TranspositionTableEntry[SIZE];

    TranspositionTableEntry lookup(long signature) {
        lookups++;
        if (SUPPORT_TT) {
            return entries[(int) signature & (SIZE - 1)];
        } else {
            return null;
        }
    }

    void store(long tag, short value, short bestChild, byte depth,
            boolean lowerBound) {
        stores++;

        if (!SUPPORT_TT)
            return;

        int index = (int) tag & (SIZE - 1);
        TranspositionTableEntry e = entries[index];

        if (e == null) {
            e = new TranspositionTableEntry();
        } else {
            if (depth < e.depth) {
                return;
            }
            overwrites++;
        }

        e.tag = tag;
        e.value = value;
        e.bestChild = bestChild;
        e.depth = depth;
        e.lowerBound = lowerBound;
        entries[index] = e;
    }

    void stats() {
        System.out.println("tt: lookups: " + lookups + ", hits: " + hits
                + ", sorts: " + sorts + ", stores: " + stores
                + ", overwrites: " + overwrites + ", score incs: "
                + scoreImprovements + ", cutoffs: " + cutOffs + ", visited: "
                + visited);
    }
}