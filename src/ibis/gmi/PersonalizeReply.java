package ibis.group;

public class PersonalizeReply extends ReplyScheme { 

    ReplyPersonalizer rp;
    ReplyScheme rs;

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
	mode += rs.mode;
    } 
}
