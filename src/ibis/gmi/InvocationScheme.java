package ibis.gmi;

/**
 * The {@link InvocationScheme} class provides a base class for all invocation schemes.
 */
public class InvocationScheme {

    /** "Single" invocation mode, see {@link SingleInvocation}. */
    public final static int I_SINGLE = 0;

    /** "Group" invocation mode, see {@link GroupInvocation}. */
    public final static int I_GROUP = 1;

    /** "Personalized" invocation mode, see {@link PersonalizedInvocation}. */
    public final static int I_PERSONAL = 2;

    /** "Flat-combined" invocation mode, see {@link CombinedInvocation}. */
    public final static int I_COMBINED_FLAT = 16;

    /** "Binomial-combined" invocation mode, see {@link CombinedInvocation}. */
    public final static int I_COMBINED_BINOMIAL = 32;

    /** A mode equal or larger than this is a combined invocation. */
    public final static int I_COMBINED = I_COMBINED_FLAT;

    /**
     * Summary value of this invocation scheme, for in a {@link GroupMethod} descriptor.
     * This one is for flat-combining, followed by a single invocation.
     */
    public final static int I_COMBINED_FLAT_SINGLE = I_COMBINED_FLAT + I_SINGLE;

    /**
     * Summary value of this invocation scheme, for in a {@link GroupMethod} descriptor.
     * This one is for flat-combining, followed by a group invocation.
     */
    public final static int I_COMBINED_FLAT_GROUP = I_COMBINED_FLAT + I_GROUP;

    /**
     * Summary value of this invocation scheme, for in a {@link GroupMethod} descriptor.
     * This one is for flat-combining, followed by a personalized invocation.
     */
    public final static int I_COMBINED_FLAT_PERSONAL = I_COMBINED_FLAT
            + I_PERSONAL;

    /**
     * Summary value of this invocation scheme, for in a {@link GroupMethod} descriptor.
     * This one is for binomial-combining, followed by a single invocation.
     */
    public final static int I_COMBINED_BINOMIAL_SINGLE = I_COMBINED_BINOMIAL
            + I_SINGLE;

    /**
     * Summary value of this invocation scheme, for in a {@link GroupMethod} descriptor.
     * This one is for binomial-combining, followed by a group invocation.
     */
    public final static int I_COMBINED_BINOMIAL_GROUP = I_COMBINED_BINOMIAL
            + I_GROUP;

    /**
     * Summary value of this invocation scheme, for in a {@link GroupMethod} descriptor.
     * This one is for binomial-combining, followed by a personalized invocation.
     */
    public final static int I_COMBINED_BINOMIAL_PERSONAL = I_COMBINED_BINOMIAL
            + I_PERSONAL;

    /** Type of this invocation scheme. */
    int mode;

    /**
     * Constructor.
     *
     * @param mode the type of this invocation scheme
     */
    InvocationScheme(int mode) {
        this.mode = mode;
    }
}