/* $Id$ */

package ibis.satin.impl;

import ibis.ipl.IbisIdentifier;

/**
 * An invocation record describes a spawned invocation, including the parameters
 * of the invocation. The Satin frontend generates a subclass of this class for
 * each spawnable method.
 */

public abstract class InvocationRecord implements java.io.Serializable, Config {
    /*
     * Of all fields, only the owner and the stamp must be sent over the
     * network. Parents too.
     */

    protected IbisIdentifier owner;

    /**
     * Used to locate this invocation record, when a remote
     * job result comes in.
     */
    protected int stamp;

    protected IbisIdentifier parentOwner;

    protected int parentStamp;

    protected transient InvocationRecord parent;

    /**
     * List of finished children, used for fault tolerance
     */
    public transient InvocationRecord finishedChild;

    /**
     * List of finished children, used for fault tolerance
     */
    public transient InvocationRecord finishedSibling;

    /**
     * List of children which need to be restarted, used for fault tolerance
     */
    public transient InvocationRecord toBeRestartedChild;

    /**
     * List of children which need to be restarted, used for fault tolerance
     */
    public transient InvocationRecord toBeRestartedSibling;

    /**
     * Number of _all_ spawned children (not spawned and not finished like with
     * spawnCounter) Used for generating globally unique stamps
     */
    public transient int numSpawned = 0;

    protected transient SpawnCounter spawnCounter;

    public transient Throwable eek; // must be public, it is used from the

    // generated code (in another package) --Rob
    public transient InvocationRecord cacheNext; /*
     * Used to link the records in
     * the cache.
     */

    public transient boolean aborted; // must be public, is accessed from

    // generated code. --Rob

    /*
     * These are used to link the records in the JobQueue. Not needed when
     * Dijkstra is used.
     */
    protected transient InvocationRecord qprev;

    protected transient InvocationRecord qnext;

    public transient int storeId; /*
     * An id for the store where the result of the
     * spawn must go.
     */

    protected transient int spawnId; /*
     * An id for the spawn in the code. Needed
     * to run the correct inlet.
     */

    protected transient LocalRecord parentLocals;

    protected transient IbisIdentifier stealer;

    /**
     * Used for fault tolerance True means, that the job is being redone after a
     * crash
     */
    public boolean reDone = false;

    /** 
     * Used for fault tolerance
     * True means, that the job is an orphan
     **/
    public boolean orphan = false;

    transient boolean alreadySentExceptionResult;

    protected transient boolean inletExecuted;

    // ArrayList parentStamps;
    // ArrayList parentOwners;

    protected InvocationRecord(SpawnCounter spawnCounter,
            InvocationRecord cacheNext, int storeId, int spawnId,
            LocalRecord parentLocals) {
        this.storeId = storeId;
        this.cacheNext = cacheNext;
        this.spawnCounter = spawnCounter;
        if (ABORTS) {
            this.spawnId = spawnId;
            this.parentLocals = parentLocals;
            aborted = false;
        } else if (FAULT_TOLERANCE) {
            this.spawnId = spawnId;
        }

        // parentStamps = new ArrayList();
        // parentOwners = new ArrayList();

        alreadySentExceptionResult = false;
        inletExecuted = false;
    }

    //.public abstract void delete();

    final protected void clear() {
        owner = null;
        stamp = -2;
        spawnCounter = null;

        qprev = null;
        qnext = null;

        storeId = -2;
        stealer = null;

        if (ABORTS || FAULT_TOLERANCE) {
            eek = null;
            parentOwner = null;
            parentStamp = -2;
            parent = null;
            aborted = false;
            spawnId = -2;
            parentLocals = null;

            // parentStamps.clear();
            // parentOwners.clear();

            alreadySentExceptionResult = false;
            inletExecuted = false;
        }

        if (FAULT_TOLERANCE) {
            numSpawned = 0;
            reDone = false;
        }
        if (FAULT_TOLERANCE) {
            finishedChild = null;
            finishedSibling = null;
            toBeRestartedChild = null;
            toBeRestartedSibling = null;
        }

    }

    /**
     * Compares this invocation record with another invocation record. Returns
     * <code>true</code> if equal.
     * 
     * @param other
     *            the invocation record to compare with.
     * @return <code>true</code> if equal, <code>false</code> if not.
     */
    public boolean equals(InvocationRecord other) {
        if (other == this) {
            return true;
        }
        return stamp == other.stamp && owner.equals(other.owner);
    }

    /**
     * Compares this invocation record with another object. Returns
     * <code>true</code> if equal.
     * 
     * @param o
     *            the object to compare with.
     * @return <code>true</code> if equal, <code>false</code> if not.
     */
    final public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof InvocationRecord) {
            InvocationRecord other = (InvocationRecord) o;
            return stamp == other.stamp && owner.equals(other.owner);
        }
        if (Config.ASSERTS) {
            System.out.println("warning: weird equals in Invocationrecord");
        }
        return false;
    }

    /**
     * Returns a hashcode that conforms with the <code>equals</code> method.
     * 
     * @return a hashcode.
     */
    final public int hashCode() {
        return stamp;
    }

    /**
     * Returns a string representation of this invocation record.
     * 
     * @return a string representation of this invocation record.
     */
    public String toString() {
        String result = "(Invocation record: stamp = " + stamp;
        result += ", owner = " + (owner == null ? "NULL" : "" + owner);
        result += ", spawnCounter = "
                + (spawnCounter == null ? "NULL" : "" + spawnCounter.value);
        result += ", stealer = " + stealer;
        result += ", parentStamp = " + parentStamp;
        result += ", parentOwner = "
                + (parentOwner == null ? "NULL" : "" + parentOwner.name());
        result += ", aborted = " + aborted;
        result += ", parent = " + (parent == null ? "NULL" : "" + parent);
        // recursive :-)
        result += ", parentLocals = "
                + (parentLocals == null ? "NULL" : "" + parentLocals) + ")";

        return result;
    }

    // used for fault tolerance
    public abstract ParameterRecord getParameterRecord();

    public abstract ReturnRecord getReturnRecord();

    /**
     * checks whether the parameters in this invocation record are the same as
     * parameters in the given parameter record
     */
    public abstract boolean equalsPR(ParameterRecord pr);

    protected abstract void runLocal() throws Throwable;

    protected abstract ReturnRecord runRemote();

}
