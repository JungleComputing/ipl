/* $Id$ */

package ibis.satin.impl.sharedObjects;

import ibis.satin.SharedObject;

public abstract class SOInvocationRecord implements java.io.Serializable {

    public String objectId;

    public SOInvocationRecord(String objectId) {
        this.objectId = objectId;
    }

    public abstract void invoke(SharedObject object);
}
