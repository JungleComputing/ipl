package ibis.gmi;

/**
 * The {@link PersonalizedInvocation} class must be used to configure a
 * group method for personalized invocations.
 */
public class PersonalizedInvocation extends InvocationScheme {

    /** The personalizer. */
    public Personalizer p;

    /**
     * Constructor.
     *
     * @param p the personalizer
     *
     * @exception ConfigurationException is thrown when the parameter is null.
     */
    public PersonalizedInvocation(Personalizer p) throws ConfigurationException {
        super(InvocationScheme.I_PERSONAL);

        this.p = p;
        if (p == null) {
            throw new ConfigurationException("Invalid method personalizer");
        }
    }
}