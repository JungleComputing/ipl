package ibis.group;

public class SingleInvocation extends InvocationScheme { 
    int destination;

    public SingleInvocation(int destination) throws ConfigurationException { 
	super(InvocationScheme.I_SINGLE); 

	this.destination = destination;
	if (destination < 0) { 
	    throw new ConfigurationException("Invocation destination must be >= 0 (" + destination);
	}
    } 
}
