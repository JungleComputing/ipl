package ibis.io;


final class IbisHash { 

    private static final boolean ASSERTS = false;
    private static final boolean STATS = false; // true; // false;
    private static final boolean TIMINGS = false; // true; // false;

    private static final int MIN_BUCKETS = 32;

    private static final int USE_NEW_BOUND = Integer.MAX_VALUE;

    /* Choose this value between 2 and 4 */
    private static final int RESIZE_FACTOR = 2;

    /* Choose this value between 1 and 3 */
    private static final int PRE_ALLOC_FACTOR = 1;

    // private static final int SHIFT1  = 5;
    // private static final int SHIFT2  = 16;
    private static final int SHIFT1  = 4;
    // private static final int SHIFT2  = 16;
    private static final int SHIFT3  = 16;

    private Object[]	dataBucket;
    private int[]	handleBucket;

    // if (STATS)
    private long contains;
    private long finds;
    private long rebuilds;
    private long collisions;
    private long rebuild_collisions;
    private long new_buckets;

    // if (TIMINGS)
    private ibis.util.Timer t_insert;
    private ibis.util.Timer t_find;
    private ibis.util.Timer t_rebuild;
    private ibis.util.Timer t_clear;

    private int offset = 0;
    private int size;
    private int initsize;
    private int alloc_size;
    private int present = 0;


    IbisHash() {
	this(MIN_BUCKETS);
    }

    IbisHash(int sz) {
	int x = 1;
	while (x < sz) {
	    x <<= 1;
	}
	if (x != sz) {
	    System.err.println("Warning: Hash table size (" + sz +
		    ") must be a power of two. Increment to " + x);
	    sz = x;
	}

	size = sz;
	initsize = sz;
	newBucketSet(initsize);

	if (STATS) {
	    contains = 0;
	    finds = 0;
	}
	if (TIMINGS) {
	    t_insert = new ibis.util.nativeCode.Rdtsc();
	    t_find = new ibis.util.nativeCode.Rdtsc();
	    t_rebuild = new ibis.util.nativeCode.Rdtsc();
	    t_clear = new ibis.util.nativeCode.Rdtsc();
	}
    }


    private void newBucketSet(int size) {
	dataBucket = new Object[size];
	handleBucket = new int[size];
	alloc_size = size;
    }


    private static final int hash_first(int b, int size) {
	// return ((b >>> SHIFT1) ^ (b >>> SHIFT2));
	// return ((b >>> SHIFT1) ^ (b & ((1 << SHIFT3) - 1))) & (size-1);
	return ((b >>> SHIFT3) ^ (b & ((1 << SHIFT3) - 1))) & (size-1);
	// // This is used in java.util.IdentityHashMap:
	// // return ((b << 1) - (b << 8)) & (size - 1);
	// return (b - (b << 7)) & (size - 1);
    }


    private static final int hash_second(int b) {
	return (b & ~0x1) + 12345;		/* some odd number */
	// Jason suggests +1 to improve cache locality
	// return 1;
	// return (b & 0xffe) + 1;	/* some SMALL odd number */
	// This is used in java.util.IdentityHashMap:
	// return 1;
    }

    /**
     * We know size is a power of two. Make mod cheap.
     */
    private static final int mod(int x, int size) {
	return (x & (size - 1));
    }


    // Don't call hashCode on the object. Some objects behave very strange :-)
    // If you don't understand this, think "ProActive".
    final int getHashCode(Object ref) {
	return System.identityHashCode(ref);
    }


    // synchronized
    final int find(Object ref, int hashCode) {

	int result;

	if (TIMINGS) {
	    t_find.start();
	}

	if (STATS) {
	    finds++;
	}

	int h0 = hash_first(hashCode, size);

	int ix = h0 + offset;
	Object b = dataBucket[ix];
	
	if (b == null) {
	    result = 0;

	} else if (b == ref) {
	    result = handleBucket[ix];

	} else {
	    int h1 = hash_second(hashCode);

	    do {
		h0 = mod(h0 + h1, size);
		ix = h0 + offset;
		b = dataBucket[ix];
	    } while (b != null && b != ref);

	    result = (b == null) ? 0 : handleBucket[ix];
	}

	if (TIMINGS) {
	    t_find.stop();
	}

	if (ASSERTS) {
	    if (result == 0) {
		for (int i = offset; i < offset + size; i++) {
		    if (dataBucket[i] == ref) {
			System.err.println("CORRUPTED HASH: find returns 'no' but it's there in bucket[" + i + "]");
		    }
		}
	    }
	}

	return result;
    }


    final int find(Object ref) {
	return find(ref, getHashCode(ref));
    }


