package ibis.ipl.server;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.support.management.AttributeDescription;

/**
 * Interface to the management service. Mostly for getting management info from the
 * server.
 * 
 * @ibis.experimental
 */
public interface ManagementServiceInterface {

    public abstract Object[] getAttributes(IbisIdentifier ibis,
            AttributeDescription... descriptions) throws Exception;

}