/* $Id$ */

package ibis.io;

import ibis.util.Timer;

/**
 * A hash table that aims for speed for pairs (Object, int).
 *
 * Goodies that differ from java.util.Hashtable:
 * + The calls into this hash are not synchronized. The caller is
 *   responsible for locking the hash.
 * + Hash table size is always a power of two for fast modulo calculations.
 */
final class IbisHash {

    private static final boolean ASSERTS
            = IOProperties.properties.getBooleanProperty(IOProperties.s_hash_asserts);

    private static final boolean STATS
            = IOProperties.properties.getBooleanProperty(IOProperties.s_hash_stats);

    private static final boolean TIMINGS
            = IOProperties.properties.getBooleanProperty(IOProperties.s_hash_timings);

    private static final int MIN_BUCKETS = 32;

    private static final int USE_NEW_BOUND = Integer.MAX_VALUE;

    /* Choose this value between 2 and 4 */
    private static final int RESIZE_FACTOR = 2;

    /* Choose this value between 1 and 3 */
    private static final int PRE_ALLOC_FACTOR = 1;

    private static final int SHIFT1  = 5;
    // private static final int SHIFT1  = 4;
    private static final int SHIFT2 = 16;

    private Object[] dataBucket;

    private int[] handleBucket;

    // if (STATS)
    private long contains;

    private long finds;

    private long rebuilds;

    private long collisions;

    private long rebuild_collisions;

    private long new_buckets;

    // if (TIMINGS)
    private Timer t_insert;

    private Timer t_find;

    private Timer t_rebuild;

    private Timer t_clear;

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
            System.err.println("Warning: Hash table size (" + sz
                    + ") must be a power of two. Increment to " + x);
            sz = x;
        }

        size = sz;
        initsize = sz;
        newBucketSet(initsize);

        if (TIMINGS) {
            t_insert = Timer.createTimer();
            t_find = Timer.createTimer();
            t_rebuild = Timer.createTimer();
            t_clear = Timer.createTimer();
        }

        if (STATS || TIMINGS) {
            Runtime.getRuntime().addShutdownHook(
                    new Thread("IbisHash ShutdownHook") {
                        public void run() {
                            statistics();
                        }
                    });
        }
    }

    private void newBucketSet(int sz) {
        dataBucket = new Object[sz];
        handleBucket = new int[sz];
        alloc_size = sz;
    }

    private static final int hash_first(int b, int size) {
        return ((b >>> SHIFT1) ^ (b & ((1 << SHIFT2) - 1))) & (size-1);
    }

    private static final int hash_second(int b) {
        return (b & ~0x1) + 12345; /* some odd number */
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
                        System.err.println("CORRUPTED HASH: find returns "
                                + "'no' but it's there in bucket[" + i + "]");
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
     * Rebuild if fill factor is too high or deletions are too high.
     */
    private final void rebuild() {

        if (2 * present <= size) {
            return;
        }

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
            // At least the new slice should fit.
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
                if (!ASSERTS) {
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
                    System.err.println("CORRUPTED HASH after rebuild: "
                            + "cannot find item[" + i + "] = "
                            + Integer.toHexString(getHashCode(old_data[i])));
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
                System.err.println("CORRUPTED HASH after rebuild: present "
                        + present + " but contains " + cont);
            }
        }

        if (TIMINGS) {
            t_rebuild.stop();
        }

        if (STATS) {
            rebuilds++;
        }
    }

    /**
     * Lazy insert (ref, handle) into the hash table.
     *
     * @param  ref the object that is inserted
     * @param  handle the (int valued) key
     * @param  hashCode the hashCode of ref that may be kept over calls to the
     * 		hash table
     * @param  lazy if lazy is <code>true</code>, insert (ref, handle) only
     * 		if ref is not yet present.
     * @return if lazy is <code>true</code> and ref is already in the hash
     * 		table, return the value of its stored handle and ignore
     * 		parameter handle. Else, ref is inserted and the value of
     * 		parameter handle is returned.
     */
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
            if (TIMINGS) {
                t_insert.stop();
            }
            return handleBucket[h0 + offset];
        }

        if (ASSERTS) {
            if (lazy) {
                for (int i = offset; i < offset + size; i++) {
                    if (dataBucket[i] == ref) {
                        System.err.println("CORRUPTED HASH: lazyPut finds "
                                + "'no' but it's there in bucket[" + i + "]");
                    }
                }
            }
        }

        dataBucket[h0 + offset] = ref;
        handleBucket[h0 + offset] = handle;

        present++;

        rebuild();

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
            System.err.println(this + ":" + " alloc_size " + alloc_size
                    + " contains " + contains + " finds " + finds
                    + " rebuilds " + rebuilds + " new buckets " + new_buckets
                    + " collisions " + collisions + " (rebuild "
                    + rebuild_collisions + ")");
        }
        if (TIMINGS) {
            System.err.println(this + " insert(" + t_insert.nrTimes() + ") "
                    + Timer.format(t_insert.totalTimeVal()) + " find("
                    + t_find.nrTimes() + ") "
                    + Timer.format(t_find.totalTimeVal()) + " rebuild("
                    + t_rebuild.nrTimes() + ") "
                    + Timer.format(t_rebuild.totalTimeVal()) + " clear("
                    + t_clear.nrTimes() + ") "
                    + Timer.format(t_clear.totalTimeVal()));
        }
    }
}
