/* $Id$ */

package ibis.ipl;

/**
 * Signals an illegal property name. A
 * <code>NoSuchPropertyException</code> is thrown to indicate
 * that an illegal property name was used in one of the methods from
 * the {@link Manageable} interface.
 */
public class NoSuchPropertyException extends Exception {

    private static final long serialVersionUID = 0x1L;

    /**
     * Constructs a <code>NoSuchPropertyException</code> with
     * <code>null</code> as its error detail message.
     */
    public NoSuchPropertyException() {
        super();
    }

    /**
     * Constructs a <code>NoSuchPropertyException</code> with
     * the specified detail message.
     *
     * @param detailMessage
     *          the detail message
     */
    public NoSuchPropertyException(String detailMessage) {
        super(detailMessage);
    }

    /**
     * Constructs a <code>NoSuchPropertyException</code> with
     * the specified detail message and cause.
     *
     * @param detailMessage
     *          the detail message
     * @param cause
     *          the cause
     */
    public NoSuchPropertyException(String detailMessage, Throwable cause) {
        super(detailMessage);
        initCause(cause);
    }

    /**
     * Constructs a <code>NoSuchPropertyException</code> with
     * the specified cause.
     *
     * @param cause
     *          the cause
     */
    public NoSuchPropertyException(Throwable cause) {
        super();
        initCause(cause);
    }
}
