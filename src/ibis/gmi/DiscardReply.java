package ibis.group;

/**
 * The {@link DiscardReply} class must be used when configuring a group method
 * to discard its result.
 */
public class DiscardReply extends ReplyScheme { 

    /**
     * Constructor.
     */
    public DiscardReply() { 
	super(ReplyScheme.R_DISCARD);
    } 
}
