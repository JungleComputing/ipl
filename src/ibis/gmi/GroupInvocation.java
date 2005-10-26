/* $Id$ */

package ibis.gmi;

/** The {@link GroupInvocation} class is to be used when configuring a group
 * method to be invoked on all members of the group.
 */
public class GroupInvocation extends InvocationScheme {

    protected int [] targets;
           
    /**
     * Constructor.
     */
    public GroupInvocation() {
        super(InvocationScheme.I_GROUP);
    }
    
    public GroupInvocation(int [] targets) throws ConfigurationException {
        super(InvocationScheme.I_GROUP);
        
        if (targets == null || targets.length < 1) {
            throw new ConfigurationException("Invalid group invocation target");
        }
        
        this.targets = (int []) targets.clone();        
    }
    
}
