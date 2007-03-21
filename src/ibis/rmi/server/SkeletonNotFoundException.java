/* $Id$ */

package ibis.rmi.server;

import ibis.rmi.RemoteException;

/**
 * A <code>SkeletonNotFoundException</code> is thrown if the skeleton
 * corresponding to the remote object that is being exported is not found.
 * Note: in Ibis RMI, we <strong>do</strong> have skeletons.
 */
public class SkeletonNotFoundException extends RemoteException {

    /** 
     * Generated
     */
    private static final long serialVersionUID = -9033123731524006368L;

    /**
     * Constructs an <code>SkeletonNotFoundException</code> with the specified
     * detail message.
     * @param s the detail message
     */
    public SkeletonNotFoundException(String s) {
        super(s);
    }

    /**
     * Constructs an <code>SkeletonNotFoundException</code> with the specified
     * detail message and nested exception.
     * @param s the detail message
     * @param ex the nested exception
     */
    public SkeletonNotFoundException(String s, Exception ex) {
        super(s, ex);
    }
}