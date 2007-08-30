/*
 * Created on May 9, 2006
 */
package ibis.satin.impl.aborts;

import ibis.ipl.ReadMessage;
import ibis.satin.impl.Config;
import ibis.satin.impl.Satin;
import ibis.satin.impl.spawnSync.IRVector;
import ibis.satin.impl.spawnSync.InvocationRecord;
import ibis.satin.impl.spawnSync.Stamp;
import ibis.satin.impl.spawnSync.StampVector;

public final class Aborts implements Config {
    private Satin s;

    private AbortsCommunication abortsComm;

    /** Abort messages are queued until the sync. */
    private StampVector abortList = new StampVector();

    /* use these to avoid locking */
    private volatile boolean gotExceptions = false;

    private volatile boolean gotAborts = false;

    private IRVector exceptionList;

    public Aborts(Satin s) {
        this.s = s;
        abortsComm = new AbortsCommunication(s);
        exceptionList = new IRVector(s);
    }

    public void addToAbortList(Stamp stamp) {
        Satin.assertLocked(s);
        if (abortLogger.isDebugEnabled()) {
            abortLogger.debug("SATIN '" + s.ident + ": got abort message");
        }
        abortList.add(stamp);
        gotAborts = true;
    }

    public void handleDelayedMessages() {
        if (gotAborts) {
            handleAborts();
        }

        if (gotExceptions) {
            handleExceptions();
        }
    }

    public void addToExceptionList(InvocationRecord r) {
        Satin.assertLocked(s);
        exceptionList.add(r);
        gotExceptions = true;
        if (inletLogger.isDebugEnabled()) {
            inletLogger.debug("SATIN '" + s.ident + ": got remote exception!");
        }
    }

    // Trace back from the exception, and execute inlets / empty inlets back to
    // the root. During this, send result messages as soon as possible.
    public void handleInlet(InvocationRecord r) {
        if (r.isInletExecuted()) {
            return;
        }

        if (r.getParentLocals() == null) {
            handleEmptyInlet(r);
            return;
        }

        s.onStack.push(r);
        InvocationRecord oldParent = s.parent;
        s.parent = r;

        try {
            if (inletLogger.isDebugEnabled()) {
                inletLogger.debug("SATIN '" + s.ident
                    + ": calling inlet caused by remote exception");
            }

            r.getParentLocals().handleException(r.getSpawnId(), r.eek, r);
            r.setInletExecuted(true);

            // restore this, there may be more spawns afterwards...
            s.parent = oldParent;
            s.onStack.pop();
        } catch (Throwable t) {
            // The inlet has thrown an exception itself.
            // The semantics of this: throw the exception to the parent,
            // And execute the inlet if it has one (might be an empty one).
            // Also, the other children of the parent must be aborted.
            r.setInletExecuted(true);

            // restore this, there may be more spawns afterwards...
            s.parent = oldParent;
            s.onStack.pop();

            if (inletLogger.isDebugEnabled()) {
                inletLogger.debug("Got an exception from exception handler! "
                    + t + ", r = " + r + ", r.parent = " + r.getParent(), t);
            }

            if (r.getParent() == null) {
                if (inletLogger.isDebugEnabled()) {
                    inletLogger.debug("there is no parent that handles it");
                }

                if (t instanceof Error) {
                    Error te = (Error) t;
                    throw te;
                }
                if (t instanceof RuntimeException) {
                    RuntimeException tr = (RuntimeException) t;
                    throw tr;
                }
                throw new Error("Inlet threw exception: ", t);
            }

            s.stats.abortsDone++;

            synchronized (s) {
                // also kill the parent itself.
                // It is either on the stack or on a remote machine.
                // Here, this is OK, the child threw an exception,
                // the parent did not catch it, and must therefore die.
                if (r.getParent().aborted) {
                    return;
                }
                r.getParent().aborted = true;
                r.getParent().eek = t; // rethrow exception
                killChildrenOf(r.getParent().getStamp());
            }

            if (!r.getParentOwner().equals(s.ident)) {
                if (inletLogger.isDebugEnabled()) {
                    inletLogger.debug("SATIN '" + s.ident
                        + ": sending exception result");
                }
                s.lb.sendResult(r.getParent(), null);
                return;
            }

            // two cases here: empty inlet or normal inlet
            if (r.getParent().getParentLocals() == null) { // empty inlet
                handleEmptyInlet(r.getParent());
            } else { // normal inlet
                handleInlet(r.getParent());
            }
        }
    }

