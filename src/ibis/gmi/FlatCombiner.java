package ibis.group;

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

    /**
     * Combine with a void result. May throw an exception that is the result
     * of an exception combine.
     *
     * @param exceptions the exceptions to be combined
     */
    public void combine(Exception [] exceptions) { 
	throw new RuntimeException("void FlatCombiner.combine(Exception []) not implemented");
    } 

    /**
     * Combine with a boolean result. May throw an exception that is the result
     * of an exception combine.
     *
     * @param results the results to be combined
     * @param exceptions the exceptions to be combined
     */
    public boolean combine(boolean [] results, Exception [] exceptions) { 
	throw new RuntimeException("boolean FlatCombiner.combine(boolean [], Exceptions []) not implemented");
    } 

    /**
     * See {@link #combine(boolean[],Exception[])}, but for byte results.
     */
    public byte combine(byte [] results, Exception [] exceptions) {
	throw new RuntimeException("byte FlatCombiner.combine(byte [], Exceptions []) not implemented");
    } 

    /**
     * See {@link #combine(boolean[],Exception[])}, but for char results.
     */
    public char combine(char [] results, Exception [] exceptions) {
	throw new RuntimeException("char FlatCombiner.combine(char [], Exceptions []) not implemented");
    } 

    /**
     * See {@link #combine(boolean[],Exception[])}, but for short results.
     */
    public short combine(short [] results, Exception [] exceptions) { 
	throw new RuntimeException("short FlatCombiner.combine(short [], Exceptions []) not implemented");
    } 

    /**
     * See {@link #combine(boolean[],Exception[])}, but for int results.
     */
    public int combine(int [] results, Exception [] exceptions) { 
	throw new RuntimeException("int FlatCombiner.combine(int [], Exceptions []) not implemented");
    } 

    /**
     * See {@link #combine(boolean[],Exception[])}, but for long results.
     */
    public long combine(long [] results, Exception [] exceptions) { 
	throw new RuntimeException("long FlatCombiner.combine(long [], Exceptions []) not implemented");
    } 

    /**
     * See {@link #combine(boolean[],Exception[])}, but for float results.
     */
    public float combine(float [] results, Exception [] exceptions) { 
	throw new RuntimeException("float FlatCombiner.combine(float [], Exceptions []) not implemented");
    } 

    /**
     * See {@link #combine(boolean[],Exception[])}, but for double results.
     */
    public double combine(double [] results, Exception [] exceptions) { 
	throw new RuntimeException("double FlatCombiner.combine(double [], Exceptions []) not implemented");
    } 

    /**
     * See {@link #combine(boolean[],Exception[])}, but for Object results.
     */
    public Object combine(Object [] results, Exception [] exceptions) { 
	throw new RuntimeException("Object FlatCombiner.combine(Object [], Exceptions []) not implemented");
    } 
} 
