/* $Id$ */

package ibis.satin;

/**
 * Optional class that application Exceptions (inlets) can extend. The advantage
 * of this is that the generation of stack traces (an expensive operation) is
 * inhibited.
 */
public class Inlet extends Throwable {

    /** 
     * Generated
     */
    private static final long serialVersionUID = 5167746747259483619L;

    /**
     * Constructs an <code>Inlet</code>.
     */
    public Inlet() {
        /* do nothing */
    }

    /**
     * Overrides the <code>fillInStackTrace</code> from <code>Throwable</code>.
     * This version does not actually create a stack trace, which are useless
     * for inlets which in Satin are usually used for returning results.
     * @return this inlet.
     */
    public Throwable fillInStackTrace() {
        return this;
    }
}
