package ibis.group;

/**
 * The {@link Personalizer} class provides a base class for parameter
 * personalizers. A parameter personalizer is used to split parameter set of a
 * method invocation into several, personalized, sets of parameters.
 */
public abstract class Personalizer { 

    /**
     * Actually does the splitting.
     *
     * @param in the input parameter set
     * @param out the resulting personalized parameters
     */
    public abstract void personalize(ParameterVector in, ParameterVector [] out);
} 
