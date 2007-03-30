/* $Id$ */

package ibis.rmi.server;

import ibis.rmi.Remote;
import ibis.rmi.RemoteException;

/**
 * A <code>ServerRef</code> represents the server side handle for a
 * remote object.
 */
public interface ServerRef extends RemoteRef {

    /**
     * Creates a stub object for the remote object specified.
     * @param obj the remote object (implementation)
     * @param data information for exporting the object
     * @return a stub for the remote object
     * @exception RemoteException if the export fails
     */
    public RemoteStub exportObject(Remote obj, Object data)
            throws RemoteException;

    /**
     * Returns the hostname of the current client, but only when called
     * from a thread handling a remote method invocation. If not, an
     * exception is thrown.
     * @return the hostname of the client
     * @exception ServerNotActiveException if not called from a thread that
     *  is currently servicing a remote method invocation
     */
    public String getClientHost() throws ServerNotActiveException;
}