/* $Id$ */

package ibis.rmi;

/**
 * A <code>ServerException</code> is thrown when a
 * <code>RemoteException</code> is thrown while processing a remote method
 * invocation on the server.
 */
public class ServerException extends RemoteException {

    /** 
     * Generated
     */
    private static final long serialVersionUID = 545208981874643134L;

    /**
     * Constructs a <code>ServerException</code> with the specified
     * detail message.
     * @param s the detail message
     */
    public ServerException(String s) {
        super(s);
    }

    /**
     * Constructs a <code>ServerException</code> with the specified
     * detail message and nested exception.
     * @param s the detail message
     * @param ex the nested exception
     */
    public ServerException(String s, Exception ex) {
        super(s, ex);
    }
}