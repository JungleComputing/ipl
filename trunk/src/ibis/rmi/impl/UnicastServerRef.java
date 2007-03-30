/* $Id$ */

package ibis.rmi.impl;

import ibis.rmi.Remote;
import ibis.rmi.RemoteException;
import ibis.rmi.server.RemoteStub;
import ibis.rmi.server.ServerRef;

import java.io.ObjectOutput;

// TODO: implement getClientHost

public class UnicastServerRef extends UnicastRef implements ServerRef,
        java.io.Serializable {
    /** 
     * Generated
     */
    private static final long serialVersionUID = -8098955765393733420L;

    public UnicastServerRef() {
        GUID = "//" + RTS.getHostname() + "/"
                + (new java.rmi.server.UID()).toString();
    }

    public RemoteStub exportObject(Remote impl, Object portData)
            throws RemoteException {
        try {
            RemoteStub stub = RTS.exportObject(impl, new UnicastRef(GUID));
            return stub;
        } catch (Exception e) {
            throw new RemoteException(e.getMessage(), e);
        }
    }

    public String getRefClass(ObjectOutput out) {
        return "UnicastServerRef";
    }

    public String getClientHost() {
        // TODO:
        return null;
    }
}
