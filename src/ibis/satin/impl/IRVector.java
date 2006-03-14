/* $Id$ */

package ibis.satin.impl;

import ibis.ipl.IbisIdentifier;

/** A vector of invocation records. */

final class IRVector implements Config {
    InvocationRecord[] l = new InvocationRecord[500];

    int count = 0;

    Satin satin;

    IRVector(Satin s) {
        this.satin = s;
    }

    void add(InvocationRecord r) {
        if (ASSERTS) {
            SatinBase.assertLocked(satin);
        }

        if (count >= l.length) {
            InvocationRecord[] nl = new InvocationRecord[l.length * 2];
            System.arraycopy(l, 0, nl, 0, l.length);
            l = nl;
        }

        l[count] = r;
        count++;
        // System.err.println("SATIN: " + satin.ident.address()
        //         + "add count is: " + count);
    }

    int size() {
        if (ASSERTS) {
            SatinBase.assertLocked(satin);
        }
        return count;
    }

    int numOf(ibis.ipl.IbisIdentifier owner) {
        if (ASSERTS) {
            SatinBase.assertLocked(satin);
        }
        int c = 0;
        for (int i = 0; i < count; i++) {
            if (l[i].stealer.equals(owner)) {
                c++;
            }
        }
        return c;
    }

    InvocationRecord remove(Stamp stamp) {
        InvocationRecord res = null;

        if (ASSERTS) {
            SatinBase.assertLocked(satin);
        }

        // System.err.println("SATIN: " + satin.ident.address()
        //         + " removing job " + stamp);

        if (ASSERTS) {
            SatinBase.assertLocked(satin);
        }

        for (int i = 0; i < count; i++) {
            if (l[i] == null) {
                System.err.println(
                        "l[i] is null, i: " + i + ",count: " + count);
            }
            if (l[i].stamp.stampEquals(stamp)) {
                res = l[i];
                count--;
                l[i] = l[count];
                // System.err.println("SATIN: " + satin.ident.address()
                //         + "remove1 count is: " + count);
                l[count] = null;
                return res;
            }
        }

        // System.err.println("SATIN: " + satin.ident.address()
        //         + " remove1 count is: " + count);

        return null;
    }

    InvocationRecord remove(InvocationRecord r) {
        if (ASSERTS) {
            SatinBase.assertLocked(satin);
        }

        for (int i = count - 1; i >= 0; i--) {
            if (l[i].equals(r)) {
                InvocationRecord res = l[i];
                count--;
                l[i] = l[count];
                l[count] = null;
                // System.err.println("SATIN: " + satin.ident.address()
                //         + " remove2 count is: " + count);
                return res;
            }
        }

        spawnLogger.error("EEK, IRVector: removeing non-existant elt: " + r);
        // System.exit(1);
        return null;
    }

    void killChildrenOf(Stamp targetStamp) {
        if (ASSERTS) {
            SatinBase.assertLocked(satin);
        }
        InvocationRecord curr;
        for (int i = 0; i < count; i++) {
            curr = l[i];
            // if (curr.aborted) {
            //     continue;  // already handled.
            // }

            if ((curr.parent != null && curr.parent.aborted)
                    || Aborts.isDescendentOf(curr, targetStamp)) {
                curr.aborted = true;
                if (abortLogger.isDebugEnabled()) {
                    abortLogger.debug("found stolen child: " + curr.stamp
                            + ", it depends on " + targetStamp);
                }
                if (ENABLE_SPAWN_LOGGING && spawnLogger.isDebugEnabled()) {
                    curr.spawnCounter.decr(curr);
                    spawnLogger.debug("IRVector.killChildrenOf: spawnCounter = "
                            + curr.spawnCounter
                            + "(" + curr.spawnCounter.value + ")");
                } else {
                    curr.spawnCounter.value--;
                }
                if (ASSERTS && curr.spawnCounter.value < 0) {
                    System.out.println("Just made spawncounter < 0");
                    new Exception().printStackTrace();
                    System.exit(1);     // Failed assertion
                }
                if (ABORT_STATS) {
                    satin.abortedJobs++;
                }
                if (STEAL_STATS) {
                    satin.abortMessages++;
                }
                // Curr is removed, but not put back in cache.
                // this is OK. Moreover, it might have children,
                // so we should keep it alive.
                // cleanup is done inside the spawner itself.
                removeIndex(i);
                i--;
                satin.sendAbortMessage(curr);
            }
        }
    }

