package ibis.ipl;

import java.io.IOException;

/**
 * There are three base classes for Ibis exceptions: this one (which extends
 * java.io.IOException), IbisException (which is a checked exception), and IbisRuntimException
 * (which is an unchecked exception).
 */
public class IbisIOException extends IOException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs an <code>IbisIOException</code> with
     * <code>null</code> as its error detail message.
     */
    public IbisIOException() {
	super();
    }

    /**
     * Constructs an <code>IbisIOException</code> with
     * the specified detail message.
     *
     * @param message
     *          the detail message
     */
    public IbisIOException(String message) {
	super(message);
    }

    /**
     * Constructs an <code>IbisIOException</code> with
     * the specified cause.
     *
     * @param cause
     *          the cause
     */
    public IbisIOException(Throwable cause) {
	super(cause);
    }

    /**
     * Constructs an <code>IbisIOException</code> with
     * the specified detail message and cause.
     *
     * @param message
     *          the detail message
     * @param cause
     *          the cause
     */
    public IbisIOException(String message, Throwable cause) {
	super(message, cause);
    }
}
