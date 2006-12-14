/* $Id$ */

package ibis.rmi.server;

/**
 * A <code>ServerNotActiveException</code> is thrown from a
 * {@link ServerRef#getClientHost} invocation when this invocation does
 * not come from a thread that is currently servicing a remote method
 * invocation.
 */
public class ServerNotActiveException extends Exception {
    /**
     * Constructs an <code>ServerNotActiveException</code> with no specified
     * detail message.
     */
    public ServerNotActiveException() {
        super();
    }

    /**
     * Constructs an <code>ServerNotActiveException</code> with the specified
     * detail message.
     * @param s the detail message
     */
    public ServerNotActiveException(String s) {
        super(s);
    }
}