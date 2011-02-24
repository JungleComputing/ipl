/* $Id$ */

package ibis.ipl;

/**
 * Signals that a connection has been closed. A
 * <code>ConnectionClosedException</code> is thrown to indicate
 * that an input or output operation has been terminated because
 * the connection was broken.
 */
public class ConnectionClosedException extends IbisIOException {
    private static final long serialVersionUID = 1L;

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
     * @param detailMessage
     *          the detail message
     */
    public ConnectionClosedException(String detailMessage) {
        super(detailMessage);
    }

    /**
     * Constructs a <code>ConnectionClosedException</code> with
     * the specified detail message and cause.
     *
     * @param detailMessage
     *          the detail message
     * @param cause
     *          the cause
     */
    public ConnectionClosedException(String detailMessage, Throwable cause) {
        super(detailMessage);
        initCause(cause);
    }

    /**
     * Constructs a <code>ConnectionClosedException</code> with
     * the specified cause.
     *
     * @param cause
     *          the cause
     */
    public ConnectionClosedException(Throwable cause) {
        super();
        initCause(cause);
    }
}
