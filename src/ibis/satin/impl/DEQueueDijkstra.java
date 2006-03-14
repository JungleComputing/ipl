/* $Id$ */

package ibis.satin.impl;

/** The Dijkstra implementation of a double-ended queue. */

// warning! this does not work correctly in combination with aborts!
// This is because we have to remove elements from random places in the queue.
// Is this true???
final class DEQueueDijkstra extends DEQueue implements Config {

    private static final int START_SIZE = 5000;

    private InvocationRecord[] l = new InvocationRecord[START_SIZE];

    private volatile int size = START_SIZE;

    private volatile int head;

    private volatile int tail;

    private Satin satin;

    DEQueueDijkstra(Satin satin) {
        this.satin = satin;
    }

    int size() {
        synchronized (satin) {
            return head - tail;
        }
    }

    void addToHead(InvocationRecord r) {
        if (head == size) {
            if (ENABLE_SPAWN_LOGGING && spawnLogger.isDebugEnabled()) {
                spawnLogger.debug("doubling DEq, new size = " + (size*2));
            }

            synchronized (satin) {
                size *= 2;
                InvocationRecord[] nl = new InvocationRecord[size];
                System.arraycopy(l, 0, nl, 0, l.length);
                l = nl;
            }
        }

        l[head] = r;
        head++;
    }

    void addToTail(InvocationRecord r) {
        throw new Error("Not implemented");
    }

    InvocationRecord getFromHead() {
        head--;
        if (head < tail) {
            head++;
            synchronized (satin) {
                head--;
                if (head < tail) {
                    head++;
                    return null;
                }
            }
        }

        InvocationRecord rval = l[head];
        l[head] = null;

        // success
        return rval;
    }

    synchronized InvocationRecord getFromTail() {
        tail++;
        if (head < tail) {
            tail--;
            return null;
        }

        InvocationRecord rval = l[tail - 1];
        l[tail - 1] = null;
        return rval;
    }

    boolean contains(InvocationRecord r) {
        for (int i = tail; i < head; i++) {
            InvocationRecord curr = l[i];
            if (curr.equals(r)) {
                return true;
            }

        }

        return false;
    }

    void removeJob(int index) {
        if (ASSERTS) {
            SatinBase.assertLocked(satin);
        }

        for (int i = index + 1; i < head; i++) {
            l[i - 1] = l[i];
        }

        l[head] = null;

        head--;
    }

    /* hold the satin lock here! */
    void killChildrenOf(Stamp targetStamp) {
        if (ASSERTS) {
            SatinBase.assertLocked(satin);
        }

        for (int i = tail; i < head; i++) {
            InvocationRecord curr = l[i];
            if (Aborts.isDescendentOf(curr, targetStamp)) {
                if (abortLogger.isDebugEnabled()) {
                    abortLogger.debug("found local child: " + curr.stamp
                            + ", it depends on " + targetStamp);
                }

                curr.aborted = true;
                if (ABORT_STATS) {
                    satin.abortedJobs++;
                }
                if (ENABLE_SPAWN_LOGGING && spawnLogger.isDebugEnabled()) {
                    curr.spawnCounter.decr(curr);
                    spawnLogger.debug("DequeueDijkstra.killChildrenOf: spawnCounter = "
                            + curr.spawnCounter
                            + "(" + curr.spawnCounter.value + ")");
                } else {
                    curr.spawnCounter.value--;
                }
                if (ASSERTS && curr.spawnCounter.value < 0) {
                    spawnLogger.fatal("Just made spawncounter < 0",
                            new Throwable());
                    System.exit(1);     // Failed assertion
                }
                removeJob(i);

                // head--;
                // l[i] = l[head];

                i--;
            }
        }
    }

    /**
     * Used for fault tolerance Aborts all the descendents of any job stolen
     * from the given (crashed) processor
     * 
     * @param owner
     *            identifier of the processor whose jobs (and their descendents)
     *            will be aborted
     */

    //never used -- dijkstra queue doesnt work with fault tolerance
    void killSubtreeOf(ibis.ipl.IbisIdentifier owner) {
        if (ASSERTS) {
            SatinBase.assertLocked(satin);
        }

        for (int i = tail; i < head; i++) {
            InvocationRecord curr = l[i];
            if (Aborts.isDescendentOf1(curr, owner)
                    || curr.owner.equals(owner)) {
                // Should'nt happen.
                curr.aborted = true;
                if (ENABLE_SPAWN_LOGGING && spawnLogger.isDebugEnabled()) {
                    curr.spawnCounter.decr(curr);
                    spawnLogger.debug("DequeueDijkstra.killSubtreeOf: spawnCounter = "
                            + curr.spawnCounter
                            + "(" + curr.spawnCounter.value + ")");
                } else {
                    curr.spawnCounter.value--;
                }
                if (ASSERTS && curr.spawnCounter.value < 0) {
                    spawnLogger.fatal("Just made spawncounter < 0",
                            new Throwable());
                    System.exit(1);     // Failed assertion
                }
                removeJob(i);
                i--;
            }
        }
    }

    void print(java.io.PrintStream out) {
        // TODO
    }

}

