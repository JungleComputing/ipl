/* $Id$ */

package ibis.satin;

import ibis.satin.impl.Satin;

/**
 * A Satin shared object must be of a class that extends this
 * <code>SharedObject</code> class.
 * This class exports one method that actually marks the object as shared.
 */
public class SharedObject implements java.io.Serializable {

    /** 
     * Generated
     */
    private static final long serialVersionUID = -5944640339385558907L;

    /** Identification of this shared object. */
    private String objectId;

    /** Counter for generating shared-object identifications. */
    private static int sharedObjectsCounter = 0;

    private boolean exported = false;

    private boolean isUnshared = true;

    /**
     * Creates an identification for the current object and marks it as shared.
     */
    protected SharedObject() {
        Satin satin = Satin.getSatin();

        if (satin == null) {
            // Assuming sequential run, not rewritten code.
            return;
        }

        //create identifier
        sharedObjectsCounter++;
        objectId = "SO" + sharedObjectsCounter + "@"
            + Satin.getSatin().ident;

        //add yourself to the sharedObjects hashtable
        satin.so.addObject(this);
    }

    /**
     * Returns true if the shared object is still local.
     * Write operations that are done while the object is still local
     * are not broadcasted.
     * As soon as an invocation record is made with this object,
     * or exportObject is called, the object is considered non-local.
     * @return true if the object is still local.
     */
    public final boolean isUnshared() {
        return isUnshared;
    }

    /**
     * This method is optional, and can be used after creating a shared object.
     * It allows Satin to immediately distribute a replica to all machines
     * participating in the application.
     * This way, machines won't have to ask for it later.
     */
    public void exportObject() {
        if (exported) {
            throw new RuntimeException(
                "you cannot export an object more than once.");
        }

        Satin satin = Satin.getSatin();

        if (satin == null) {
            // Assuming sequential run, not rewritten code.
            return;
        }

        if (! isUnshared) {
            throw new RuntimeException(
                    "write method invoked before exportObject");
        }

        isUnshared = false;

        synchronized (satin) {
            satin.so.broadcastSharedObject(this);
            exported = true;
        }
    }

    public final String getObjectId() {
        return objectId;
    }

    public final String getObjectIdAndSetNonlocal() {
        isUnshared = false;
        return objectId;
    }
}
