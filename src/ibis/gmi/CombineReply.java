package ibis.group;

public class CombineReply extends ReplyScheme { 

    public FlatCombiner flatCombiner;
    public BinaryCombiner binaryCombiner;

    public CombineReply(FlatCombiner flatCombiner) throws ConfigurationException { 
	super(ReplyScheme.R_COMBINE_FLAT);

	this.flatCombiner = flatCombiner;
	if (flatCombiner == null) { 
	    throw new ConfigurationException("Invalid result combiner " + flatCombiner);
	}
    } 

    public CombineReply(BinaryCombiner binaryCombiner) throws ConfigurationException { 
	super(ReplyScheme.R_COMBINE_BINARY);

	this.binaryCombiner = binaryCombiner;
	if (binaryCombiner == null) { 
	    throw new ConfigurationException("Invalid result combiner " + binaryCombiner);
	}
    } 
}
