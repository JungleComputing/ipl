package ibis.gmi;

/** The {@link GroupInvocation} class is to be used when configuring a group method
 * to be invoked on all members of the group.
 */
public class GroupInvocation extends InvocationScheme {

    /**
     * Constructor.
     */
    public GroupInvocation() {
        super(InvocationScheme.I_GROUP);
    }
}