package ibis.impl.net;

import ibis.util.TypedProperties;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Provide cached fixed-size memory block allocation.
 */
public final class NetAllocator {

    /**
     * Activate allocator stats.
     */
    private static final boolean STATISTICS = TypedProperties.booleanProperty(
            NetIbis.alloc_stats, false);

    /**
     * Activate alloc-without-free checking.
     */
    private static final boolean DEBUG = false;

    /**
     * Print on allocation of a new NetAllocator
     */
    private static final boolean VERBOSE = false;

    /**
     * The default size of the cache stack.
     */
    private static final int defaultMaxBlock = 16;

    /**
     * The default size of the cache stack if blocks are large.
     */
    private static final int defaultMaxBigBlock = 1;

    /**
     * If {link #blockSize} > {link #bigBlockThreshold} the maxBlock
     * value is set to {#link defaultMaxBigBlock} instead of
     * {#link defaultMaxBlock} to limit the risk of memory overflow.
     *
     * Judicially choose the mtu of net.gm as the default value...
     */
    private static final int bigBlockThreshold = TypedProperties.intProperty(
            NetIbis.alloc_thres, 128 * 1024);

    /**
     * Allocate buffers in chunks
     */
    private static final int BLOCK_CHUNK = 16;

    /**
     * Store the block caches.
     *
     * This map is indexed by <code>Integer(<block size>)</code>.
     */
    private static final HashMap stackMap = new HashMap();

    /**
     * Store the last backtrace associated with a block allocation.
     *
     * This map is indexed by the byte block reference.
     */
    private HashMap debugMap = null;

    /**
     * Provide a byte block caching stack.
     */
    private static class BlockStack {

        /**
         * Store the previously allocated blocks.
         *
         * The use of a stack as the data structure means that the last freed
         * block will be reallocated first.
         */
        byte[][] stack = null;

        /**
         * Store the number of blocks in the cache stack.
         */
        int stackPtr = 0;

        /**
         * The number of clients for this allocator size
         */
        int clients = 0;

        int warningIssued = 32 * bigBlockThreshold;
    }

    /**
     * A {@link ibis.impl.net.NetStat} for
     * {@linkplain ibis.impl.net.NetAllocator allocator} monitoring.
     */
    private static class NetAllocatorStat extends NetStat {

        /**
         * The number of blocks allocated while the cache {@link #stack} is
         * empty.
         */
        private int uncached_alloc = 0;

        /**
         * The number of blocks reallocated from the cache {@link #stack}.
         */
        private int cached_alloc = 0;

        /**
         * The number of blocks returned to cache {@link #stack}.
         */
        private int cached_free = 0;

        /**
         * The number of blocks discarded because the cache {@link #stack} was
         * full.
         */
        private int uncached_free = 0;

        /**
         * Store the stack trace at the allocator creation time for debugging
         * purpose.
         */
        String[] callerArray = null;

        /**
         * The constructor.
         *
         * @param on indicates whether the stat object is active or not.
         * @param moduleName is the name of the software entity the stat
         * 		object is monitoring.
         */
        public NetAllocatorStat(boolean on, String moduleName) {
            super(on, moduleName);

            if (DEBUG) {
                StackTraceElement[] steArray = new Throwable().getStackTrace();
                callerArray = new String[steArray.length];
                for (int i = 0; i < steArray.length; i++) {
                    callerArray[i] = steArray[i].toString();
                }
            }
        }

        /**
         * The constructor.
         *
         * @param on indicates whether the stat object is active or not.
         */
        public NetAllocatorStat(boolean on) {
            this(on, "");
        }

        /**
         * Uncached allocations monitoring.
         */
        public void incUncachedAlloc() {
            if (on) {
                uncached_alloc++;
            }
        }

        /**
         * Cached allocations monitoring.
         */
        public void incCachedAlloc() {
            if (on) {
                cached_alloc++;
            }
        }

        /**
         * Cached frees monitoring.
         */
        public void incCachedFree() {
            if (on) {
                cached_free++;
            }
        }

        /**
         * Uncached frees monitoring.
         */
        public void incUncachedFree() {
            if (on) {
                uncached_free++;
            }
        }

        /**
         * Report display.
         */
        public void report() {
            if (on) {
                String separator
                    = new String("____________________________________");

                System.err.println();
                System.err.println("Allocator stats " + moduleName);
                System.err.println(separator);

                reportVal(uncached_alloc, "uncached allocation");
                reportVal(cached_alloc, "cached allocation");
                reportVal(cached_free, "cached free");
                reportVal(uncached_free, "uncached free");

                if (DEBUG) {
                    if (uncached_alloc + cached_alloc > cached_free
                            + uncached_free) {
                        System.err.println("alloc/free mismatch: "
                                + (uncached_alloc + cached_alloc) + "/"
                                + (cached_free + uncached_free));
                        for (int i = 0; i < callerArray.length; i++) {
                            System.err.println("frame " + i + ": "
                                    + callerArray[i]);
                        }
                        System.err.println(separator);
                    }
                }

                System.err.println(" - ");

            }

        }

    }

    /**
     * The allocator's specific stat object.
     */
    private NetAllocatorStat stat = null;

    /**
     * The size of each memory block.
     */
    private int blockSize = 0;

    /**
     * The local reference to the caching data structure.
     *
     * The structure selected for that purpose is a stack implemented
     * in a fixed size array for several reasons:
     * <UL>
     * <LI> The use of a stack has the advantage that the most recently buffer
     * is reused first, which improves data locality.
     * <LI> The use of a fixed size array prevents memory leaks.
     * <LI> The use of a fixed size array seems to be much more efficient than
     * a Java 'Stack' object.
     * </UL>
     */
    private BlockStack stack = null;

