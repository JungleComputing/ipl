package ibis.group;

/**
 * The {@link CombinedInvocation} class must be used whenever a group method must be
 * configured using a combined invocation scheme. Currently, there are two such schemes:
 * a "flat" invocation scheme, which uses a {@link FlatInvocationCombiner} to combine
 * the parameters, and a "binomial" invocation scheme, which uses a
 * {@link BinomialInvocationCombiner} to combine the parameters. In the flat invocation
 * scheme, all invocations are sent to a single node, which combines the parameters and
 * then invokes the method using the underlying invocation scheme (which could be single,
 * group, or personalized). In the binomial invocation scheme, the parameters are combined
 * using a binomial tree.
 */
public class CombinedInvocation extends InvocationScheme { 
    
    /**
     * The name of this combined invocation scheme (used for identification purposes).
     */
    String id;

    /**
     * The flat combiner.
     */
    FlatInvocationCombiner flatCombiner;

    /**
     * The binomial combiner.
     */
    BinomialInvocationCombiner binCombiner;

    /**
     * The underlying invocation scheme, single, group, or personalized.
     */
    InvocationScheme inv;

    /**
     * The rank number within this combined invocation scheme. This has nothing to
     * do with rank numbers within a group! Method invokers don't have to be a member
     * of the group.
     */
    int rank;

    /**
     * The total number of invokers in this combined invocation.
     */
    int size;

    /**
     * Constructor. This one is for a flat invocation combining scheme.
     *
     * @param identifier identifies this combined invocation object
     * @param rank indicates the rank of the creator of this {@link CombinedInvocation}
     * object
     * @param size indicates the total number of invokers
     * @param combiner indicates the parameter combiner itself
     * @param inv indicates the underlying invocation scheme
     *
     * @exception {@link ConfigurationException} is thrown on an illegal combination of
     * arguments.
     */
    public CombinedInvocation(String identifier, 
			      int rank,
			      int size,
			      FlatInvocationCombiner combiner, 
			      InvocationScheme inv) throws ConfigurationException { 		

	super(InvocationScheme.I_COMBINED_FLAT);

	this.id = identifier;
	this.rank = rank;
	this.size = size;		
	this.flatCombiner = combiner;
	this.inv = inv;

	if (id == null) { 
	    throw new ConfigurationException("Invalid operation identifier " + id);
	} 
	if (size < 1) { 
	    throw new ConfigurationException("Invalid number of participants " + size + " (must be >= 1)");
	} 
	if (flatCombiner == null) { 
	    throw new ConfigurationException("Invalid method combiner");
	} 

	if (inv == null || (inv.mode > InvocationScheme.I_PERSONAL)) { 
	    throw new ConfigurationException("Invalid nested invocation " + inv);
	}

	System.out.println("CombinedInvocation(" + identifier + ", " + size + ", FLAT): mode = " + mode);
    } 

    /**
     * Constructor. This one is for a binomial invocation combining scheme.
     *
     * @param identifier identifies this combined invocation object
     * @param rank indicates the rank of the creator of this {@link CombinedInvocation}
     * object
     * @param size indicates the total number of invokers
     * @param combiner indicates the parameter combiner itself
     * @param inv indicates the underlying invocation scheme
     *
     * @exception {@link ConfigurationException} is thrown on an illegal combination of
     * arguments.
     */
    public CombinedInvocation(String identifier, 
			      int rank,
			      int size,
			      BinomialInvocationCombiner combiner, 
			      InvocationScheme inv) throws ConfigurationException { 		

	super(InvocationScheme.I_COMBINED_BINOMIAL);

	this.id = identifier;
	this.rank = rank;
	this.size = size;		
	this.binCombiner = combiner;
	this.inv = inv;

	if (id == null) { 
	    throw new ConfigurationException("Invalid operation identifier " + id);
	} 
	if (size < 1) { 
	    throw new ConfigurationException("Invalid number of participants " + size + " (must be >= 1)");
	} 
	if (binCombiner == null) { 
	    throw new ConfigurationException("Invalid method combiner");
	} 
	if (inv == null || (inv.mode > InvocationScheme.I_PERSONAL)) { 
	    throw new ConfigurationException("Invalid nested invocation " + inv);
	}
    } 
}
