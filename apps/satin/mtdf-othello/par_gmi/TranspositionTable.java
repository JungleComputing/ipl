/* $Id$ */

import ibis.gmi.*;
import ibis.util.Timer;

final class TranspositionTable extends GroupMember implements
        TranspositionTableIntr {

    static final boolean SUPPORT_TT = true;

    static final int SIZE = 1 << 23;

    static final int BUF_SIZE = 10000;

    static int lookups = 0, hits = 0, sorts = 0, stores = 0, used = 0,
        overwrites = 0, visited = 0, scoreImprovements = 0, cutOffs = 0,
        bcasts = 0;

    static Timer bcastTimer = Timer.createTimer();

    static Timer rstoreTimer = Timer.createTimer();

    int poolSize;

    int rank;

    int tagSize;

    int[] tags;

    short[] values = new short[SIZE];

    short[] bestChildren = new short[SIZE];

    byte[] depths = new byte[SIZE];

    boolean[] lowerBounds = new boolean[SIZE];

    boolean[] valid = new boolean[SIZE];

    int bufCount = 0;

    int[] bufindex = new int[BUF_SIZE];

    int[] buftags;

    short[] bufvalues = new short[BUF_SIZE];

    short[] bufbestChildren = new short[BUF_SIZE];

    byte[] bufdepths = new byte[BUF_SIZE];

    boolean[] buflowerBounds = new boolean[BUF_SIZE];

    TranspositionTableIntr group;

    TranspositionTable(int tagSize) {
        this.tagSize = tagSize;
        this.tags = new int[SIZE * tagSize];
        this.buftags = new int[BUF_SIZE * tagSize];
    }

    void setGroup(TranspositionTableIntr group) {
        this.group = group;
    }

    synchronized int lookup(Tag tag) {
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
        if (!SUPPORT_TT)
            return;

        int index = tag.hashCode() & (SIZE - 1);

        if (localStore(index, tag, value, bestChild, depth, lowerBound)) {
            forwardStore(index, tag, value, bestChild, depth, lowerBound);
        }
    }

    synchronized boolean localStore(int index, Tag tag, short value,
            short bestChild, byte depth, boolean lowerBound) {
        stores++;

        if (valid[index] && depth < depths[index]) {
            return false;
        }

        if (!valid[index]) used++;
        else overwrites++;

        tag.store(tags, index * tagSize);
        values[index] = value;
        bestChildren[index] = bestChild;
        depths[index] = depth;
        lowerBounds[index] = lowerBound;
        valid[index] = true;

        return true;
    }

    void doRemoteStore(int index, int[] tagArray, int tagIndex, short value,
            short bestChild, byte depth, boolean lowerBound) {
        stores++;

        if (valid[index] && depth < depths[index]) {
            return;
        }

        if (!valid[index]) used++;
        else overwrites++;

        System.arraycopy(tagArray, tagIndex, tags, index * tagSize, tagSize);
        values[index] = value;
        bestChildren[index] = bestChild;
        depths[index] = depth;
        lowerBounds[index] = lowerBound;
        valid[index] = true;
    }

    private void forwardStore(int index, Tag tag, short value, short bestChild,
            byte depth, boolean lowerBound) {
        bufindex[bufCount] = index;
        tag.store(buftags, bufCount * tagSize);
        bufvalues[bufCount] = value;
        bufbestChildren[bufCount] = bestChild;
        bufdepths[bufCount] = depth;
        buflowerBounds[bufCount] = lowerBound;
        bufCount++;

        if (bufCount == BUF_SIZE) {
            bcastTimer.start();

            bcasts++;

            group.remoteStore(new TranspositionTableWrapper(Group.rank(),
                    bufindex, buftags, bufvalues, bufbestChildren, bufdepths,
                    buflowerBounds));

            bufCount = 0;

            bcastTimer.stop();
        }
    }

    static void stats() {
        System.err.println("tt: lookups: " + lookups + ", hits: " + hits
                + ", sorts: " + sorts + ", stores: " + stores + ", used: "
                + used + ", overwrites: " + overwrites + ", score incs: "
                + scoreImprovements + ", bcasts: " + bcasts + ", bcast time: "
                + bcastTimer.totalTime() + ", bcast avg: "
                + bcastTimer.averageTime() + ", rstore time: "
                + rstoreTimer.totalTime() + ", rstore avg: "
                + rstoreTimer.averageTime() + ", cutoffs: " + cutOffs
                + ", visited: " + visited);
    }

    public void remoteStore(TranspositionTableWrapper container) {
        if (container.rank == Group.rank()) return;

        rstoreTimer.start();

        for (int i = 0; i < container.index.length; i++) {
            doRemoteStore(container.index[i], container.tag, i*4,
                    container.value[i], container.bestChild[i],
                    container.depth[i], container.lowerBound[i]);
        }

        rstoreTimer.stop();
    }
}
