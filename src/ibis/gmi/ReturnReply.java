package ibis.group;

public class ReturnReply extends ReplyScheme { 

    int rank;

    public ReturnReply(int rank) throws ConfigurationException { 
	super(ReplyScheme.R_RETURN);
	this.rank = rank;
	if (rank < 0) { 
	    throw new ConfigurationException("Invalid return rank " + rank);
	}
    } 
}
