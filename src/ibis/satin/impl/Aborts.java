/* $Id$ */

package ibis.satin.impl;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.WriteMessage;

import java.io.IOException;

public abstract class Aborts extends WorkStealing {

    /**
     * Aborts the spawns that are the result of the specified invocation record.
     * The invocation record of the invocation actually throwing the exception
     * is also specified, but it is valid only for clones with inlets.
     * 
     * @param outstandingSpawns
     *            parent of spawns that need to be aborted.
     * @param exceptionThrower
     *            invocation throwing the exception.
     */
    public synchronized void abort(InvocationRecord outstandingSpawns,
            InvocationRecord exceptionThrower) {
        // We do not need to set outstanding Jobs in the parent frame to null,
        // it is just used for assigning results.
        // get the lock, so no-one can steal jobs now, and no-one can change my
        // tables.
        if (abortLogger.isDebugEnabled()) {
            abortLogger.debug("q " + q.size() + ", s " + onStack.size() + ", o "
                    + outstandingJobs.size());
        }
        try {
            if (abortLogger.isDebugEnabled()) {
                abortLogger.debug("SATIN '" + ident
                        + "': Abort, outstanding = " + outstandingSpawns
                        + ", thrower = " + exceptionThrower);
            }
            if (SPAWN_STATS) {
                aborts++;
            }

            if (exceptionThrower != null) { // can be null if root does an
                // abort.
                // kill all children of the parent of the thrower.
                if (abortLogger.isDebugEnabled()) {
                    abortLogger.debug("killing children of "
                            + exceptionThrower.parentStamp);
                }
                killChildrenOf(exceptionThrower.parentStamp);
            }

            // now kill mine
            if (outstandingSpawns != null) {
                Stamp stamp;
                if (outstandingSpawns.parent == null) {
                    stamp = null;
                } else {
                    stamp = outstandingSpawns.parent.stamp;
                }

                if (abortLogger.isDebugEnabled()) {
                    abortLogger.debug("killing children of my own: " + stamp);
                }
                killChildrenOf(stamp);
            }

            if (abortLogger.isDebugEnabled()) {
                abortLogger.debug("SATIN '" + ident + "': Abort DONE");
            }
        } catch (Exception e) {
            abortLogger.warn("GOT EXCEPTION IN RTS!: " + e, e);
        }
    }

    protected void killChildrenOf(Stamp targetStamp) {
        if (ABORT_TIMING) {
            abortTimer.start();
        }

        if (ASSERTS) {
            assertLocked(this);
        }
        /*
         * while(true) {
         *     long abortCount = abortedJobs;
         * 
         *     if (abortLogger.isDebugEnabled()) {
         *         abortLogger.debug("killChildrenOf: iter = " + iter 
         *             + " abort cnt = " + abortedJobs);
         *     }
         */
        // try work queue, outstanding jobs and jobs on the stack
        // but try stack first, many jobs in q are children of stack jobs.
        onStack.killChildrenOf(targetStamp);
        q.killChildrenOf(targetStamp);
        outstandingJobs.killChildrenOf(targetStamp);
        /*
         *     if(abortedJobs == abortCount) {
         *         // no more jobs were removed.
         *         break;
         *     }
         * 
         *     iter++;
         * }
         */
        if (ABORT_TIMING) {
            abortTimer.stop();
        }
    }

    //abort every job that was spawned on targetOwner
    //or is a child of a job spawned on targetOwner
    //used for fault tolerance
    protected void killSubtreeOf(IbisIdentifier targetOwner) {
        onStack.killSubtreeOf(targetOwner);
        q.killSubtreeOf(targetOwner);
        outstandingJobs.killSubtreeOf(targetOwner);
    }

    /**
     * Determines if the specified invocation record is a descendent of
     * the job indicated by the specied stamp.
     */
    static boolean isDescendentOf(InvocationRecord child, Stamp targetStamp) {
        if (child.parentStamp == null) {
            if (targetStamp == null) {
                return true;
            }
            return false;
        }
        return child.parentStamp.isDescendentOf(targetStamp);
    }

    static boolean isDescendentOf1(InvocationRecord child,
            IbisIdentifier targetOwner) {
        if (child.parentOwner.equals(targetOwner)) {
            return true;
        }
        if (child.parent == null) {
            return false;
        }

        return isDescendentOf1(child.parent, targetOwner);
    }

    /*
     * static boolean isDescendentOf(InvocationRecord child, Stamp targetStamp) {
     *     for(int i = 0; i < child.parentStamps.size(); i++) {
     *         Stamp currStamp = (Stamp) child.parentStamps.get(i);
     *         IbisIdentifier currOwner = (IbisIdentifier)
     *                 child.parentOwners.get(i);
     * 
     *         if (currStamp.stampEquals(targetStamp)) {
     *             return true;
     *         }
     *     }
     *     return false;
     * }
     */
    /*
     * message combining for abort messages does not work (I tried). It is very
     * unlikely that one node stole more than one job from me
     */
    void sendAbortMessage(InvocationRecord r) {
        if (abortLogger.isDebugEnabled()) {
            abortLogger.debug("SATIN '" + ident
                    + ": sending abort message to: " + r.stealer + " for job "
                    + r.stamp);
        }

        if (deadIbises.contains(r.stealer)) {
            /* don't send abort and store messages to crashed ibises */
            return;
        }

        try {
            Victim v = getVictimNoWait(r.stealer);
            if (v == null) {
                return;
            }

            WriteMessage writeMessage = v.newMessage();
            writeMessage.writeByte(Protocol.ABORT);
            writeMessage.writeObject(r.parentStamp);
            long cnt = writeMessage.finish();
            if (STEAL_STATS) {
                if (inDifferentCluster(r.stealer)) {
                    interClusterMessages++;
                    interClusterBytes += cnt;
                } else {
                    intraClusterMessages++;
                    intraClusterBytes += cnt;
                }
            }
        } catch (IOException e) {
            abortLogger.info("SATIN '" + ident
                    + "': Got Exception while sending abort message: " + e, e);
            // This should not be a real problem, it is just inefficient.
            // Let's continue...
            // System.exit(1);
            // if we don't continue, make this a "fatal".
        }
    }

    void addToAbortList(Stamp stamp) {
        if (ASSERTS) {
            assertLocked(this);
        }
        if (abortLogger.isDebugEnabled()) {
            abortLogger.debug("SATIN '" + ident + ": got abort message");
        }
        abortList.add(stamp);
        gotAborts = true;
    }

    synchronized void handleAborts() {
        Stamp stamp;

        while (true) {
            if (abortList.count > 0) {
                stamp = abortList.stamps[0];
                abortList.removeIndex(0);
            } else {
                gotAborts = false;
                return;
            }

            if (abortLogger.isDebugEnabled()) {
                abortLogger.debug("SATIN '" + ident
                        + ": handling abort message: stamp = " + stamp);
            }

            if (ABORT_STATS) {
                aborts++;
            }

            killChildrenOf(stamp);

            if (abortLogger.isDebugEnabled()) {
                abortLogger.debug("SATIN '" + ident
                        + ": handling abort message: stamp = " + stamp
                        + " DONE");
            }
        }
    }
}
