/* $Id$ */

package ibis.satin.impl.spawnSync;

import ibis.ipl.IbisIdentifier;
import ibis.satin.impl.Config;
import ibis.satin.impl.aborts.LocalRecord;
import ibis.satin.impl.sharedObjects.SOReferenceSourceCrashedException;

/**
 * An invocation record describes a spawned invocation, including the parameters
 * of the invocation. The Satin frontend generates a subclass of this class for
 * each spawnable method.
 * Of all fields, only the owner, the stamp and the parent info must be sent
 * over the network.
 */
public abstract class InvocationRecord implements java.io.Serializable, Config {

    /**
     * Must be public, it is used from the generated code (in another package)
     */
    public transient Throwable eek;

    /**
     * The machine that spawned this job.
     * Used by my subclasses.
     */
    protected IbisIdentifier owner;

    /**
     * Used to locate this invocation record, when a remote
     * job result comes in.
     */
    private Stamp stamp;

    /**
     * The machine that spawned my parent (can be null for root jobs).
     */
    private IbisIdentifier parentOwner;

    /**
     * The stamp of my parent (can be null for root jobs).
     */
    private Stamp parentStamp;

    /**
     * The invocation record of my parent (can be null for root and stolen jobs).
     */
    protected transient InvocationRecord parent;

    private transient SpawnCounter spawnCounter;

    /** 
     * Must be public, is accessed from generated code.
     */
    public transient boolean aborted;

    /**
     * An id for the store where the result of the
     * spawn must go. Must be public, used by generated code.
     */
    public transient int storeId;

    /**
     * These are used to link the records in the JobQueue.
     */
    private transient InvocationRecord qprev;

    private transient InvocationRecord qnext;

    /**
     * Used to link the records in
     * the cache. Used by generated code.
     */
    public transient InvocationRecord cacheNext;

    /**
     * An id for the spawn in the code. Needed
     * to run the correct inlet.
     */
    protected transient int spawnId;

    protected transient LocalRecord parentLocals;

    private transient IbisIdentifier stealer;

    private transient boolean alreadySentExceptionResult;

    // Used by my subclasses
    protected transient boolean inletExecuted;

    /**
     * List of finished children; used for fault tolerance.
     */
    private transient InvocationRecord finishedChild;

    /**
     * List of finished children; used for fault tolerance.
     */
    private transient InvocationRecord finishedSibling;

    /**
     * List of children which need to be restarted; used for fault tolerance.
     */
    private transient InvocationRecord toBeRestartedChild;

    /**
     * List of children which need to be restarted; used for fault tolerance.
     */
    private transient InvocationRecord toBeRestartedSibling;

    /**
     * Used for fault tolerance. True means that the job is being redone after a
     * crash.
     */
    private boolean reDone;

    /** 
     * Used for fault tolerance.
     * True means that the job is an orphan
     **/
    private boolean orphan;

    protected InvocationRecord(SpawnCounter spawnCounter,
        InvocationRecord cacheNext, int storeId, int spawnId,
        LocalRecord parentLocals) {
        init(spawnCounter, cacheNext, storeId, spawnId, parentLocals);
    }

    /** Used for the invocation record cache. */
    final protected void init(SpawnCounter spawnCounter,
        InvocationRecord cacheNext, int storeId, int spawnId,
        LocalRecord parentLocals) {
        this.storeId = storeId;
        this.cacheNext = cacheNext;
        this.spawnCounter = spawnCounter;
        this.spawnId = spawnId;
        this.parentLocals = parentLocals;
    }

    /** Used for the invocation record cache. */
    final protected void clear() {
        owner = null;
        Stamp.deleteStamp(stamp);
        stamp = null;
        spawnCounter = null;

        qprev = null;
        setQnext(null);

        storeId = -2;
        stealer = null;

        eek = null;
        parentOwner = null;
        parentStamp = null;
        parent = null;
        aborted = false;
        spawnId = -2;
        parentLocals = null;

        alreadySentExceptionResult = false;
        inletExecuted = false;
        reDone = false;
        finishedChild = null;
        finishedSibling = null;
        toBeRestartedChild = null;
        toBeRestartedSibling = null;
    }

    /**
     * Compares this invocation record with another invocation record. Returns
     * <code>true</code> if equal.
     * 
     * @param other
     *            the invocation record to compare with.
     * @return <code>true</code> if equal, <code>false</code> if not.
     */
    public final boolean equals(InvocationRecord other) {
        if (other == this) {
            return true;
        }
        return stamp.stampEquals(other.stamp) && owner.equals(other.owner);
    }

