package ibis.ipl;

/**
 * There are three base classes for Ibis exceptions: this one (which is a checked
 * exception), IbisRuntimeException (which is an unchecked exception), and IbisIOException.
 * The latter exists because we want it to be a subclass of java.io.IOException.
 */
public class IbisException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs an <code>IbisException</code> with
     * <code>null</code> as its error detail message.
     */
    public IbisException() {
	super();
    }

    /**
     * Constructs an <code>IbisException</code> with
     * the specified detail message.
     *
     * @param message
     *          the detail message
     */
    public IbisException(String message) {
	super(message);
    }

    /**
     * Constructs an <code>IbisException</code> with
     * the specified cause.
     *
     * @param cause
     *          the cause
     */
    public IbisException(Throwable cause) {
	super(cause);
    }

    /**
     * Constructs an <code>IbisException</code> with
     * the specified detail message and cause.
     *
     * @param message
     *          the detail message
     * @param cause
     *          the cause
     */
    public IbisException(String message, Throwable cause) {
	super(message, cause);
    }
}
