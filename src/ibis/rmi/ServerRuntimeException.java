/* $Id$ */

package ibis.rmi;

/**
 * A <code>ServerRuntimeException</code> is thrown when a
 * <code>RuntimeException</code> is thrown while processing a remote method
 * invocation on the server.
 */
public class ServerRuntimeException extends RemoteException {
    /** 
     * Generated
     */
    private static final long serialVersionUID = -2791906070755014943L;

    /**
     * Constructs a <code>ServerRuntimeException</code> with the specified
     * detail message and nested exception.
     * @param s the detail message
     * @param ex the nested exception
     */
    public ServerRuntimeException(String s, Exception ex) {
        super(s, ex);
    }
}