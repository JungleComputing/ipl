/* $Id$ */

package ibis.ipl;

/**
 * Signals a timeout in a {@link ReceivePort#receive(long)} invocation.
 * This exception is thrown when, during an invocation of one of the
 * receive() variants with a timeout, the timeout expires.
 */
public class ReceiveTimedOutException extends IbisIOException {

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
     * @param s		the detail message
     */
    public ReceiveTimedOutException(String s) {
        super(s);
    }

    /**
     * Constructs a <code>ReceiveTimedOutException</code> with
     * the specified detail message and cause.
     *
     * @param s		the detail message
     * @param cause	the cause
     */
    public ReceiveTimedOutException(String s, Throwable cause) {
        super(s, cause);
    }

    /**
     * Constructs a <code>ReceiveTimedOutException</code> with
     * the specified cause.
     *
     * @param cause	the cause
     */
    public ReceiveTimedOutException(Throwable cause) {
        super(cause);
    }
}
