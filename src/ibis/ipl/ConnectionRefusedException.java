package ibis.ipl;

/**
 * Signals that a connection has been refused. A
 * <code>ConnectionRefusedException</code> is thrown to indicate
 * that a sendport connect was refused.
 */
public class ConnectionRefusedException extends IbisIOException {

    /**
     * Constructs a <code>ConnectionRefusedException</code> with
     * <code>null</code> as its error detail message.
     */
    public ConnectionRefusedException() {
        super();
    }

    /**
     * Constructs a <code>ConnectionRefusedException</code> with
     * the specified detail message.
     *
     * @param s		the detail message
     */
    public ConnectionRefusedException(String s) {
        super(s);
    }

    /**
     * Constructs a <code>ConnectionRefusedException</code> with
     * the specified detail message and cause.
     *
     * @param s		the detail message
     * @param cause	the cause
     */
    public ConnectionRefusedException(String s, Throwable cause) {
        super(s, cause);
    }

    /**
     * Constructs a <code>ConnectionRefusedException</code> with
     * the specified cause.
     *
     * @param cause	the cause
     */
    public ConnectionRefusedException(Throwable cause) {
        super(cause);
    }
}