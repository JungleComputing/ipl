package ibis.gmi;

/**
 * The {@link SingleInvocation} class must be used to configure a group
 * method to a single invocation. This means that the invocation is forwarded
 * to a single object of the group.
 */
public class SingleInvocation extends InvocationScheme {
    /**
     * Identifies the rank within the group of the object on which the method
     * is to be invoked.
     */
    int destination;

    /**
     * Constructor.
     *
     * @param destination the rank within the group of the object object
     * on which the method is to be invoked
     *
     * @exception ConfigurationException is thrown when an illegal
     * parameter is supplied.
     */
    public SingleInvocation(int destination) throws ConfigurationException {
        super(InvocationScheme.I_SINGLE);

        this.destination = destination;
        if (destination < 0) {
            throw new ConfigurationException(
                    "Invocation destination must be >= 0 (" + destination + ")");
        }
    }
}