package ibis.io;


final class Bucket {        	
    Object	data;
    int		handle;
    int		hashCode;
    Bucket	next;		// Maintain free list
} 

final class IbisHash { 

    static final boolean STATS = false;

    static final int MIN_BUCKETS = 32;
    // static final int SHIFT1  = 5;
    // static final int SHIFT2  = 16;
    static final int SHIFT1  = 4;
    // static final int SHIFT2  = 16;
    static final int SHIFT3  = 16;

    private Bucket cache;

    private static int contains, finds, rebuilds, collisions, rebuild_collisions, new_buckets;
    private Bucket [] bucket;
    private int offset = 0;
    private int size;
    private int initsize;
    private int alloc_size;
    private int present = 0;


    IbisHash() {
	this(MIN_BUCKETS);
    }

    IbisHash(int sz) {
	alloc_size = sz;
	size = sz;
	initsize = sz;
	bucket = new Bucket[sz];
	if (STATS) contains = finds = 0;
    }

    private final Bucket getBucket() {
	Bucket b = cache;

	if (b == null) {
	    b = new Bucket();
	    if (STATS) new_buckets++;
	}
	else {
	    cache = b.next;
	}

	return b;
    }


    private final void putBucket(Bucket b) {
	b.next = cache;
	b.data = null;
	cache = b;
    }


    private static final int hash_first(int b, int size) {
	// return ((b >>> SHIFT1) ^ (b >>> SHIFT2));
	// return ((b >>> SHIFT1) ^ (b & ((1 << SHIFT3) - 1))) & (size-1);
	return ((b >>> SHIFT3) ^ (b & ((1 << SHIFT3) - 1))) & (size-1);
    }


    private static final int hash_second(int b) {
	return (b & ~0x1) + 12345;		/* some odd number */
    }

    /**
     * We know size is a power of two. Make mod cheap.
     */
    private static final int mod(int x, int size) {
	return (x & (size - 1));
    }


    // synchronized
    final int find(Object ref, int h) {

	if (STATS) finds++;

	int h0 = hash_first(h, size);

	Bucket b = bucket[h0 + offset];
	
	if (b == null) return 0;
	if (b.data == ref) return b.handle;

	int h1 = hash_second(h);

	do {
	    h0 = mod(h0 + h1, size);
	    b = bucket[h0 + offset];
	} while (b != null && b.data != ref);

	return b == null ? 0 : b.handle;
    }


    final int find(Object ref) {
	// Don't call hashCode on the object. Some objects behave very strange :-)
	// If you don't understand this, think "ProActive".
	return find(ref, System.identityHashCode(ref));
    }


    /**
     * Rebuild if present > 0.5 size
     */
    private final void rebuild() {

	int n = size * 4;

	int new_offset = 0;
	Bucket[] new_bucket = bucket;

	/* Only buy a new array when we really overflow.
	 * If the array we allocated previously still has enough
	 * free space, use first/last slice when we currently use
	 * last/first slice. */
	if (n + size > alloc_size) {
	    alloc_size = 3 * n;
	    new_bucket = new Bucket[alloc_size];
	} else if (offset == 0) {
	    new_offset = alloc_size - n;
	}

	for (int i = 0; i < size; i++) {
	    Bucket b = bucket[i + offset];
	    if (b != null) {
		int h = b.hashCode;
		int h0 = hash_first(h, n);
		while (new_bucket[h0 + new_offset] != null) {
		    int h1 = hash_second(h);
		    do {
			h0 = mod(h0 + h1, n);
			if (STATS) rebuild_collisions++;
		    } while (new_bucket[h0 + new_offset] != null);
		}
		new_bucket[h0 + new_offset] = b;
		bucket[i + offset] = null;
	    }
	}

	bucket = new_bucket;
	size = n;
	offset = new_offset;

	if (STATS) rebuilds++;
    }


    // synchronized
    final void put(Object ref, int handle, int h) {

	int h0 = hash_first(h, size);

	Bucket b = bucket[h0 + offset];

	if (b != null && b.data != ref) {
	    int h1 = hash_second(h);
	    do {
		h0 = mod(h0 + h1, size);
		if (STATS) collisions++;
		b = bucket[h0 + offset];
	    } while (b != null && b.data != ref);
	}

	if (b == null) {
	    b = getBucket();
	}

	b.data     = ref;
	b.handle   = handle;
	b.hashCode = h;

	bucket[h0 + offset] = b;

	present++;

	if (2 * present > size) {
	    rebuild();
	}

	if (STATS) contains++;
    }


    final void put(Object ref, int handle) {
	put(ref, handle, System.identityHashCode(ref));
    }


    // synchronized
    final void clear() {
	/**
	 * The hash is between 1/4 and 1/2 full.
	 * Cleaning is most efficient by doing a swipe over
	 * the whole bucket array.
	 */
	for (int i = 0; i < size; i++) {
	    Bucket b = bucket[i + offset];
	    if (b != null) {
		putBucket(b);
		bucket[i + offset] = null;
	    }
	}

	size = initsize;
	offset = 0;
	present = 0;

	if (STATS) {
	    // contains = finds = 0;
	}
    }


    final static void statistics() {
	if (STATS) {
	    System.err.println("IbisHash:" +
		    //  "buckets     = " + size +
		    " contains " + contains +
		    " finds " + finds +
		    " rebuilds " + rebuilds +
		    " new buckets " + new_buckets +
		    " collisions " + collisions +
		    " (rebuild " + rebuild_collisions +
		    ")");
	}
    }
}
