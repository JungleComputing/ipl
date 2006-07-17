/*
 * Created on Apr 27, 2006
 */
/*
 * @todo: rethink the way objects are shipped, both at the beginning of the
 * computation and in case of inconsistencies; instead of waiting for some
 * time and only then shipping, start shipping immediately and if the object
 * arrives in the meantime, cancel the request
 */

// @@@ if LRMC is used, don't create bcast send and receive ports.

package ibis.satin.impl.sharedObjects;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReadMessage;
import ibis.ipl.StaticProperties;
import ibis.satin.SharedObject;
import ibis.satin.impl.Config;
import ibis.satin.impl.Satin;
import ibis.satin.impl.spawnSync.InvocationRecord;

import java.util.HashMap;
import java.util.Vector;

public final class SharedObjects implements Config {
    private final static int WAIT_FOR_UPDATES_TIME = 10000;

    /* use these to avoid locking */
    protected volatile boolean gotSORequests = false;

    protected boolean gotObject = false;

    protected boolean receivingMcast = false;

    protected SharedObject sharedObject = null;

    /** List that stores requests for shared object transfers */
    protected SORequestList SORequestList = new SORequestList();

    /** Used for storing pending shared object invocations (SOInvocationRecords)*/
    private Vector soInvocationList = new Vector();

    private Satin s;

    private volatile boolean gotSOInvocations = false;

    protected HashMap sharedObjects = new HashMap();

    private SOCommunication soComm;

    public SharedObjects(Satin s, StaticProperties requestedProperties) {
        this.s = s;
        soComm = new SOCommunication(s);
        soComm.init(requestedProperties);
    }

    /** Add an object to the object table */
    public void addObject(SharedObject object) {
        synchronized (s) {
            sharedObjects.put(object.objectId, object);
        }
    }

    /**
     * Execute all the so invocations stored in the so invocations list
     */
    public void handleSOInvocations() {
        SharedObject so = null;
        SOInvocationRecord soir = null;
        String soid = null;

        gotSOInvocations = false;
        while (true) {
            s.stats.handleSOInvocationsTimer.start();

            if (soInvocationList.size() == 0) {
                s.stats.handleSOInvocationsTimer.stop();
                return;
            }
            soir = (SOInvocationRecord) soInvocationList.remove(0);
            soid = soir.getObjectId();
            so = (SharedObject) sharedObjects.get(soid);

            if (so == null) {
                s.stats.handleSOInvocationsTimer.stop();
                return;
            }

            // No need to hold the satin lock here.
            // Object transfer requests cannot be handled
            // in the middle of a method invocation, 
            // as transfers are  delayed until a safe point is
            // reached
            soir.invoke(so);
            s.stats.handleSOInvocationsTimer.stop();
        }
    }

    /** Return a reference to a shared object */
    public SharedObject getSOReference(String objectId) {
        synchronized (s) {
            SharedObject obj = (SharedObject) sharedObjects.get(objectId);
            if (obj == null) {
                System.err.println("OOPS, object not found in getSOReference");
            }
            return obj;
        }
    }

    /**
     * Check if the given shared object is in the table, if not, ship it from
     * source. This is called from the generated code.
     */
    public void setSOReference(String objectId, IbisIdentifier source)
        throws SOReferenceSourceCrashedException {
        SharedObject obj = null;

        synchronized (s) {
            while (true) {
                s.handleDelayedMessages();
                obj = (SharedObject) sharedObjects.get(objectId);
                if (obj != null) break;
                if (!receivingMcast) break;

                // wait if we are receiving a shared objects multicast at this moment. 
                try {
                    s.wait();
                } catch (Exception e) {
                    // ignored
                }
            }
        }

        if (obj == null) {
            soComm.fetchObject(objectId, source);
        }
    }

    /**
     * Receive a shared object from another node (called by the MessageHandler
     */
    public void receiveObject(SharedObject obj) {
        synchronized (s) {
            gotObject = true;
            sharedObject = obj;
            s.notifyAll();
        }
    }

    /**
     * Add a shared object invocation record to the so invocation record list;
     * the invocation will be executed later
     */
    public void addSOInvocation(SOInvocationRecord soir) {
        synchronized (s) {
            soInvocationList.add(soir);
            receivingMcast = false;
            gotSOInvocations = true;
            s.notifyAll();
        }
    }

    /** returns false if the job must be aborted */
    public boolean executeGuard(InvocationRecord r) {
        try {
            doExecuteGuard(r);
        } catch (SOReferenceSourceCrashedException e) {
            //the source has crashed - abort the job
            return false;
        }
        return true;
    }

    /**
     * Execute the guard of the invocation record r, wait for updates, if
     * necessary, ship objects if necessary
     */
    private void doExecuteGuard(InvocationRecord r)
        throws SOReferenceSourceCrashedException {
        boolean satisfied;
        long startTime;

        //restore shared object references
        r.setSOReferences();

        satisfied = r.guard();
        if (satisfied) {
            return;
        }

        soLogger.info("SATIN '" + s.ident.name() + "': "
            + "guard not satisfied, waiting for updates..");

        synchronized (s) {
            while (receivingMcast) {
                try {
                    s.wait();
                } catch (Exception e) {
                    // ignored
                }
            }
        }

        startTime = System.currentTimeMillis();
        do {
            s.handleDelayedMessages();
            satisfied = r.guard();
        } while (!satisfied
            && System.currentTimeMillis() - startTime < WAIT_FOR_UPDATES_TIME);

        if (!satisfied) {
            // try to ship the object from the owner of the job
            soLogger.info("SATIN '" + s.ident.name() + "': "
                + "guard not satisfied, trying to ship shared objects ...");
            Vector objRefs = r.getSOReferences();

            // A shared object update may have arrived
            // during one of the fetches.
            while (true) {
                if (objRefs.isEmpty()) break;
                s.handleDelayedMessages();
                if (r.guard()) break;

                String ref = (String) objRefs.remove(0);
                soComm.fetchObject(ref, r.getOwner());
            }

            if (ASSERTS) {
                soLogger.info("SATIN '" + s.ident.name() + "': "
                    + "objects shipped, checking again..");
                if (!r.guard()) {
                    soLogger.fatal("SATIN '" + s.ident.name() + "':"
                        + " panic! inconsistent after shipping objects");
                    System.exit(1); // Failed assert
                }
            }
        }
    }

    public void addToSORequestList(IbisIdentifier requester, String objID) {
        Satin.assertLocked(s);
        SORequestList.add(requester, objID);
        gotSORequests = true;
    }

    public void handleDelayedMessages() {
        if (gotSORequests) {
            soComm.handleSORequests();
        }

        if (gotSOInvocations) {
            s.so.handleSOInvocations();
        }

        soComm.sendAccumulatedSOInvocations();
    }

    public void handleSORequest(ReadMessage m) {
        soComm.handleSORequest(m);
    }

    public void handleSOTransfer(ReadMessage m) {
        soComm.handleSOTransfer(m);
    }

    public void createSoPorts(IbisIdentifier[] joiners) {
        soComm.createSoReceivePorts(joiners);
    }

    public void addSOConnection(IbisIdentifier id) {
        soComm.addSOConnection(id);
    }

    public void removeSOConnection(IbisIdentifier id) {
        soComm.removeSOConnection(id);
    }

    public void broadcastSOInvocation(SOInvocationRecord r) {
        soComm.broadcastSOInvocation(r);
    }

    public void broadcastSharedObject(SharedObject object) {
        soComm.broadcastSharedObject(object);
    }
}
