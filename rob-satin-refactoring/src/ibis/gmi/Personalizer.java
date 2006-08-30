/* $Id$ */

package ibis.gmi;

/**
 * The {@link Personalizer} class provides a base class for parameter
 * personalizers. A parameter personalizer is used to split the parameter set
 * of a method invocation into several, personalized, sets of parameters.
 */
public abstract class Personalizer {

    /** Prevent construction from outside. */
    protected Personalizer() {
    	// nothing here
    }

    /**
     * Splits the specified list of parameters, and writes them into
     * the specified array of parameter lists.
     *
     * @param in the input parameter list
     * @param out the resulting personalized parameter lists
     */
    public abstract void personalize(ParameterVector in, ParameterVector[] out);
}
