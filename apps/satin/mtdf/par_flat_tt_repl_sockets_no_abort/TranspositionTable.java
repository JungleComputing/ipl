/* $Id$ */


import java.util.Random;
import java.io.*;
import java.net.*;

import ibis.util.PoolInfo;
import ibis.util.Timer;

final class TranspositionTable {
    static final boolean SUPPORT_TT = true;

    static final int TT_BITS = 24;

    static final int SIZE = 1 << TT_BITS;

    static final int BUF_SIZE = 3000;

    static final int REPLICATED_DEPTH = 7; // @@@ experiment with this

    static long lookups = 0, hits = 0, sorts = 0, stores = 0, overwrites = 0,
            visited = 0, scoreImprovements = 0, cutOffs = 0, bcasts = 0;

    static final byte LOWER_BOUND = 1;

    static final byte UPPER_BOUND = 2;

    static final byte EXACT_BOUND = 3;

    long[] tags = new long[SIZE]; // index bits are redundant...

    short[] values = new short[SIZE];

    short[] bestChildren = new short[SIZE];

    byte[] depths = new byte[SIZE];

    boolean[] lowerBounds = new boolean[SIZE];

    int bufCount = 0;

    int[] bufindex = new int[BUF_SIZE];

    long[] buftags = new long[BUF_SIZE]; // index bits are redundant...

    short[] bufvalues = new short[BUF_SIZE];

    short[] bufbestChildren = new short[BUF_SIZE];

    byte[] bufdepths = new byte[BUF_SIZE];

    boolean[] buflowerBounds = new boolean[BUF_SIZE];

    DataOutputStream bcastStream;

    Timer bcastTimer = Timer.createTimer();

    Timer bcastHandlerTimer = Timer.createTimer();

    PoolInfo info = PoolInfo.createPoolInfo();

    int rank = info.rank();

    int poolSize = info.size();

    TTReceiver[] receiverThreads = new TTReceiver[poolSize];

    TranspositionTable() {
        Random random = new Random();

        //		System.err.println("hosts = " + poolSize + ", rank = " + rank);

        try {
            ServerSocket serv = new ServerSocket(5555);
            Socket[] sockets = new Socket[poolSize];

            for (int i = 0; i < rank; i++) {
                acceptConnection(serv, sockets);
            }

            for (int i = rank + 1; i < poolSize; i++) {
                sockets[i] = makeConnection(i);
            }

            OutputStreamSplitter splitter = new OutputStreamSplitter();
            for (int i = 0; i < poolSize; i++) {
                if (i == rank)
                    continue;

                splitter.add(sockets[i].getOutputStream());
            }

            //			System.err.println(rank + ": all connected!!!");

            BufferedOutputStream bo = new BufferedOutputStream(splitter,
                    1024 * 1024);
            bcastStream = new DataOutputStream(bo);
            //			bcastStream.flush();

            for (int i = 0; i < poolSize; i++) {
                if (i == rank)
                    continue;
                receiverThreads[i] = new TTReceiver(this, sockets[i]);
                //				receiverThreads[i].setDaemon(true);
                receiverThreads[i].start();
            }
        } catch (Exception e) {
            System.err.println("Exc: " + e);
        }

        System.err.println(rank + ": TT created");
    }

    void acceptConnection(ServerSocket serv, Socket[] sockets) {
        Socket s;

        try {
            //			System.err.println(rank + ": accepting from 5555");
            s = serv.accept();
            //			System.err.println(rank + ": accepted");
            s.setTcpNoDelay(true);
            InputStream in = s.getInputStream();
            int other = in.read();
            sockets[other] = s;
            //			System.err.println(rank + ": accepted socket, other = " + other);
        } catch (Exception e) {
            System.err.println("Exc: " + e);
        }
    }

    Socket makeConnection(int dest) {
        Socket s;

        while (true) {
            try {
                //				System.err.println(rank + ": connecting to " + dest);
                s = new Socket(info.hostName(dest), 5555);
                //				System.err.println(rank + ": connecting to " + dest + " succ");
                s.setTcpNoDelay(true);

                OutputStream out = s.getOutputStream();
                out.write((byte) rank);
                out.flush();
                //				System.err.println(rank + ": connecting to " + dest + " DONE");
                break;
            } catch (Exception e) {
                //				System.err.print(".");
                //				System.err.println("Exc: "+e);
                try {
                    Thread.sleep(1000);
                } catch (Exception x) {
                }
            }
        }

        return s;
    }

    synchronized int lookup(long signature) {
        lookups++;
        if (SUPPORT_TT) {
            int index = (int) signature & (SIZE - 1);
            if (depths[index] > 0)
                return index;
            return -1;
        } else {
            return -1;
        }
    }

