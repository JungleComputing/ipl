/* $Id$ */

package ibis.ipl;

/**
 * Signals that a connection has been closed. A
 * <code>ConnectionClosedException</code> is thrown to indicate
 * that an input or output operation has been terminated because
 * the connection was broken.
 */
public class ConnectionClosedException extends java.io.IOException {

    /** 
     * Generated
     */
    private static final long serialVersionUID = 9097069370016270215L;

    /**
     * Constructs a <code>ConnectionClosedException</code> with
     * <code>null</code> as its error detail message.
     */
    public ConnectionClosedException() {
        super();
    }

    /**
     * Constructs a <code>ConnectionClosedException</code> with
     * the specified detail message.
     *
     * @param s		the detail message
     */
    public ConnectionClosedException(String s) {
        super(s);
    }

    /**
     * Constructs a <code>ConnectionClosedException</code> with
     * the specified detail message and cause.
     *
     * @param s		the detail message
     * @param cause	the cause
     */
    public ConnectionClosedException(String s, Throwable cause) {
        super(s);
        initCause(cause);
    }

    /**
     * Constructs a <code>ConnectionClosedException</code> with
     * the specified cause.
     *
     * @param cause	the cause
     */
    public ConnectionClosedException(Throwable cause) {
        super();
        initCause(cause);
    }
}
