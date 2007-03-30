/* $Id$ */

package ibis.rmi;

/**
 * An <code>AlreadyBoundException</code> is thrown if an attempt
 * is made to bind an object in the registry to a name that is
 * already in use.
 */
public class AlreadyBoundException extends Exception {
    
    private static final long serialVersionUID = -913191118620596055L;

    /**
     * Constructs an <code>AlreadyBoundException</code> with no specified
     * detail message.
     */
    public AlreadyBoundException() {
        super();
    }

    /**
     * Constructs an <code>AlreadyBoundException</code> with the specified
     * detail message.
     * @param s the detail message
     */
    public AlreadyBoundException(String s) {
        super(s);
    }

    /**
     * Constructs an <code>AlreadyBoundException</code> with the specified
     * detail message and cause.
     * @param s the detail message
     * @param e the cause
     */
    public AlreadyBoundException(String s, Throwable e) {
        super(s, e);
    }
}
