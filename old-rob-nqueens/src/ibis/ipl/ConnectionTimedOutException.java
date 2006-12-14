/* $Id$ */

package ibis.ipl;

/**
 * Signals that an attempt to set up a connection timed out. A
 * <code>ConnectionTimedOutException</code> is thrown to indicate
 * that a sendport connect timed out.
 */
public class ConnectionTimedOutException extends IbisIOException {

    /**
     * Constructs a <code>ConnectionTimedOutException</code> with
     * <code>null</code> as its error detail message.
     */
    public ConnectionTimedOutException() {
        super();
    }

    /**
     * Constructs a <code>ConnectionTimedOutException</code> with
     * the specified detail message.
     *
     * @param s		the detail message
     */
    public ConnectionTimedOutException(String s) {
        super(s);
    }

    /**
     * Constructs a <code>ConnectionTimedOutException</code> with
     * the specified detail message and cause.
     *
     * @param s		the detail message
     * @param cause	the cause
     */
    public ConnectionTimedOutException(String s, Throwable cause) {
        super(s, cause);
    }

    /**
     * Constructs a <code>ConnectionTimedOutException</code> with
     * the specified cause.
     *
     * @param cause	the cause
     */
    public ConnectionTimedOutException(Throwable cause) {
        super(cause);
    }
}