    void store(long tag, short value, short bestChild, byte depth,
            boolean lowerBound) {
        if (!SUPPORT_TT)
            return;

        // give receiver threads some time...
        if (poolSize > 1)
            Thread.yield();

        int index = (int) tag & (SIZE - 1);

        boolean forward = localStore(index, tag, value, bestChild, depth,
                lowerBound);

        if (forward && depth >= REPLICATED_DEPTH && poolSize > 1) {
            forwardStore(index, tag, value, bestChild, depth, lowerBound);
        }
    }

    synchronized boolean localStore(int index, long tag, short value,
            short bestChild, byte depth, boolean lowerBound) {
        stores++;

        if (depth < depths[index]) {
            return false;
        }

        overwrites++;

        tags[index] = tag;
        values[index] = value;
        bestChildren[index] = bestChild;
        depths[index] = depth;
        lowerBounds[index] = lowerBound;

        return true;
    }

    void doRemoteStore(int index, long tag, short value, short bestChild,
            byte depth, boolean lowerBound) {
        stores++;

        if (depth < depths[index]) {
            return;
        }

        overwrites++;

        tags[index] = tag;
        values[index] = value;
        bestChildren[index] = bestChild;
        depths[index] = depth;
        lowerBounds[index] = lowerBound;
    }

    void exit() {
        try {
            bcastStream.writeByte(-1);
            bcastStream.flush();
            bcastStream.close();
        } catch (Exception e) {
            System.out.println("Exc on exit: " + e);
        }
    }

    private void forwardStore(int index, long tag, short value,
            short bestChild, byte depth, boolean lowerBound) {
        bufindex[bufCount] = index;
        buftags[bufCount] = tag;
        bufvalues[bufCount] = value;
        bufbestChildren[bufCount] = bestChild;
        bufdepths[bufCount] = depth;
        buflowerBounds[bufCount] = lowerBound;
        bufCount++;

        // ok, send it off
        if (bufCount == BUF_SIZE) {
            //			System.err.print(rank);
            //			System.err.println(rank + ": doing bcast of tt buf");
            bcastTimer.start();
            bcasts++;

            try {
                bcastStream.writeByte(1);
                for (int i = 0; i < BUF_SIZE; i++) {
                    bcastStream.writeInt(bufindex[i]);
                }

                for (int i = 0; i < BUF_SIZE; i++) {
                    bcastStream.writeLong(buftags[i]);
                }

                for (int i = 0; i < BUF_SIZE; i++) {
                    bcastStream.writeShort(bufvalues[i]);
                }

                for (int i = 0; i < BUF_SIZE; i++) {
                    bcastStream.writeShort(bufbestChildren[i]);
                }

                //				bcastStream.write(bufdepths, 0, BUF_SIZE);

                for (int i = 0; i < BUF_SIZE; i++) {
                    bcastStream.writeByte(bufdepths[i]);
                }

                for (int i = 0; i < BUF_SIZE; i++) {
                    bcastStream.writeBoolean(buflowerBounds[i]);
                }
                bcastStream.flush();
            } catch (Exception e) {
                System.err.println("Exc: " + e);
            }

            bufCount = 0;
            bcastTimer.stop();
            //			System.err.println(rank + ": bcast of tt buf DONE");
        }
    }

    protected void finalize() throws Throwable {
        System.err.println("tt: lookups: " + lookups + ", hits: " + hits
                + ", sorts: " + sorts + ", stores: " + stores
                + ", overwrites: " + overwrites + ", score incs: "
                + scoreImprovements + ", bcasts: " + bcasts + ", bcast time: "
                + bcastTimer.totalTime() + ", bcast avg: "
                + bcastTimer.averageTime() + ", bcast handler time: "
                + bcastHandlerTimer.totalTime() + ", bcast handler avg: "
                + bcastHandlerTimer.averageTime() + ", cutoffs: " + cutOffs
                + ", visited: " + visited);
        System.err.println("Transposition table size was " + TT_BITS
                + " bits = " + SIZE + " entries");
        System.err.println("Replicated depth >= " + REPLICATED_DEPTH);
    }

    public synchronized void remoteStore(int[] aindex, long[] atag,
            short[] avalue, short[] abestChild, byte[] adepth,
            boolean[] alowerBound) {
        //		System.err.println("received bcast of tt buf");
        bcastHandlerTimer.start();
        for (int i = 0; i < aindex.length; i++) {
            doRemoteStore(aindex[i], atag[i], avalue[i], abestChild[i],
                    adepth[i], alowerBound[i]);
        }
        bcastHandlerTimer.stop();
        //		System.err.println("received bcast of tt buf handled");
    }
}