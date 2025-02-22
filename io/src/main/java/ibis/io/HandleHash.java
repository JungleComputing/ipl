/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* $Id$ */

package ibis.io;

import ibis.util.Timer;

/**
 * A hash table that aims for speed for pairs (Object, int). This one is
 * specially made for (object, handle) pairs.
 */
public final class HandleHash {

    private static final boolean STATS = IOProperties.properties.getBooleanProperty(
            IOProperties.s_hash_stats);

    private static final boolean TIMINGS = IOProperties.properties.getBooleanProperty(
            IOProperties.s_hash_timings);

    private static final int MIN_BUCKETS = 32;

    private static final boolean CACHE_HASH = false;

    /*
     * Choose this value between 50 and 200.
     * It determines the amount of chaining
     * before the hashmap is resized. Lower value means less chaining, but
     * larger hashtable.
     */
    private static final int RESIZE_PERCENTAGE = IOProperties.properties.getIntProperty(
            IOProperties.s_hash_resize, 100);

    /** Maps handle to object. */
    private Object[] dataBucket;

    /** Maps handle to hashcode. */
    private int[] hashBucket;   // if (CACHE_HASH)

    /** Maps handle to next with same hash value. */
    private int[] nextBucket;

    /** Maps hash value to handle. */
    private int[] map;

    // if (STATS)
    private long finds;

    private long rebuilds;

    private long collisions;

    private long rebuild_collisions;

    private int maxsize;

    private int mapsize;

    // if (TIMINGS)
    private Timer t_insert;

    private Timer t_find;

    private Timer t_rebuild;

    private Timer t_growbucket;

    /** Initial size of buckets. */
    private int initSize;

    /** When to grow ... */
    private int sizeThreshold;

    /** maximum handle+1 */
    private int size;

    /** Number of entries. */
    private int present;

    public HandleHash() {
        this(MIN_BUCKETS);
    }

    public HandleHash(int sz) {

        int x = 1;
        while (x < sz) {
            x <<= 1;
        }
        if (x != sz) {
            System.err.println("Warning: Hash table size (" + sz
                    + ") must be a power of two. Increment to " + x);
            sz = x;
        }

        initSize = sz;
        maxsize = initSize;
        mapsize = initSize;

        init(sz);

        if (TIMINGS) {
            t_insert = Timer.createTimer();
            t_find = Timer.createTimer();
            t_rebuild = Timer.createTimer();
            t_growbucket = Timer.createTimer();
        }
        if (STATS || TIMINGS) {
            Runtime.getRuntime().addShutdownHook(
                    new Thread("HandleHash ShutdownHook") {
                        public void run() {
                            statistics();
                        }
                    });
        }
    }

    private void init(int sz) {
        sizeThreshold = (sz * RESIZE_PERCENTAGE) / 100;

        map = new int[sz];
        nextBucket = new int[sz];
        if (CACHE_HASH) {
            hashBucket = new int[sz];
        }
        dataBucket = new Object[sz];
        size = 1;
        present = 0;
    }

    /**
     * We know size is a power of two. Make mod cheap.
     */
    private static final int mod(int x, int size) {
        return (x & (size - 1));
    }

    static final int getHashCode(Object ref) {
        int h = System.identityHashCode(ref);

        h += ~(h << 9);
        h ^=  (h >>> 14);
        h +=  (h << 4);
        h ^=  (h >>> 10);

        return h;
    }

    public final int find(Object ref) {
        return find(ref, getHashCode(ref));
    }

    public final int find(Object ref, int hashcode) {
        if (TIMINGS) {
            t_find.start();
        }

        if (STATS) {
            finds++;
        }

        int h = mod(hashcode, map.length);
        for (int i = map[h]; i > 0; i = nextBucket[i]) {
            if (dataBucket[i] == ref) {
                if (TIMINGS) {
                    t_find.stop();
                }
                
                return i;
            }
        }

        if (TIMINGS) {
            t_find.stop();
        }
        return 0;
    }

    /**
     * Rebuild if fill factor is too high.
     */
    private final void growMap() {

        long saved_collisions = collisions;

        if (TIMINGS) {
            t_rebuild.start();
        }
        map = new int[(map.length << 1)];
        if (map.length > mapsize) {
            mapsize = map.length;
        }
        sizeThreshold = (map.length * RESIZE_PERCENTAGE) / 100;

        for (int i = 0; i < size; i++) {
            if (dataBucket[i] != null) {
                int h;
                if (CACHE_HASH) {
                    h = hashBucket[i];
                } else {
                    h = getHashCode(dataBucket[i]);
                }
                h = mod(h, map.length);

                if (STATS) {
                    if (map[h] != 0) {
                        collisions++;
                    }
                }
                nextBucket[i] = map[h];
                map[h] = i;
            }
        }

        if (TIMINGS) {
            t_rebuild.stop();
        }

        if (STATS) {
            rebuild_collisions += collisions - saved_collisions;
            rebuilds++;
        }
    }

