package ibis.gmi;

/**
 * The {@link BinomialCombiner} class serves as a base class for user-defined
 * binomial (reply) combiners. This class contains methods that can be used
 * to do a pairwise combine of the results. There is a combine method for
 * each result type. The user-defined combiner-class should redefine the
 * combine methods that it will use.
 *
 * This class is not abstract, because the user-defined combiner does not have
 * to supply all "combine" methods (for all different result types). Therefore,
 * default ones are supplied that just throw an exception.
 */
public class BinomialCombiner implements java.io.Serializable {

    /**
     * Combine method without a result. Note that if any exception is to
     * be propagated, the combine should throw an exception.
     *
     * @param rank1 rank within the group of the node giving the first result
     * @param ex1 possible exception thrown by node
     * @param rank2 rank within the group of the node giving the second result
     * @param ex2 possible exception thrown by this node
     * @param size total number of results to be combined (the size of the group)
     */
    public void combine(int rank1, Exception ex1, int rank2, Exception ex2,
            int size) {
        throw new RuntimeException(
                "void BinomialCombiner.combine(int, Exception, int, Exception, int) not implemented");
    }

    /**
     * Combiner with boolean results.
     *
     * See {@link #combine(int,Exception,int,Exception,int)}
     * for a description of some of the parameters.
     * The other parameters are:
     *
     * @param result1 the first result
     * @param result2 the second result
     */
    public boolean combine(int rank1, boolean result1, Exception e1, int rank2,
            boolean result2, Exception e2, int size) {
        throw new RuntimeException(
                "boolean BinomialCombiner.combine(int, boolean, Exception, int, boolean, Exception, int) not implemented");
    }

    /**
     * Combiner with byte results.
     *
     * See {@link #combine(int,boolean,Exception,int,boolean,Exception,int)}
     * for a description of the parameters.
     */
    public byte combine(int rank1, byte result1, Exception e1, int rank2,
            byte result2, Exception e2, int size) {
        throw new RuntimeException(
                "byte BinomialCombiner.combine(int, byte, Exception, int, byte, Exception, int) not implemented");
    }

    /**
     * Combiner with char results.
     *
     * See {@link #combine(int,boolean,Exception,int,boolean,Exception,int)}
     * for a description of the parameters.
     */
    public char combine(int rank1, char result1, Exception e1, int rank2,
            char result2, Exception e2, int size) {
        throw new RuntimeException(
                "char BinomialCombiner.combine(int, char, Exception, int, char, Exception, int) not implemented");
    }

    /**
     * Combiner with short results.
     *
     * See {@link #combine(int,boolean,Exception,int,boolean,Exception,int)}
     * for a description of the parameters.
     */
    public short combine(int rank1, short result1, Exception e1, int rank2,
            short result2, Exception e2, int size) {
        throw new RuntimeException(
                "short BinomialCombiner.combine(int, short, Exception, int, short, Exception, int) not implemented");
    }

    /**
     * Combiner with int results.
     *
     * See {@link #combine(int,boolean,Exception,int,boolean,Exception,int)}
     * for a description of the parameters.
     */
    public int combine(int rank1, int result1, Exception e1, int rank2,
            int result2, Exception e2, int size) {
        throw new RuntimeException(
                "int BinomialCombiner.combine(int, int, Exception, int, int, Exception, int) not implemented");
    }

    /**
     * Combiner with long results.
     *
     * See {@link #combine(int,boolean,Exception,int,boolean,Exception,int)}
     * for a description of the parameters.
     */
    public long combine(int rank1, long result1, Exception e1, int rank2,
            long result2, Exception e2, int size) {
        throw new RuntimeException(
                "long BinomialCombiner.combine(int, long, Exception, int, long, Exception, int) not implemented");
    }

    /**
     * Combiner with float results.
     *
     * See {@link #combine(int,boolean,Exception,int,boolean,Exception,int)}
     * for a description of the parameters.
     */
    public float combine(int rank1, float result1, Exception e1, int rank2,
            float result2, Exception e2, int size) {
        throw new RuntimeException(
                "float BinomialCombiner.combine(int, float, Exception, int, float, Exception, int) not implemented");
    }

    /**
     * Combiner with double results.
     *
     * See {@link #combine(int,boolean,Exception,int,boolean,Exception,int)}
     * for a description of the parameters.
     */
    public double combine(int rank1, double result1, Exception e1, int rank2,
            double result2, Exception e2, int size) {
        throw new RuntimeException(
                "double BinomialCombiner.combine(int, double, Exception, int, double, Exception, int) not implemented");
    }

    /**
     * Combiner with Object results.
     *
     * See {@link #combine(int,boolean,Exception,int,boolean,Exception,int)}
     * for a description of the parameters.
     */
    public Object combine(int rank1, Object result1, Exception e1, int rank2,
            Object result2, Exception e2, int size) {
        throw new RuntimeException(
                "Object BinomialCombiner.combine(int, Object, Exception, int, Object, Exception, int) not implemented");
    }
}