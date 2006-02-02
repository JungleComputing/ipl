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
     * Identification of this shared object. Must be public because it is
     * accessed from code that is rewritten by Satinc.
     */
    public String objectId;

    /** Counter for generating shared-object identifications. */
    private static int sharedObjectsCounter = 0;

    private boolean exported = false;
    
    /**
     * Creates an identification for the current object and marks it as shared.
     */
    protected SharedObject() {
        Satin satin = Satin.getSatin();

        if (satin == null) {
            System.err.println("EEEKK, could not find satin");
            System.exit(1);
        }

        //create identifier
        sharedObjectsCounter++;
        objectId = "satin_shared_object" + sharedObjectsCounter + "@"
            + Satin.getSatinIdent().name();

        //add yourself to the sharedObjects hashtable
        satin.addObject(this);
    }

    /**
     * This method is optional, and can be used after creating a shared object.
     * It allows Satin to immediately distribute a replica to all machines
     * participating in the application.
     * This way, machines won't have to ask for it later.
     */
    public void exportObject() {
        if(exported) {
            throw new RuntimeException("you cannot export an object more than once.");
        }
        
        Satin satin = Satin.getSatin();

        if (satin == null) {
            System.err.println("EEEKK, could not find satin");
            System.exit(1);
        }

        synchronized (satin) {
            satin.broadcastSharedObject(this);
            exported = true;
        }
    }
}
