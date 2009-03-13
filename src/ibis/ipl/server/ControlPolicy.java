package ibis.ipl.server;

import java.security.AccessControlException;

/**
 * @experimental
 * @author rkemp
 */
public interface ControlPolicy {

    /**
     * @experimental
     * @param authenticationObject
     * @throws AccessControlException
     */
    public void onJoin(Object authenticationObject)
            throws AccessControlException;

}