    public void killChildrenOf(Stamp targetStamp) {
        try {
            s.stats.abortTimer.start();

            if (ASSERTS) {
                Satin.assertLocked(s);
            }

            // try work queue, outstanding jobs and jobs on the stack
            // but try stack first, many jobs in q are children of stack jobs.
            s.onStack.killChildrenOf(targetStamp, false);
            s.q.killChildrenOf(targetStamp);
            s.outstandingJobs.killChildrenOf(targetStamp, false);
        } finally {
            s.stats.abortTimer.stop();
        }
    }

    // Trace back from the exception, and execute inlets / empty inlets back to
    // the root. During this, send result messages as soon as possible.
    private void handleEmptyInlet(InvocationRecord r) {
        // if r does not have parentLocals, this means
        // that the PARENT does not have a try catch block around the spawn.
        // there is thus no inlet to call in the parent.

        if (r.eek == null) {
            return;
        }
        if (r.getParent() == null) {
            // Throw the exception, otherwise it will just disappear ...
            throw new Error("Spawned job threw exception: ", r.eek);
        }
        if (ASSERTS && r.getParentLocals() != null) {
            s.assertFailed("parenlocals is not null in empty inlet", new Exception());
        }

        if (inletLogger.isDebugEnabled()) {
            inletLogger.debug("SATIN '" + s.ident
                + ": Got exception, empty inlet: " + r.eek, r.eek);
        }

        synchronized (s) {
            // also kill the parent itself.
            // It is either on the stack or on a remote machine.
            // Here, this is OK, the child threw an exception,
            // the parent did not catch it, and must therefore die.
            r.getParent().aborted = true;
            r.getParent().eek = r.eek; // rethrow exception
            killChildrenOf(r.getParent().getStamp());
        }

        if (!r.getParentOwner().equals(s.ident)) {
            if (inletLogger.isDebugEnabled()) {
                inletLogger.debug("SATIN '" + s.ident
                    + ": sending exception result");
            }
            s.lb.sendResult(r.getParent(), null);
            return;
        }

        // now the recursion step
        if (r.getParent().getParentLocals() != null) { // parent has inlet
            handleInlet(r.getParent());
        } else {
            handleEmptyInlet(r.getParent());
        }
    }

    // both here and in handleEmpty inlets: sendResult NOW if parentOwner is on
    // remote machine
    private void handleExceptions() {
        InvocationRecord r;
        while (true) {
            synchronized (s) {
                r = exceptionList.removeIndex(0);
                if (r == null) {
                    gotExceptions = false;
                    return;
                }
            }

            if (inletLogger.isDebugEnabled()) {
                inletLogger.debug("SATIN '" + s.ident
                    + ": handling remote exception: " + r.eek + ", inv = " + r);
            }

            //  If there is an inlet, call it.
            handleInlet(r);
            r.decrSpawnCounter();

            if (inletLogger.isDebugEnabled()) {
                inletLogger.debug("SATIN '" + s.ident
                    + ": handling remote exception DONE");
            }
        }
    }

    private void handleAborts() {
        synchronized (s) {

            Stamp stamp;

            while (true) {
                if (abortList.getCount() > 0) {
                    stamp = abortList.getStamp(0);
                    abortList.removeIndex(0);
                } else {
                    gotAborts = false;
                    return;
                }

                if (abortLogger.isDebugEnabled()) {
                    abortLogger.debug("SATIN '" + s.ident
                        + ": handling abort message: stamp = " + stamp);
                }

                s.stats.abortsDone++;
                killChildrenOf(stamp);

                if (abortLogger.isDebugEnabled()) {
                    abortLogger.debug("SATIN '" + s.ident
                        + ": handling abort message: stamp = "
                        + stamp + " DONE");
                }
            }
        }
    }

    public void handleAbort(ReadMessage m) {
        abortsComm.handleAbort(m);
    }

    public void sendAbortMessage(InvocationRecord r) {
        abortsComm.sendAbortMessage(r);
    }
}