    /**
     * Rebuild if present > 0.5 size
     */
    private final void rebuild() {

	if (TIMINGS) {
	    t_rebuild.start();
	}

	int n = size * RESIZE_FACTOR;

	int new_offset = 0;

	Object[] old_data = dataBucket;
	int[] old_handle = handleBucket;

	/* Only buy a new array when we really overflow.
	 * If the array we allocated previously still has enough
	 * free space, use first/last slice when we currently use
	 * last/first slice. */
	if (n + size > alloc_size) {
	    newBucketSet(PRE_ALLOC_FACTOR * n);
	} else if (offset == 0) {
	    new_offset = alloc_size - n;
	}

	for (int i = 0; i < size; i++) {
	    int ix = i + offset;
	    Object b = old_data[ix];
	    if (b != null) {
		int h = System.identityHashCode(b);
		int h0 = hash_first(h, n);
		while (dataBucket[h0 + new_offset] != null) {
		    int h1 = hash_second(h);
		    do {
			h0 = mod(h0 + h1, n);
			if (STATS) {
			    rebuild_collisions++;
			}
		    } while (dataBucket[h0 + new_offset] != null);
		}
		dataBucket[h0 + new_offset] = b;
		handleBucket[h0 + new_offset] = old_handle[ix];
		if (! ASSERTS) {
		    old_data[ix] = null;
		}
	    }
	}

	int old_offset = offset;
	int old_size = size;

	size = n;
	offset = new_offset;

	if (ASSERTS) {
	    for (int i = old_offset; i < old_offset + old_size; i++) {
		if (old_data[i] != null && find(old_data[i]) == 0) {
		    System.err.println("CORRUPTED HASH after rebuild: cannot find item[" + i + "] = " + Integer.toHexString(System.identityHashCode(old_data[i])));
		}
		old_data[i] = null;
	    }
	    int cont = 0;
	    for (int i = offset; i < offset + size; i++) {
		if (dataBucket[i] != null) {
		    cont++;
		}
	    }
	    if (cont != present) {
		System.err.println("CORRUPTED HASH after rebuild: present " + present + " but contains " + cont);
	    }
	}

	if (TIMINGS) {
	    t_rebuild.stop();
	}

	if (STATS) {
	    rebuilds++;
	}
    }


    private int put(Object ref, int handle, int hashCode, boolean lazy) {

	if (TIMINGS) {
	    t_insert.start();
	}

	int h0 = hash_first(hashCode, size);

	Object b = dataBucket[h0 + offset];

	if (b != null && b != ref) {
	    int h1 = hash_second(hashCode);
	    do {
		h0 = mod(h0 + h1, size);
		if (STATS) {
		    collisions++;
		}
		b = dataBucket[h0 + offset];
	    } while (b != null && b != ref);
	}

	if (lazy && b != null) {
	    return handleBucket[h0 + offset];
	}

	if (ASSERTS) {
	    if (lazy) {
		for (int i = offset; i < offset + size; i++) {
		    if (dataBucket[i] == ref) {
			System.err.println("CORRUPTED HASH: lazyPut finds 'no' but it's there in bucket[" + i + "]");
		    }
		}
	    }
	}

	dataBucket[h0 + offset] = ref;
	handleBucket[h0 + offset] = handle;

	present++;

	if (2 * present > size) {
	    rebuild();
	}

	if (TIMINGS) {
	    t_insert.stop();
	}

	if (STATS) {
	    contains++;
	}

	return handle;
    }


    // synchronized
    final void put(Object ref, int handle, int hashCode) {
	put(ref, handle, hashCode, false);
    }


    // synchronized
    final void put(Object ref, int handle) {
	put(ref, handle, getHashCode(ref));
    }


    // synchronized
    final int lazyPut(Object ref, int handle, int hashCode) {
	return put(ref, handle, hashCode, true);
    }


    // synchronized
    final int lazyPut(Object ref, int handle) {
	return lazyPut(ref, handle, getHashCode(ref));
    }


    // synchronized
    final void clear() {
	/**
	 * The hash is between 1/4 and 1/2 full.
	 * Cleaning is most efficient by doing a swipe over
	 * the whole bucket array.
	 */

	if (TIMINGS) {
	    t_clear.start();
	}

	if (size < USE_NEW_BOUND) {
	    for (int i = 0; i < size; i++) {
		dataBucket[i + offset] = null;
	    }
	} else {
// System.err.println("New bucket set...");
	    newBucketSet(initsize);
	}

	size = initsize;
	offset = 0;
	present = 0;

	if (TIMINGS) {
	    t_clear.stop();
	}

	if (STATS) {
	    // contains = 0;
	    // finds = 0;
	}
    }


    final void statistics() {
	if (STATS) {
	    System.err.println(this + ": " +
		    //  "buckets     = " + size +
		    " contains " + contains +
		    " finds " + finds +
		    " rebuilds " + rebuilds +
		    " new buckets " + new_buckets +
		    " collisions " + collisions +
		    " (rebuild " + rebuild_collisions +
		    ")");
	}
	if (TIMINGS) {
	    System.err.println(this + ": per insert: " +
		" insert(" + t_insert.nrTimes() + ") " +
		t_insert.format(t_insert.totalTimeVal() / t_insert.nrTimes()) +
		" find(" + t_find.nrTimes() + ") " +
		t_insert.format(t_find.totalTimeVal() / t_insert.nrTimes()) +
		" rebuild(" + t_rebuild.nrTimes() + ") " +
		t_insert.format(t_rebuild.totalTimeVal() / t_insert.nrTimes()) +
		" clear(" + t_clear.nrTimes() + ") " +
		t_insert.format(t_clear.totalTimeVal() / t_insert.nrTimes()));
	}
    }
}
