/* $Id$ */

import ibis.satin.SatinTupleSpace;

final class TranspositionTable {
    static final boolean SUPPORT_TT = true;

    static final int SIZE = 1 << 23;

    static long lookups = 0, hits = 0, sorts = 0, stores = 0, used = 0,
            overwrites = 0, visited = 0, scoreImprovements = 0, cutOffs = 0;

    static int minDepth =
            ibis.util.TypedProperties.intProperty("mtdf.tt.min_depth", 5);

    TranspositionTableEntry lookup(Tag tag) {
        lookups++;

        if (!SUPPORT_TT)
            return null;

        String hash = "" + (tag.hashCode() & (SIZE - 1));

        TranspositionTableEntry entry =
            (TranspositionTableEntry)SatinTupleSpace.peek(hash);

        if (entry != null && entry.tag.equals(tag))
            return entry;

        return null;
    }

    void store(Tag tag, short value, short bestChild, byte depth,
            boolean lowerBound) {
        stores++;

        if (!SUPPORT_TT)
            return;

        String hash = "" + (tag.hashCode() & (SIZE - 1));

        TranspositionTableEntry entry =
            (TranspositionTableEntry)SatinTupleSpace.peek(hash);

        if (entry != null && depth < entry.depth)
            return;

        if (entry == null) {
            used++;
            entry = new TranspositionTableEntry(tag, value, bestChild, depth,
                                                                lowerBound);
        }
        else {
            overwrites++;
            entry.set(tag, value, bestChild, depth, lowerBound);
        }

        if (depth >= minDepth)
            SatinTupleSpace.add(hash, entry);
    }

    static void stats() {
        System.out.println("tt: lookups: " + lookups + ", hits: " + hits
                + ", sorts: " + sorts + ", stores: " + stores + ", used: "
                + used + ", overwrites: " + overwrites + ", score incs: "
                + scoreImprovements + ", cutoffs: " + cutOffs + ", visited: "
                + visited);
    }
}
