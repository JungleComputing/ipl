package ibis.ipl.impl.net;

import ibis.ipl.impl.net.__;

/**
 * Provides cached fixed-size memory block allocation.
 */
public final class NetAllocator {

	/**
	 * The default size of the cache.
	 */
	private static final int      defaultMaxBlock = 16;

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
			return new byte[blockSize];
		}

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

		if (stackPtr >= defaultMaxBlock) 
			return;

		stack[stackPtr] = block;
		stackPtr++;
	}
}