    /**
     * Used for fault tolerance send an ABORT_AND_STORE message to the stealer
     * of each descendent of the given job
     */
    void killAndStoreChildrenOf(Stamp targetStamp) {

        if (ASSERTS) {
            SatinBase.assertLocked(satin);
        }

        InvocationRecord curr;
        for (int i = 0; i < count; i++) {
            curr = l[i];
            if ((curr.parent != null && curr.parent.aborted)
                    || Aborts.isDescendentOf(curr, targetStamp)) {
                curr.aborted = true;
                if (abortLogger.isDebugEnabled()) {
                    abortLogger.debug("found stolen child: " + curr.stamp
                            + ", it depends on " + targetStamp);
                }
                if (ENABLE_SPAWN_LOGGING && spawnLogger.isDebugEnabled()) {
                    curr.spawnCounter.decr(curr);
                    spawnLogger.debug("IRVector.killAndStoreChildrenOf: spawnCounter = "
                            + curr.spawnCounter
                            + "(" + curr.spawnCounter.value + ")");
                } else {
                    curr.spawnCounter.value--;
                }
                if (ASSERTS && curr.spawnCounter.value < 0) {
                    System.out.println("Just made spawncounter < 0");
                    new Exception().printStackTrace();
                    System.exit(1);     // Failed assertion
                }
                if (ABORT_STATS) {
                    satin.abortedJobs++;
                }
                if (STEAL_STATS) {
                    satin.abortMessages++;
                }

                // Curr is removed, but not put back in cache.
                // this is OK. Moreover, it might have children,
                // so we should keep it alive.
                // cleanup is done inside the spawner itself.
                removeIndex(i);
                i--;
                satin.sendAbortAndStoreMessage(curr);
            }
        }
    }

    //abort every job that was spawned on targetOwner
    //or is a child of a job spawned on targetOwner
    void killSubtreeOf(IbisIdentifier targetOwner) {

        if (ASSERTS) {
            SatinBase.assertLocked(satin);
        }

        InvocationRecord curr;
        for (int i = 0; i < count; i++) {
            curr = l[i];
            if ((curr.parent != null && curr.parent.aborted)
                    || Aborts.isDescendentOf1(curr, targetOwner)
                    || curr.owner.equals(targetOwner)) {
                //this shouldnt happen, actually
                curr.aborted = true;
                if (abortLogger.isDebugEnabled()) {
                    abortLogger.debug("found stolen child: " + curr.stamp
                            + ", it depends on " + targetOwner);
                }
                if (ENABLE_SPAWN_LOGGING && spawnLogger.isDebugEnabled()) {
                    curr.spawnCounter.decr(curr);
                    spawnLogger.debug("IRVector.killSubtreeOf: spawnCounter = "
                            + curr.spawnCounter
                            + "(" + curr.spawnCounter.value + ")");
                } else {
                    curr.spawnCounter.value--;
                }
                if (ASSERTS && curr.spawnCounter.value < 0) {
                    System.out.println("Just made spawncounter < 0");
                    new Exception().printStackTrace();
                    System.exit(1);     // Failed assertion
                }
                // if(ABORT_STATS) {
                //     satin.abortedJobs++;
                // }
                if (STEAL_STATS) {
                    satin.abortMessages++;
                }
                removeIndex(i);
                i--;
                satin.sendAbortMessage(curr);
            }
        }
        // System.err.println("SATIN: " + satin.ident.address() + "count is: "
        //         + count);
    }

    /**
     * Used for fault tolerance send an ABORT_AND_STORE message to the stealer
     * of each job that is a descendent of any job spawned by the targetOwner
     */

