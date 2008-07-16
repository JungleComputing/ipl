/* $Id$ */

package ibis.util;

/**
 * An <code>IllegalLockStateException</code> is thrown to indicate that a
 * thread has attempted to lock, unlock, wait, or notify a {@link Monitor}
 * that it does not own, or that has been cleaned up.
 */
public class IllegalLockStateException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a <code>AlreadyConnectedException</code> with
     * <code>null</code> as its error detail message.
     */
    public IllegalLockStateException() {
        super();
    }

    /**
     * Constructs a <code>IllegalLockStateException</code> with
     * the specified detail message.
     * @param s         the detail message
     */
    public IllegalLockStateException(String s) {
        super(s);
    }

    /**
     * Constructs a <code>IllegalLockStateException</code> with
     * the specified detail message and cause
     * @param s         the detail message
     * @param cause     the cause
     */
    public IllegalLockStateException(String s, Throwable cause) {
        super(s, cause);
    }

    /**
     * Constructs a <code>IllegalLockStateException</code> with
     * the specified cause
     * @param cause     the cause
     */
    public IllegalLockStateException(Throwable cause) {
        super(cause);
    }
}
