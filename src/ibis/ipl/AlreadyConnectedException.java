package ibis.ipl;

/**
 * Signals that a connection has been refused, because it already exists.
 */
public class AlreadyConnectedException extends IbisIOException {

    /**
     * Constructs a <code>AlreadyConnectedException</code> with
     * <code>null</code> as its error detail message.
     */
    public AlreadyConnectedException() {
	super();
    }

    /**
     * Constructs a <code>AlreadyConnectedException</code> with
     * the specified detail message.
     *
     * @param s		the detail message
     */
    public AlreadyConnectedException(String s) {
	super(s);
    }

    /**
     * Constructs a <code>AlreadyConnectedException</code> with
     * the specified detail message and cause.
     *
     * @param s		the detail message
     * @param cause	the cause
     */
    public AlreadyConnectedException(String s, Throwable cause) {
	super(s, cause);
    }

    /**
     * Constructs a <code>AlreadyConnectedException</code> with
     * the specified cause.
     *
     * @param cause	the cause
     */
    public AlreadyConnectedException(Throwable cause) {
	super(cause);
    }
}