    void killAndStoreSubtreeOf(IbisIdentifier targetOwner) {

        if (ASSERTS) {
            SatinBase.assertLocked(satin);
        }

        InvocationRecord curr;
        for (int i = 0; i < count; i++) {
            curr = l[i];
            if ((curr.parent != null && curr.parent.aborted)
                    || Aborts.isDescendentOf1(curr, targetOwner)
                    || curr.owner.equals(targetOwner)) {
                //this shouldnt happen, actually
                curr.aborted = true;
                if (ENABLE_SPAWN_LOGGING && spawnLogger.isDebugEnabled()) {
                    curr.spawnCounter.decr(curr);
                    spawnLogger.debug("IRVector.killAndStoreSubtreeOf: spawnCounter = "
                            + curr.spawnCounter
                            + "(" + curr.spawnCounter.value + ")");
                } else {
                    curr.spawnCounter.value--;
                }
                if (ASSERTS && curr.spawnCounter.value < 0) {
                    System.out.println("Just made spawncounter < 0");
                    new Exception().printStackTrace();
                    System.exit(1);     // Failed assertion
                }
                removeIndex(i);
                i--;
                satin.sendAbortAndStoreMessage(curr);
            }
        }
    }

    void killAll() {

        if (ASSERTS) {
            SatinBase.assertLocked(satin);
        }

        InvocationRecord curr;

        for (int i = 0; i < count; i++) {
            curr = l[i];
            curr.aborted = true;
            if (ENABLE_SPAWN_LOGGING && spawnLogger.isDebugEnabled()) {
                curr.spawnCounter.decr(curr);
                spawnLogger.debug("IRVector.killAll: spawnCounter = "
                        + curr.spawnCounter
                        + "(" + curr.spawnCounter.value + ")");
            } else {
                curr.spawnCounter.value--;
            }
            removeIndex(i);
            i--;
        }
    }

    InvocationRecord removeIndex(int i) {
        if (ASSERTS) {
            SatinBase.assertLocked(satin);
        }
        if (i >= count) {
            return null;
        }

        InvocationRecord res = l[i];
        count--;
        l[i] = l[count];
        l[count] = null;
        return res;
    }

    /**
     * Used for fault tolerance remove all the jobs stolen by targetOwner and
     * put them back in the taskQueue
     */
    void redoStolenByWorkQueue(IbisIdentifier crashedIbis) {

        if (ASSERTS) {
            SatinBase.assertLocked(satin);
        }

        for (int i = count - 1; i >= 0; i--) {
            if (crashedIbis.equals(l[i].stealer)) {
                l[i].reDone = true;
                l[i].stealer = null;
                satin.q.addToTail(l[i]);
                if (FT_STATS) {
                    satin.restartedJobs++;
                }
                count--;
                l[i] = l[count];

            }
        }
    }

    /**
     * Used for fault tolerance; remove all the jobs stolen by crashedIbis
     * and attach them to their parents
     */
    void redoStolenByAttachToParents(IbisIdentifier crashedIbis) {

        if (ASSERTS) {
            SatinBase.assertLocked(satin);
        }

        for (int i = count - 1; i >= 0; i--) {
            if (crashedIbis.equals(l[i].stealer)) {
                l[i].reDone = true;
                l[i].stealer = null;
                satin.attachToParentToBeRestarted(l[i]);
                if (FT_STATS) {
                    satin.restartedJobs++;
                }
                count--;
                l[i] = l[count];

            }
        }
    }

    void print(java.io.PrintStream out) {
        /*
         * if (ASSERTS) {
         *     Satin.assertLocked(satin);
         * }
         */

        out.println("=IRVector " + satin.ident + ":=============");
        for (int i = 0; i < count; i++) {
            ParameterRecord pr = l[i].getParameterRecord();
            out.println("outjobs [" + i + "] = " + pr + ","
                    + l[i].stealer);
        }
        out.println("end of IRVector: " + satin.ident + "=");
    }

    InvocationRecord first() {
        return l[0];
    }
}
