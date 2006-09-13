/* $Id$ */

package ibis.satin.impl.sharedObjects;

import ibis.satin.SharedObject;

public abstract class SOInvocationRecord implements java.io.Serializable {

    private String objectId;

    public SOInvocationRecord(String objectId) {
        this.objectId = objectId;
    }

    protected abstract void invoke(SharedObject object);

    protected String getObjectId() {
        return objectId;
    }
}
