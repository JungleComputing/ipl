package ibis.gmi;

/**
 * The {@link PersonalizeReply} class must be used when configuring a group method
 * to have a personalized reply scheme. Each group method invoker gets a reply,
 * which is personalized by the {@link ReplyPersonalizer} object in this reply scheme
 * object. This is really only useful when there is more than one invoker, so with
 * a combined invocation scheme.
 */
public class PersonalizeReply extends ReplyScheme { 

    /**
     * The reply personalizer.
     */
    public ReplyPersonalizer rp;

    /**
     * The underlying reply scheme. In fact, this is not "underlying", but "on top",
     * because this is the reply scheme that is used to create the reply that is to
     * be personalized.
     */
    public ReplyScheme rs;

    /**
     * Constructor.
     *
     * @param rp the reply personalizer
     * @param rs the underlying reply scheme
     *
     * @exception {@link ConfigurationException} is thrown when one of the paramers is
     * null, or the underlying reply scheme is discarding or already personalizing.
     */
    public PersonalizeReply(ReplyPersonalizer rp, ReplyScheme rs) throws ConfigurationException { 
	super(ReplyScheme.R_PERSONALIZED);
	this.rp = rp;
	this.rs = rs;
	if (rp == null) { 
	    throw new ConfigurationException("Invalid result personalizer " + rp);
	}
	if (rs == null ||
	    (rs.mode == ReplyScheme.R_DISCARD) ||
	    (rs.mode >= ReplyScheme.R_PERSONALIZED)) { 
	    throw new ConfigurationException("Invalid nested reply scheme " + rs);
	}
    } 
}