    private void growBuckets(int sz) {
        if (TIMINGS) {
            t_growbucket.start();
        }
        int newsize = (sz << 1) + 1;
        if (newsize > maxsize) {
            maxsize = newsize;
        }
        int[] newNextBucket = new int[newsize];
        System.arraycopy(nextBucket, 0, newNextBucket, 0, sz);
        nextBucket = newNextBucket;
        Object[] newDataBucket = new Object[newsize];
        System.arraycopy(dataBucket, 0, newDataBucket, 0, sz);
        dataBucket = newDataBucket;
        if (CACHE_HASH) {
            int[] newHashBucket = new int[newsize];
            System.arraycopy(hashBucket, 0, newHashBucket, 0, sz);
            hashBucket = newHashBucket;
        }
        if (TIMINGS) {
            t_growbucket.stop();
        }
    }

    /**
     * Insert (ref, handle) into the hash table.
     * 
     * @param ref
     *            the object that is inserted
     * @param handle
     *            the (int valued) key
     * @param hashcode
     *            the hashcode of ref that may be kept over calls to the hash
     *            table
     * @return the handle.
     */
    public int put(Object ref, int handle, int hashcode) {

        if (present >= sizeThreshold) {
            growMap();
        }

        if (handle >= dataBucket.length) {
            growBuckets(handle);
        }

        if (TIMINGS) {
            t_insert.start();
        }

        int h = mod(hashcode, map.length);
        dataBucket[handle] = ref;
        if (CACHE_HASH) {
            hashBucket[handle] = hashcode;
        }
        if (STATS) {
            if (map[h] != 0) {
                collisions++;
            }
        }
        nextBucket[handle] = map[h];
        map[h] = handle;
        if (TIMINGS) {
            t_insert.stop();
        }
        present++;
        if (handle >= size) {
            size = handle + 1;
        }
        return handle;
    }

    /**
     * Insert (ref, handle) into the hash table lazily. If already present, the
     * present handle is returned instead.
     * 
     * @param ref
     *            the object that is inserted
     * @param handle
     *            the (int valued) key
     * @param hashcode
     *            the hashcode of ref that may be kept over calls to the hash
     *            table
     * @return the handle found.
     */
    public final int lazyPut(Object ref, int handle, int hashcode) {
        int f = find(ref, hashcode);
        if (f != 0) {
            return f;
        }

        return put(ref, handle, hashcode);
    }

    public final int put(Object ref, int handle) {
        return put(ref, handle, getHashCode(ref));
    }

    public final int lazyPut(Object ref, int handle) {
        return lazyPut(ref, handle, getHashCode(ref));
    }

    public final void clear() {

        // Check if the table has grown. If not, we
        // can reuse the existing arrays.

        if (present != 0) {
            if (size < initSize) {

                for (int i = 0; i < map.length; i++) {
                    map[i] = 0;
                }

                for (int i = 0; i < size; i++) {
                    nextBucket[i] = 0;
                    dataBucket[i] = null;
                }

                size = 1;
                present = 0;
            } else {
                init(initSize);
            }
        }
    }

    final void statistics() {
        if (STATS) {
            System.err.println(this + ": " +
            // "buckets = " + size +
                    " mapsize " + mapsize + " maxsize " + maxsize + " finds "
                    + finds + " rebuilds " + rebuilds + " collisions "
                    + collisions + " (rebuild " + rebuild_collisions + ")");
        }
        if (TIMINGS) {
            System.err.println(this +
                    " insert(" + t_insert.nrTimes() + ") "
                    + Timer.format(t_insert.totalTimeVal())
                    + " find(" + t_find.nrTimes() + ") "
                    + Timer.format(t_find.totalTimeVal())
                    + " rebuild(" + t_rebuild.nrTimes() + ") "
                    + Timer.format(t_rebuild.totalTimeVal())
                    + " growBucket(" + t_growbucket.nrTimes() + ") "
                    + Timer.format(t_growbucket.totalTimeVal()));
        }
    }
}
