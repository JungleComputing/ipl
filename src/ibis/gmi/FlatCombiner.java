/* $Id$ */

package ibis.gmi;

/**
 * The {@link FlatCombiner} class serves as a base class for user-defined
 * result flat-combiners. The user-defined flatcombiner is supposed to redefine
 * the "combine" versions that the user is going to invoke.
 *
 * This class is not abstract, because the user-defined combiner does not have
 * to supply all "combine" methods (for all different result types). Therefore,
 * default ones are supplied that just throw an exception.
 */
public class FlatCombiner {

    /** Disable construction from outside. */
    protected FlatCombiner() {
    }

    /**
     * Combine with a void result. May throw an exception that is the result
     * of an exception combine.
     *
     * @param exceptions the exceptions to be combined
     */
    public void combine(Exception[] exceptions) {
        throw new RuntimeException(
                "void FlatCombiner.combine(Exception []) not implemented");
    }

    /**
     * Combine with a boolean result. May throw an exception that is the result
     * of an exception combine.
     *
     * @param results the results to be combined
     * @param exceptions the exceptions to be combined
     * @return result of the combine operation
     */
    public boolean combine(boolean[] results, Exception[] exceptions) {
        throw new RuntimeException(
                "boolean FlatCombiner.combine(boolean [], Exceptions []) "
                + "not implemented");
    }

    /**
     * Combine with a byte result. May throw an exception that is the result
     * of an exception combine.
     *
     * @param results the results to be combined
     * @param exceptions the exceptions to be combined
     * @return result of the combine operation
     */
    public byte combine(byte[] results, Exception[] exceptions) {
        throw new RuntimeException(
                "byte FlatCombiner.combine(byte [], Exceptions []) "
                + "not implemented");
    }

    /**
     * Combine with a char result. May throw an exception that is the result
     * of an exception combine.
     *
     * @param results the results to be combined
     * @param exceptions the exceptions to be combined
     * @return result of the combine operation
     */
    public char combine(char[] results, Exception[] exceptions) {
        throw new RuntimeException(
                "char FlatCombiner.combine(char [], Exceptions []) "
                + "not implemented");
    }

    /**
     * Combine with a short result. May throw an exception that is the result
     * of an exception combine.
     *
     * @param results the results to be combined
     * @param exceptions the exceptions to be combined
     * @return result of the combine operation
     */
    public short combine(short[] results, Exception[] exceptions) {
        throw new RuntimeException(
                "short FlatCombiner.combine(short [], Exceptions []) "
                + "not implemented");
    }

    /**
     * Combine with an int result. May throw an exception that is the result
     * of an exception combine.
     *
     * @param results the results to be combined
     * @param exceptions the exceptions to be combined
     * @return result of the combine operation
     */
    public int combine(int[] results, Exception[] exceptions) {
        throw new RuntimeException(
                "int FlatCombiner.combine(int [], Exceptions []) "
                + "not implemented");
    }

    /**
     * Combine with a long result. May throw an exception that is the result
     * of an exception combine.
     *
     * @param results the results to be combined
     * @param exceptions the exceptions to be combined
     * @return result of the combine operation
     */
    public long combine(long[] results, Exception[] exceptions) {
        throw new RuntimeException(
                "long FlatCombiner.combine(long [], Exceptions []) "
                + "not implemented");
    }

    /**
     * Combine with a float result. May throw an exception that is the result
     * of an exception combine.
     *
     * @param results the results to be combined
     * @param exceptions the exceptions to be combined
     * @return result of the combine operation
     */
    public float combine(float[] results, Exception[] exceptions) {
        throw new RuntimeException(
                "float FlatCombiner.combine(float [], Exceptions []) "
                + "not implemented");
    }

    /**
     * Combine with a double result. May throw an exception that is the result
     * of an exception combine.
     *
     * @param results the results to be combined
     * @param exceptions the exceptions to be combined
     * @return result of the combine operation
     */
    public double combine(double[] results, Exception[] exceptions) {
        throw new RuntimeException(
                "double FlatCombiner.combine(double [], Exceptions []) "
                + "not implemented");
    }

    /**
     * Combine with an Object result. May throw an exception that is the result
     * of an exception combine.
     *
     * @param results the results to be combined
     * @param exceptions the exceptions to be combined
     * @return result of the combine operation
     */
    public Object combine(Object[] results, Exception[] exceptions) {
        throw new RuntimeException(
                "Object FlatCombiner.combine(Object [], Exceptions []) "
                + "not implemented");
    }
}
