import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;

import ibis.util.PoolInfo;
import ibis.util.Timer;

final class TranspositionTable extends UnicastRemoteObject implements
        TranspositionTableIntr {

    static final boolean SUPPORT_TT = true;

    static final int SIZE = 1 << 23;

    static final int BUF_SIZE = 10000;

    static int lookups = 0, hits = 0, sorts = 0, stores = 0, used = 0,
        overwrites = 0, visited = 0, scoreImprovements = 0, cutOffs = 0,
        bcasts = 0;

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

    TranspositionTableIntr[] others;

    static Timer bcastTimer = Timer.createTimer();

    TranspositionTable(int tagSize) throws RemoteException {
        this.tagSize = tagSize;
        this.tags = new int[SIZE * tagSize];
        this.buftags = new int[BUF_SIZE * tagSize];

        Registry r = null;

        PoolInfo info = PoolInfo.createPoolInfo();
        rank = info.rank();
        poolSize = info.size();

        System.err.println("hosts = " + poolSize + ", rank = " + rank);

        try {
            r = LocateRegistry.createRegistry(5555);
        } catch (Exception e) {
            System.err.println(rank + ": failed to create registry: " + e);
            try {
                Thread.sleep(1000);
            } catch (Exception x) {
            }
        }

        while (true) {
            try {
                r.bind("TT", this);
                break;
            } catch (Exception e) {
                System.err.println(rank + ": bind error: " + e);
                try {
                    Thread.sleep(1000);
                } catch (Exception x) { }
            }
        }
        System.err.println(rank + ": bound my remote object");

        others = new TranspositionTableIntr[poolSize - 1];
        int index = 0;
        for (int i = 0; i < poolSize; i++) {
            if (i != rank) {
                while (true) {
                    try {
                        others[index] = (TranspositionTableIntr) Naming
                                .lookup("//" + info.hostName(i) + ":5555/TT");
                        index++;
                        break;
                    } catch (Exception e) {
                        System.err
                                .println(rank + ": could not do lookup: " + e);
                        try {
                            Thread.sleep(1000);
                        } catch (Exception x) { }
                    }
                }
            }
        }

        System.err.println(rank + ": got all remote refs");
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

        // ok, send it off
        if (bufCount == BUF_SIZE) {
            bcastTimer.start();

            bcasts++;

            for (int i = 0; i < others.length; i++) {
                try {
                    others[i].remoteStore(bufindex, buftags, bufvalues,
                                bufbestChildren, bufdepths, buflowerBounds);
                } catch (Exception e) {
                    System.err.println("eek, rmi failed");
                    System.exit(1);
                }
            }

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
                + bcastTimer.averageTime() + ", cutoffs: " + cutOffs
                + ", visited: " + visited);
    }

    public synchronized void remoteStore(int[] aindex, int[] atag,
            short[] avalue, short[] abestChild, byte[] adepth,
            boolean[] alowerBound) throws RemoteException {

        for (int i = 0; i < aindex.length; i++) {
            doRemoteStore(aindex[i], atag, i * tagSize, avalue[i], abestChild[i],
                    adepth[i], alowerBound[i]);
        }
    }
}
