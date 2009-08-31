/* $Id$ */

package ibis.ipl;

/**
 * Signals that an Ibis instance could not be created.
 */
public class IbisCreationFailedException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a <code>IbisCreationFailedException</code> with
     * <code>null</code> as its error detail message.
     */
    public IbisCreationFailedException() {
        super();
    }

    /**
     * Constructs a <code>IbisCreationFailedException</code> with the
     * specified detail message.
     * 
     * @param detailMessage
     *            the detail message
     */
    public IbisCreationFailedException(String detailMessage) {
        super(detailMessage);
    }

    /**
     * Constructs a <code>IbisCreationFailedException</code> with the
     * specified detail message and cause.
     * 
     * @param detailMessage
     *            the detail message
     * @param cause
     *            the cause
     */
    public IbisCreationFailedException(String detailMessage, Throwable cause) {
        super(detailMessage, cause);
    }

    /**
     * Constructs a <code>IbisCreationFailedException</code> with the
     * specified cause.
     * 
     * @param cause
     *            the cause
     */
    public IbisCreationFailedException(Throwable cause) {
        super(cause);
    }
}
