/* $Id$ */

package ibis.rmi.server;

import java.io.Externalizable;

/**
 * A <code>RemoteRef</code> represents the handle for a remote object.
 * A {@link ibis.rmi.server.RemoteStub RemoteStub} uses a remote reference
 * for remote method invocation.
 */
public interface RemoteRef extends Externalizable {
    /**
     * Returns the class name of the reference type to be serialized
     * to the specified stream.
     * @param out the output stream to which the reference will be 
     *  serialized
     * @return the class name of the reference type
     */
    public String getRefClass(java.io.ObjectOutput out);

    /**
     * Returns a hashcode for a remote object. Two stubs that refer to
     * the same remote object will have the same hash code.
     * @return the remote object hashcode
     */
    public int remoteHashCode();

    /**
     * Compares to remote objects for equality.
     * @param obj a reference to the remote object to compare with.
     * @return the result of the comparison.
     */
    public boolean remoteEquals(RemoteRef obj);

    /**
     * Returns a string representing this remote object reference.
     * @return a string representing this remote object reference
     */
    public String remoteToString();
}