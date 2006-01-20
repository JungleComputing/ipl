package ibis.satin;

import ibis.satin.impl.Satin;

/**
 * A Satin shared object must be of a class that extends this
 * <code>SharedObject</code> class.
 * This class exports one method that actually marks the object as shared.
 */
public class SharedObject implements java.io.Serializable {

    /**
     * Identification of this shared object. Must be public because it is
     * accessed from code that is rewritten by Satinc.
     */
    public String objectId;

    /** Counter for generating shared-object identifications. */
    private static int sharedObjectsCounter = 0;

    protected SharedObject() {
    	// does nothing
    }

    /**
     * Creates an identification for the current object and marks it as shared.
     */
    public void exportObject() {
        Satin satin = Satin.getSatin();

        if (satin != null) {
            //create identifier
            sharedObjectsCounter++;
            objectId = "satin_shared_object" + sharedObjectsCounter + "@"
                + Satin.getSatinIdent().name();

            //add yourself to the sharedObjects hashtable
            satin.addObject(this);

            synchronized (satin) {
                satin.broadcastSharedObject(this);
            }
        } else {
            System.err.println("EEEKK, could not find satin");
            System.exit(1);
        }
    }
}
