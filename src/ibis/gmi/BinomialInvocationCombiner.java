package ibis.gmi;

/**
 * The {@link BinomialInvocationCombiner} class serves as a base class
 * for user-defined binomial invocation combiners.
 */
public abstract class BinomialInvocationCombiner {
    /**
     * The combine method, to be defined by any class that extends
     * the {@link BinomialInvocationCombiner} class.
     *
     * @param in1 the first input {@link ParameterVector}
     * @param in2 the second input {@link ParameterVector}
     * @param out the result {@link ParameterVector} of combining the "in" parameters
     */
    public abstract void combine(ParameterVector in1, ParameterVector in2,
            ParameterVector out);
}