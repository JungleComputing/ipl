/* $Id$ */

package ibis.ipl;

/**
 * Signals that no matching Ibis could be found.
 * <code>NoMatchingIbisException</code> is thrown to indicate
 * that no matching Ibis could be found in
 * {@link ibis.ipl.IbisFactory#createIbis(CapabilitySet, CapabilitySet,
 * java.util.Properties, RegistryEventHandler) Ibis.createIbis}.
 */
public class NoMatchingIbisException extends Exception {
    /** 
     * Generated
     */
    private static final long serialVersionUID = 5408221950692659540L;

    /**
     * Constructs a <code>NoMatchingIbisException</code> with
     * <code>null</code> as its error detail message.
     */
    public NoMatchingIbisException() {
        super();
    }

    /**
     * Constructs a <code>NoMatchingIbisException</code> with
     * the specified detail message.
     *
     * @param s		the detail message
     */
    public NoMatchingIbisException(String s) {
        super(s);
    }

    /**
     * Constructs a <code>NoMatchingIbisException</code> with
     * the specified detail message and cause.
     *
     * @param s		the detail message
     * @param cause	the cause
     */
    public NoMatchingIbisException(String s, Throwable cause) {
        super(s, cause);
    }

    /**
     * Constructs a <code>NoMatchingIbisException</code> with
     * the specified cause.
     *
     * @param cause	the cause
     */
    public NoMatchingIbisException(Throwable cause) {
        super(cause);
    }
}
