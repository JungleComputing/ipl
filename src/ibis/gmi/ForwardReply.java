package ibis.group;

public class ForwardReply extends ReplyScheme { 

    public Forwarder f;

    public ForwardReply(Forwarder f) throws ConfigurationException { 

	super(ReplyScheme.R_FORWARD);

	this.f = f;
	if (f == null) { 
	    throw new ConfigurationException("Invalid return forwarder " + f);
	}
    } 
}
