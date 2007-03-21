/* $Id$ */

package ibis.rmi;

/**
 * A <code>ServerError</code> is thrown when an
 * <code>Error</code> is thrown while processing a remote method
 * invocation on the server.
 */
public class ServerError extends RemoteException {
    /** 
     * Generated
     */
    private static final long serialVersionUID = 5072214030682233117L;

    /**
     * Constructs a <code>ServerError</code> with the specified
     * detail message and nested exception.
     * @param s the detail message
     * @param ex the nested exception
     */
    public ServerError(String s, Error ex) {
        super(s, ex);
    }
}