    /**
     * Compares this invocation record with another object. Returns
     * <code>true</code> if equal.
     * 
     * @param o
     *            the object to compare with.
     * @return <code>true</code> if equal, <code>false</code> if not.
     */
    public final boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof InvocationRecord) {
            InvocationRecord other = (InvocationRecord) o;
            return stamp.stampEquals(other.stamp) && owner.equals(other.owner);
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
        return stamp.hashCode();
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
            + (spawnCounter == null ? "NULL" : "" + spawnCounter.getValue());
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

    public abstract ReturnRecord getReturnRecord();

    /** initializes the references to shared objects inside this
     invocation record after stealing the job */
    public abstract void setSOReferences()
        throws SOReferenceSourceCrashedException;

    /** Returns a list of objectIds of the shared objects this
     record holds references of. */
    public abstract java.util.Vector<String> getSOReferences();

    /** Executes the guard function, used for shared objects consistency. */
    public boolean guard() {
        return true;
    }

    public abstract void runLocal() throws Throwable;

    public abstract ReturnRecord runRemote();

    public abstract void clearParams();

    public final IbisIdentifier getOwner() {
        return owner;
    }

    public final void setOwner(IbisIdentifier owner) {
        this.owner = owner;
    }

    public final Stamp getStamp() {
        return stamp;
    }

    public final InvocationRecord getParent() {
        return parent;
    }

    public final IbisIdentifier getParentOwner() {
        return parentOwner;
    }

    public final Stamp getParentStamp() {
        return parentStamp;
    }

    public final void decrSpawnCounter() {
        spawnCounter.decr(this);
    }

    public final void incrSpawnCounter() {
        spawnCounter.incr(this);
    }

    public final void setStealer(IbisIdentifier stealer) {
        this.stealer = stealer;
    }

    public final IbisIdentifier getStealer() {
        return stealer;
    }

    public final void setFinishedChild(InvocationRecord finishedChild) {
        this.finishedChild = finishedChild;
    }

    public final InvocationRecord getFinishedChild() {
        return finishedChild;
    }

    public final void setFinishedSibling(InvocationRecord finishedSibling) {
        this.finishedSibling = finishedSibling;
    }

    public final InvocationRecord getFinishedSibling() {
        return finishedSibling;
    }

    public final void setToBeRestartedChild(InvocationRecord toBeRestartedChild) {
        this.toBeRestartedChild = toBeRestartedChild;
    }

    public final InvocationRecord getToBeRestartedChild() {
        return toBeRestartedChild;
    }

    public final void setToBeRestartedSibling(
        InvocationRecord toBeRestartedSibling) {
        this.toBeRestartedSibling = toBeRestartedSibling;
    }

    public final InvocationRecord getToBeRestartedSibling() {
        return toBeRestartedSibling;
    }

    public final void setReDone(boolean reDone) {
        this.reDone = reDone;
    }

    public final boolean isReDone() {
        return reDone;
    }

    public final void setOrphan(boolean orphan) {
        this.orphan = orphan;
    }

    public final boolean isOrphan() {
        return orphan;
    }

    public final int getSpawnId() {
        return spawnId;
    }

    protected final void setParentLocals(LocalRecord parentLocals) {
        this.parentLocals = parentLocals;
    }

    public final LocalRecord getParentLocals() {
        return parentLocals;
    }

    public final void setAlreadySentExceptionResult(
        boolean alreadySentExceptionResult) {
        this.alreadySentExceptionResult = alreadySentExceptionResult;
    }

    public final boolean alreadySentExceptionResult() {
        return alreadySentExceptionResult;
    }

    public final void setInletExecuted(boolean inletExecuted) {
        this.inletExecuted = inletExecuted;
    }

    public final boolean isInletExecuted() {
        return inletExecuted;
    }

    /**
     * Determines if the specified invocation record is a descendent of
     * the job indicated by the specied stamp.
     */
    public final boolean isDescendentOf(Stamp targetStamp) {
        if (parentStamp == null) {
            if (targetStamp == null) {
                return true;
            }
            return false;
        }
        return parentStamp.isDescendentOf(targetStamp);
    }

    public final boolean isDescendentOf(IbisIdentifier targetOwner) {
        if (parent == null) {
            return false;
        }

        if (parentOwner.equals(targetOwner)) {
            return true;
        }
        return parent.isDescendentOf(targetOwner);
    }

    /**
     * Attach a child to its parent's finished children list.
     */
    public final void jobFinished() {
        if (parent != null) {
            finishedSibling = parent.finishedChild;
            parent.finishedChild = this;
        }

        //remove the job's children list
        finishedChild = null;
    }

    public final void spawn(IbisIdentifier ident, InvocationRecord parent) {
        owner = ident;
        this.parent = parent;
        if (parent == null) {
            parentStamp = null;
            parentOwner = null;
            stamp = Stamp.createStamp(null);
        } else {
            parentStamp = parent.stamp;
            parentOwner = parent.owner;
            stamp = Stamp.createStamp(parent.stamp);
        }
        spawnCounter.incr(this);
    }

    protected final void setQprev(InvocationRecord qprev) {
        this.qprev = qprev;
    }

    protected final InvocationRecord getQprev() {
        return qprev;
    }

    protected final void setQnext(InvocationRecord qnext) {
        this.qnext = qnext;
    }

    protected final InvocationRecord getQnext() {
        return qnext;
    }
}
