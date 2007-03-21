/* $Id$ */

package ibis.rmi.server;

import ibis.rmi.Remote;
import ibis.rmi.RemoteException;
import ibis.rmi.impl.UnicastServerRef;

import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * The <code>UnicastRemoteObject</code> class defines a remote object
 * that is only valid while its server process is alive.
 * A remote object should extend <code>RemoteObject</code>, usually by
 * extending <code>UnicastRemoteObject</code>.
 */
public class UnicastRemoteObject extends RemoteServer {
    /** 
     * Generated
     */
    private static final long serialVersionUID = 5245546242783456894L;

    /**
     * Creates and exports a new UnicastRemoteObject object.
     * @throws RemoteException if it failed to export the object
     */
    protected UnicastRemoteObject() throws RemoteException {
        exportObject(this);
    }

    /**
     * Returns a clone of the remote object.
     * @return the new remote object
     * @exception CloneNotSupportedException if clone failed due to
     *  a RemoteException
     */
    public Object clone() throws CloneNotSupportedException {
        try {
            UnicastRemoteObject r = (UnicastRemoteObject) (super.clone());
            r.reexport();
            return r;
        } catch (RemoteException e) {
            throw new CloneNotSupportedException("Clone failed: "
                    + e.toString());
        }
    }

    /**
     * Exports the remote object to allow it to receive incoming calls.
     * @param obj the remote object to be exported
     * @return the remote object stub
     * @exception RemoteException if the export fails
     */
    public static RemoteStub exportObject(Remote obj) throws RemoteException {
        // Use exportObject from the "current" UnicastServerRef".
        ServerRef ref = new UnicastServerRef();
        if (obj instanceof RemoteObject) {
            if (((RemoteObject) obj).ref != null) {
                throw new ExportException("object already exported");
            }
            ((RemoteObject) obj).ref = ref;
        }
        return ref.exportObject(obj, null);
    }

    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {
        in.defaultReadObject();
        reexport();
    }

    private void reexport() throws RemoteException {
        if (ref != null) {
            ((UnicastServerRef) ref).exportObject(this, null);
        } else {
            exportObject(this);
        }
    }
}