package ibis.group;

/**
 * The {@link CombineReply} class must be used to configure a group method
 * with a combined reply.
 * There are two combiner types: {@link FlatCombiner} which combines results
 * in gather style, and {@link BinomialCombiner}, which combines results in
 * reduce style.
 *
 * @see GroupMethod#configure
 */
public class CombineReply extends ReplyScheme { 

    /**
     * The gather style combiner.
     */
    public FlatCombiner flatCombiner;

    /**
     * The reduce style combiner.
     */
    public BinomialCombiner binomialCombiner;

    /**
     * Constructor with a {@link FlatCombiner}.
     *
     * @param flatCombiner the {@link FlatCombiner}
     * 
     * @exception {@link ConfigurationException} when the parameter is null.
     */
    public CombineReply(FlatCombiner flatCombiner) throws ConfigurationException { 
	super(ReplyScheme.R_COMBINE_FLAT);

	this.flatCombiner = flatCombiner;
	if (flatCombiner == null) { 
	    throw new ConfigurationException("Invalid result combiner " + flatCombiner);
	}
    } 

    /**
     * Constructor with a {@link BinomialCombiner}.
     *
     * @param binCombiner the {@link BinomialCombiner}
     *
     * @exception {@link ConfigurationException} when the parameter is null.
     */
    public CombineReply(BinomialCombiner binCombiner) throws ConfigurationException { 
	super(ReplyScheme.R_COMBINE_BINOMIAL);

	binomialCombiner = binCombiner;
	if (binomialCombiner == null) { 
	    throw new ConfigurationException("Invalid result combiner " + binomialCombiner);
	}
    } 
}
