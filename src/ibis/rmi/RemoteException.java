/* $Id$ */

package ibis.rmi;

/**
 * A <code>RemoteException</code> is the common superclass for several
 * exceptions that may occur during a remove method invocation.
 */
public class RemoteException extends ibis.ipl.IbisIOException {
    /**
     * Constructs an <code>RemoteException</code> with no specified
     * detail message.
     */
    public RemoteException() {
        super();
    }

    /**
     * Constructs an <code>RemoteException</code> with the specified
     * detail message.
     * @param s the detail message
     */
    public RemoteException(String s) {
        super(s);
    }

    /**
     * Constructs an <code>RemoteException</code> with the specified
     * detail message and cause.
     * @param s the detail message
     * @param e the cause
     */
    public RemoteException(String s, Throwable e) {
        super(s);
        initCause(e);
    }

    /**
     * Constructs an <code>RemoteException</code> with no specified
     * detail message and specified cause
     * @param e the cause
     */
    public RemoteException(Throwable e) {
        super();
        initCause(e);
    }
}