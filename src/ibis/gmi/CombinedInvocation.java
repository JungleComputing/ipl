package ibis.group;

public class CombinedInvocation extends InvocationScheme { 
    
    String id;
    FlatInvocationCombiner flatCombiner;
    BinaryInvocationCombiner binCombiner;
    InvocationScheme inv;
    int rank, size;
    int counter = 0;

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
	} else {
	    //mode += inv.mode;
	}

	System.out.println("CombinedInvocation(" + identifier + ", " + size + ", FLAT): mode = " + mode);
    } 

    public CombinedInvocation(String identifier, 
			      int rank,
			      int size,
			      BinaryInvocationCombiner combiner, 
			      InvocationScheme inv) throws ConfigurationException { 		

	super(InvocationScheme.I_COMBINED_BINARY);

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
	} else {
	    //mode += inv.mode;
	}
    } 
}
