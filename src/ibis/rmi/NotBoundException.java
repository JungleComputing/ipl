/* $Id$ */

package ibis.rmi;

/**
 * A <code>NotBoundException</code> is thrown if an attempt is made
 * to lookup or unbind a name that has no binding.
 */
public class NotBoundException extends ibis.ipl.IbisException {
    /**
     * Constructs an <code>NotBoundException</code> with no specified
     * detail message.
     */
    public NotBoundException() {
        super();
    }

    /**
     * Constructs an <code>NotBoundException</code> with the specified
     * detail message.
     * @param s the detail message
     */
    public NotBoundException(String s) {
        super(s);
    }

    /**
     * Constructs an <code>NotBoundException</code> with the specified
     * detail message and cause.
     * @param s the detail message
     * @param e the cause
     */
    public NotBoundException(String s, Throwable e) {
        super(s, e);
    }
}