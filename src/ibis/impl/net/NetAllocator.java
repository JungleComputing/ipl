package ibis.ipl.impl.net;

import ibis.ipl.impl.net.__;

/**
 * Provides cached fixed-size memory block allocation.
 */
public final class NetAllocator {

        /**
         * Activate allocator stats.
         */
	private static final boolean	STATISTICS = false;

	/**
	 * The default size of the cache.
	 */
	private static final int      defaultMaxBlock = 16;


        public class NetAllocatorStat extends NetStat {
                private int uncached_alloc = 0;
                private int cached_alloc   = 0;
                private int cached_free    = 0;
                private int uncached_free  = 0;

                public NetAllocatorStat(boolean on, String moduleName){
                        super(on, moduleName);
                }
                
                public NetAllocatorStat(boolean on) {
                        this(on, "");
                }

                public void incUncachedAlloc() {
                        if (on) {
                                uncached_alloc++;
                        }
                }

                public void incCachedAlloc() {
                        if (on) {
                                cached_alloc++;
                        }
                }
                
                public void incCachedFree() {
                        if (on) {
                                cached_free++;
                        }
                }
                
                public void incUncachedFree() {
                        if (on) {
                                uncached_free ++;
                        }
                }
                
                public void report() {
                        if (on) {
                                System.err.println();
                                System.err.println("Allocator stats "+moduleName);
                                System.err.println("------------------------------------");

                                reportVal(uncached_alloc,   "uncached allocation");
                                reportVal(cached_alloc,     "cached allocation");
                                reportVal(cached_free,      "cached free");
                                reportVal(uncached_free,    "uncached free");
                        }
                
                }
                
        }
        
	
        private NetAllocatorStat      stat = null;

	/**
	 * The size of each memory block.
	 */
	private 	     int      blockSize       = 0;

	/**
	 * The caching data structure.
	 *
	 * The structure selected for that purpose is a stack implemented
	 * in a fixed size array for several reasons:
	 * <UL>
	 * <LI> The use of a stack has the advantage that the most recently  buffer
	 * is reused first which improves data locality.
	 * <LI> The use of a fixed size array prevents memory leaks.
	 * <LI> The use of a fixed size array seems to be much more efficient than
	 * a Java 'Stack' object.
	 * </UL>
	 */
	private 	     byte[][] stack           = null;

	/**
	 * The stack pointer.
	 */
	private 	     int      stackPtr        = 0;

	/**
	 * Constructor allowing to select the block size and the cache
	 * size of the allocator.
	 *
	 * @param blockSize The size of the memory blocks provided by this allocator.
	 * @param maxBlock  The maximum number of blocks to be cached.
	 */
	public NetAllocator(int blockSize, int maxBlock) {                
                if (blockSize < 1) {
                        throw new Error("invalid block size");
                }
                
                if (maxBlock < 1) {
                        throw new Error("invalid maximum block number");
                }
                
                stat = new NetAllocatorStat(STATISTICS, "blockSize = "+blockSize+", maxBlock = "+maxBlock);

		this.blockSize = blockSize;
		stack = new byte[maxBlock][];
	}

	/**
	 * Constructor allowing to select the block size of the allocator and using the
	 * {@linkplain #defaultMaxBlock default cache size}.
	 *
	 * @param blocksize The size of the memory blocks provided by this allocator.
	 */
	public NetAllocator(int blockSize) {
		this(blockSize, defaultMaxBlock);
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
	public synchronized byte[] allocate() {
		if (stackPtr == 0) {
                        stat.incUncachedAlloc();
			return new byte[blockSize];
		}

                stat.incCachedAlloc();

		stackPtr--;
		return stack[stackPtr];
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
	public synchronized void free(byte[] block) {
		if (block.length != blockSize) {
			__.abort__("invalid buffer");
		}

		if (stackPtr >= defaultMaxBlock)  {
                        stat.incUncachedFree();
			return;
		}

                stat.incCachedFree();

		stack[stackPtr] = block;
		stackPtr++;
	}

}
