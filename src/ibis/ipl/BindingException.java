package ibis.ipl;

/**
 * Signals that a request for binding/unbinding of a receiveport failed.
 */
public class BindingException extends IbisIOException {

    /**
     * Constructs a <code>BindingException</code> with
     * <code>null</code> as its error detail message.
     */
    public BindingException() {
        super();
    }

    /**
     * Constructs a <code>BindingException</code> with
     * the specified detail message.
     *
     * @param s		the detail message
     */
    public BindingException(String s) {
        super(s);
    }

    /**
     * Constructs a <code>BindingException</code> with
     * the specified detail message and cause.
     *
     * @param s		the detail message
     * @param cause	the cause
     */
    public BindingException(String s, Throwable cause) {
        super(s, cause);
    }

    /**
     * Constructs a <code>BindingException</code> with
     * the specified cause.
     *
     * @param cause	the cause
     */
    public BindingException(Throwable cause) {
        super(cause);
    }
}
