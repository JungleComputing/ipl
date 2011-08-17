/* $Id$ */

package ibis.ipl;

/**
 * Signals a timeout in a {@link ReceivePort#receive(long)} invocation.
 * This exception is thrown when, during an invocation of one of the
 * receive() variants with a timeout, the timeout expires.
 */
public class ReceiveTimedOutException extends IbisIOException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a <code>ReceiveTimedOutException</code> with
     * <code>null</code> as its error detail message.
     */
    public ReceiveTimedOutException() {
        super();
    }

    /**
     * Constructs a <code>ReceiveTimedOutException</code> with
     * the specified detail message.
     *
     * @param detailMessage
     *          the detail message
     */
    public ReceiveTimedOutException(String detailMessage) {
        super(detailMessage);
    }

    /**
     * Constructs a <code>ReceiveTimedOutException</code> with
     * the specified detail message and cause.
     *
     * @param detailMessage
     *          the detail message
     * @param cause
     *          the cause
     */
    public ReceiveTimedOutException(String detailMessage, Throwable cause) {
        super(detailMessage);
        initCause(cause);
    }

    /**
     * Constructs a <code>ReceiveTimedOutException</code> with
     * the specified cause.
     *
     * @param cause
     *          the cause
     */
    public ReceiveTimedOutException(Throwable cause) {
        super();
        initCause(cause);
    }
}
