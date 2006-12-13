/* $Id$ */

package ibis.ipl;

/**
 * Signals that there was an error in the Ibis configuration.
 * <code>IbisConfigurationException</code> is thrown to indicate
 * that there is something wrong in the way Ibis was configured.
 */
public class IbisConfigurationException extends RuntimeException {
    /**
     * Constructs a <code>IbisConfigurationException</code> with
     * <code>null</code> as its error detail message.
     */
    public IbisConfigurationException() {
        super();
    }

    /**
     * Constructs a <code>IbisConfigurationException</code> with
     * the specified detail message.
     *
     * @param s		the detail message
     */
    public IbisConfigurationException(String s) {
        super(s);
    }

    /**
     * Constructs a <code>IbisConfigurationException</code> with
     * the specified detail message and cause.
     *
     * @param s		the detail message
     * @param cause	the cause
     */
    public IbisConfigurationException(String s, Throwable cause) {
        super(s, cause);
    }

    /**
     * Constructs a <code>IbisConfigurationException</code> with
     * the specified cause.
     *
     * @param cause	the cause
     */
    public IbisConfigurationException(Throwable cause) {
        super(cause);
    }
}
