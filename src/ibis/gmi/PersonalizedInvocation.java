package ibis.group;

public class PersonalizedInvocation extends InvocationScheme { 
    public Personalizer p;

    public PersonalizedInvocation(Personalizer p) throws ConfigurationException { 		
	super(InvocationScheme.I_PERSONAL);

	this.p = p;
	if (p == null) { 
	    throw new ConfigurationException("Invalid method personalizer");
	}
    } 
}
