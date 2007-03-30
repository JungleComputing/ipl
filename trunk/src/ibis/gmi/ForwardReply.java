/* $Id$ */

package ibis.gmi;

/**
 * The {@link ForwardReply} class must be used when configuring a group method
 * to forward all replies to a {@link Forwarder} object.
 */
public class ForwardReply extends ReplyScheme {

    /**
     * The {@link Forwarder} of this reply scheme.
     */
    public Forwarder f;

    /**
     * Constructor.
     *
     * @param f the {@link Forwarder} of this replyscheme
     *
     * @exception ConfigurationException is thrown when the parameter is null.
     */
    public ForwardReply(Forwarder f) throws ConfigurationException {

        super(ReplyScheme.R_FORWARD);

        this.f = f;
        if (f == null) {
            throw new ConfigurationException("Invalid return forwarder " + f);
        }
    }
}
