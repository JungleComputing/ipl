/* $Id$ */

package ibis.gmi;

/**
 * The {@link ReturnReply} class must be used to configure a group method that
 * returns a single reply, from the indicated group member.
 * In a group invocation, only the reply from the indicated group member is
 * used, the other replies are ignored.
 */
public class ReturnReply extends ReplyScheme {

    /** The rank of the group member that is supposed to deliver the reply. */
    int rank;

    /**
     * Constructor.
     *
     * @param rank the group member that is to deliver the reply
     *
     * @exception ConfigurationException is thrown when an invalid
     * parameter is given.
     */
    public ReturnReply(int rank) throws ConfigurationException {
        super(ReplyScheme.R_RETURN);
        this.rank = rank;
        if (rank < 0) {
            throw new ConfigurationException("Invalid return rank " + rank);
        }
    }
}