    /**
     * Constructor allowing to select the block size and the cache
     * size of the allocator.
     *
     * @param blockSize size of the memory blocks provided by this allocator.
     */
    public NetAllocator(int blockSize) {

        if (blockSize < 1) {
            throw new IllegalArgumentException("invalid block size");
        }

        this.blockSize = blockSize;

        addClient();

        stat = new NetAllocatorStat(STATISTICS, "blockSize = " + blockSize
                + ", maxBlock = " + stack.stack.length);

        if (DEBUG) {
            debugMap = new HashMap();
            Runtime.getRuntime().addShutdownHook(
                    new Thread("NetAllocator ShutdownHook") {
                        public void run() {
                            synchronized (debugMap) {
                                shutdownHook();
                            }
                        }
                    });
        }

    }

    /**
     * Notify the allocator that it has one more client.
     * The allocator may adapt its cache size to the number of clients.
     */
    void addClient() {
        synchronized (stackMap) {

            Integer key = new Integer(blockSize);
            stack = (BlockStack) stackMap.get(key);
            int clients = (stack == null) ? 1 : (stack.clients + 1);

            int maxBlock = (blockSize > bigBlockThreshold) ? defaultMaxBigBlock
                    : defaultMaxBlock;

            if (maxBlock < 1) {
                throw new IllegalArgumentException(
                        "invalid maximum block number");
            }

            maxBlock = Math.max(maxBlock, clients);

            if (stack == null) {
                stack = new BlockStack();
                stack.stack = new byte[maxBlock][];
                if (VERBOSE) {
                    System.err.println(this + ": blockSize " + blockSize
                            + " new stack[" + maxBlock + "]");
                }
                stackMap.put(key, stack);
            } else if (stack.stack.length < maxBlock) {
                maxBlock = ((maxBlock + BLOCK_CHUNK - 1) / BLOCK_CHUNK)
                        * BLOCK_CHUNK;
                if (VERBOSE) {
                    System.err.println(this + ": blockSize " + blockSize
                            + " resize stack[" + maxBlock + "]");
                }
                // Upround maxBlock to a BLOCK_CHUNK-fold to decrease
                // realloc costs
                byte[][] s = new byte[maxBlock][];
                System.arraycopy(stack.stack, 0, s, 0, stack.stack.length);
                stack.stack = s;
                // Don't even have to replace stack in the stackMap
            } else {
                if (VERBOSE) {
                    System.err.println(this + ": blockSize " + blockSize
                            + " reuse stack[" + maxBlock + "]");
                }
            }
            if (maxBlock * blockSize > 2 * stack.warningIssued) {
                stack.warningIssued = maxBlock * blockSize;
                System.err.println(NetIbis.hostName()
                        + ": WARNING: buffered cache for blocksize "
                        + blockSize + " is going to exceed "
                        + (maxBlock * blockSize / (1 << 20)) + " MB");
            }

            stack.clients++;
        }
    }

    /**
     * Notify the allocator that it has one fewer clients.
     * The allocator may adapt its cache size to the number of clients.
     */
    void removeClient() {
        synchronized (stackMap) {
            stack.clients--;
        }
    }

    /**
     * Print some information at shutdown about the blocks that were not
     * freed, for debugging purpose.
     */
    protected void shutdownHook() {
        if (DEBUG) {
            Iterator i = debugMap.values().iterator();

            while (i.hasNext()) {
                String[] a = (String[]) i.next();

                System.err.println("unfreed blocks: " + blockSize + " byte"
                        + ((blockSize > 1) ? "s" : ""));
                System.err.println("---------------");
                for (int j = 0; j < a.length; j++) {
                    System.err.println("frame " + j + ": " + a[j]);
                }
                System.err.println("_______________");
            }

        }

    }

    /**
     * Returns the size of the blocks provided by this allocator.
     *
     * @return The allocator's block size.
     */
    public int getBlockSize() {
        return blockSize;
    }

    /**
     * Allocates a memory block.
     *
     * If no memory block is available in the cache, a new block
     * is allocated. Otherwise the most recently used block is selected.
     *
     * @return A memory block.
     */
    public byte[] allocate() {
        byte[] b = null;

        synchronized (stackMap) {
            if (stack.stackPtr == 0) {
                stat.incUncachedAlloc();
                b = new byte[blockSize];
            } else {
                stat.incCachedAlloc();

                stack.stackPtr--;
                b = stack.stack[stack.stackPtr];
            }
        }

        if (DEBUG) {
            StackTraceElement[] steArray = (new Throwable()).getStackTrace();
            String[] callerArray = new String[steArray.length];
            for (int i = 0; i < steArray.length; i++) {
                callerArray[i] = steArray[i].toString();
            }
            debugMap.put(b, callerArray);
        }

        return b;
    }

    /**
     * Frees a memory block.
     *
     * If the cache is full, the buffer is discarded, otherwise it
     * is added to the cache. The block length must equals the
     * allocator's block length. An invalid block length most
     * certainly indicates a program bug and causes the program to
     * be <STRONG>aborted</STRONG>.
     *
     * @param block the memory block to free.
     */
    public void free(byte[] block) {
        if (DEBUG) {
            Object o = debugMap.remove(block);
            if (o == null) {
                throw new Error("invalid block");
            }
        }

        synchronized (stackMap) {
            if (block.length != blockSize) {
                __.abort__("invalid block");
            }

            if (stack.stackPtr >= stack.stack.length) {
                stat.incUncachedFree();
                return;
            }

            stat.incCachedFree();

            stack.stack[stack.stackPtr] = block;
            stack.stackPtr++;
        }
    }

}
