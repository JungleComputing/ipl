/* $Id$ */

package ibis.gmi;

/**
 * Class {@link ReplyScheme} is the base class for all reply schemes.
 */
public class ReplyScheme {

    /** For reply discarding, see {@link DiscardReply}. */
    public static final int R_DISCARD = 0;

    /** For returning a reply, see {@link ReturnReply}. */
    public static final int R_RETURN = 1;

    /** For forwarding replies, see {@link ForwardReply}. */
    public static final int R_FORWARD = 2;

    /** For flat-combining replies, see {@link CombineReply}. */
    public static final int R_COMBINE_FLAT = 3;

    /** For binomial-combining replies, see {@link CombineReply}. */
    public static final int R_COMBINE_BINOMIAL = 4;

    /** Mode for personalized replies, see {@link PersonalizeReply}. */
    public static final int R_PERSONALIZED = 16;

    /**
     * Value used in a group method descriptor (see {@link GroupMethod}), which
     * summarizes its reply scheme.
     */
    public static final int R_PERSONALIZED_RETURN = R_PERSONALIZED + R_RETURN;

    /**
     * Value used in a group method descriptor (see {@link GroupMethod}), which
     * summarizes its reply scheme.
     */
    public static final int R_PERSONALIZED_FORWARD = R_PERSONALIZED + R_FORWARD;

    /**
     * Value used in a group method descriptor (see {@link GroupMethod}), which
     * summarizes its reply scheme.
     */
    public static final int R_PERSONALIZED_COMBINE_FLAT = R_PERSONALIZED
            + R_COMBINE_FLAT;

    /**
     * Value used in a group method descriptor (see {@link GroupMethod}), which
     * summarizes its reply scheme.
     */
    public static final int R_PERSONALIZED_COMBINE_BINOMIAL = R_PERSONALIZED
            + R_COMBINE_BINOMIAL;

    /** Indicates one of the basic reply schemes. */
    int mode;

    /**
     * Constructor.
     *
     * @param mode the basic reply scheme
     */
    ReplyScheme(int mode) {
        this.mode = mode;
    }
}
