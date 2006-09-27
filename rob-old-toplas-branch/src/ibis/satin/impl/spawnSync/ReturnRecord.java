/* $Id$ */

package ibis.satin.impl.spawnSync;

/**
 * A return record describes the result (return value) of a spawned invocation.
 * The Satin frontend generates a subclass of this class for each caller of a
 * spawnable method. The return value is also represented in the invocation
 * record, but must be represented separately to prevent the invocation
 * parameters from being serialized and sent over the network twice.
 */
public abstract class ReturnRecord implements java.io.Serializable {
    protected Stamp stamp;

    /**
     * The exception or error thrown by the spawned invocation. May (of course)
     * be <code>null</code>.
     */
    protected Throwable eek = null;

    /**
     */
    private int updatesSent;

    protected ReturnRecord(Throwable eek) {
        this.eek = eek;
    }

    /**
     * Extracts the return value from this return record, and stores it in the
     * given invocation record.
     * 
     * @param r
     *            the invocation record.
     */
    public abstract void assignTo(InvocationRecord r);

    public void setStamp(Stamp stamp) {
        this.stamp = stamp;
    }

    public Stamp getStamp() {
        return stamp;
    }

    public void setEek(Throwable eek) {
        this.eek = eek;
    }

    public Throwable getEek() {
        return eek;
    }

    protected void setUpdatesSent(int updatesSent) {
        this.updatesSent = updatesSent;
    }

    protected int getUpdatesSent() {
        return updatesSent;
    }
}
