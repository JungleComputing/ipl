package ibis.gmi;

/**
 * The {@link FlatInvocationCombiner} class provides a base class for user-defined
 * flat invocation combiners.
 */
public abstract class FlatInvocationCombiner {

    /**
     * The parameter combiner. The {@link ParameterVector}s in the "in" array are
     * combined into one {@link ParameterVector} "out".
     *
     * @param in the parameters to be combined
     * @param out will contain the result of the combination
     */
    public abstract void combine(ParameterVector[] in, ParameterVector out);
}