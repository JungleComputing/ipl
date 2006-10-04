/*
 * Created on Apr 27, 2006
 */

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

class SharedObjectInfo {
    long lastBroadcastTime;

    IbisIdentifier[] destinations;

    SharedObject sharedObject;
}

public final class SharedObjects implements Config {
    /* use these to avoid locking */
    protected volatile boolean gotSORequests = false;

    protected boolean receivingMcast = false;

    /** List that stores requests for shared object transfers */
    protected SORequestList SORequestList = new SORequestList();

    /** Used for storing pending shared object invocations (SOInvocationRecords) */
    private Vector soInvocationList = new Vector();

    private Satin s;

    private volatile boolean gotSOInvocations = false;

    /** A hash containing all shared objects: 
     * (String objectID, SharedObject object) */
    private HashMap sharedObjects = new HashMap();

    private SOCommunication soComm;

    public SharedObjects(Satin s, StaticProperties requestedProperties) {
        this.s = s;
        soComm = new SOCommunication(s);
        soComm.init(requestedProperties);
    }

    /** Add an object to the object table */
    public void addObject(SharedObject object) {
        SharedObjectInfo i = new SharedObjectInfo();
        i.sharedObject = object;
        synchronized (s) {
            sharedObjects.put(object.objectId, i);
        }
        
        soLogger.debug("SATIN '" + s.ident.name() + "': " + "object added, id = " + object.objectId);
    }

    /** Return a reference to a shared object */
    public SharedObject getSOReference(String objectId) {
        s.stats.getSOReferencesTimer.start();
        try {
            synchronized (s) {
                SharedObjectInfo i = (SharedObjectInfo) sharedObjects.get(objectId);
                if (i == null) {
                    soLogger.debug("SATIN '" + s.ident.name() + "': " + "object not found in getSOReference");
                    return null;
                }
                return i.sharedObject;
            }
        } finally {
            s.stats.getSOReferencesTimer.stop();
        }
    }

    /** Return a reference to a shared object */
    public SharedObjectInfo getSOInfo(String objectId) {
        synchronized (s) {
            return (SharedObjectInfo) sharedObjects.get(objectId);
        }
    }

    void registerMulticast(SharedObject object, IbisIdentifier[] destinations) {
        synchronized (s) {
            SharedObjectInfo i =
                    (SharedObjectInfo) sharedObjects.get(object.objectId);
            if (i == null) {
                soLogger.warn("OOPS, object not found in registerMulticast");
                return;
            }

            i.destinations = destinations;
            i.lastBroadcastTime = System.currentTimeMillis();
        }
    }

    /**
     * Execute all the so invocations stored in the so invocations list
     */
    public void handleSOInvocations() {
        gotSOInvocations = false;
        while (true) {

            if (soInvocationList.size() == 0) {
                return;
            }
            s.stats.handleSOInvocationsTimer.start();
            SOInvocationRecord soir =
                    (SOInvocationRecord) soInvocationList.remove(0);
            SharedObject so = getSOReference(soir.getObjectId());

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

    /**
     * Check if the given shared object is in the table, if not, ship it from
     * source. This is called from the generated code.
     */
    public void setSOReference(String objectId, IbisIdentifier source)
            throws SOReferenceSourceCrashedException {
        s.handleDelayedMessages();
        SharedObject obj = getSOReference(objectId);
        if (obj == null) {
            if(source == null) {
                throw new Error("internal error, source is null in setSOReference");
            }
            soComm.fetchObject(objectId, source, null);
        }
    }

    /**
     * Add a shared object invocation record to the so invocation record list;
     * the invocation will be executed later
     */
    public void addSOInvocation(SOInvocationRecord soir) {
        synchronized (s) {
            soInvocationList.add(soir);
            gotSOInvocations = true;
            s.notifyAll();
        }
    }

    /** returns false if the job must be aborted */
    public boolean executeGuard(InvocationRecord r) {   
        s.stats.soGuardTimer.start();
        try {
            doExecuteGuard(r);
        } catch (SOReferenceSourceCrashedException e) {
            //the source has crashed - abort the job
            return false;
        } finally {
            s.stats.soGuardTimer.stop();
        }
        return true;
    }

    /**
     * Execute the guard of the invocation record r, wait for updates, if
     * necessary, ship objects if necessary
     */
    private void doExecuteGuard(InvocationRecord r)
            throws SOReferenceSourceCrashedException {
        // restore shared object references
        r.setSOReferences();

        if (r.guard()) return;

        soLogger.info("SATIN '" + s.ident.name() + "': "
            + "guard not satisfied, getting updates..");

        // try to ship the object(s) from the owner of the job
        Vector objRefs = r.getSOReferences();
        if (objRefs == null || objRefs.isEmpty()) {
            soLogger
                .fatal("SATIN '" + s.ident.name() + "': "
                    + "a guard is not satisfied, but the spawn does not " +
                                "have shared objects.\n" + 
                                "This is not a correct Satin program.");
            System.exit(1);
        }

        // A shared object update may have arrived
        // during one of the fetches.
        while (true) {
            s.handleDelayedMessages();
            if (r.guard()) {
                return;
            }

            String ref = (String) objRefs.remove(0);
            soComm.fetchObject(ref, r.getOwner(), r);
        }
    }

    public void addToSORequestList(IbisIdentifier requester, String objID, boolean demand) {
        Satin.assertLocked(s);
        SORequestList.add(requester, objID, demand);
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

    public void handleSORequest(ReadMessage m, boolean demand) {
        soComm.handleSORequest(m, demand);
    }

    public void handleSOTransfer(ReadMessage m) {
        soComm.handleSOTransfer(m);
    }

    public void handleSONack(ReadMessage m) {
        soComm.handleSONack(m);
    }

    public void handleJoins(IbisIdentifier[] joiners) {
        soComm.handleJoins(joiners);
    }

    public void handleMyOwnJoin() {
        soComm.handleMyOwnJoin();
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

    public void handleCrash(IbisIdentifier id) {
        soComm.handleCrash(id);
    }
    
    public void exit() {
        soComm.exit();
    }
}
