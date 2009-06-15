package ibis.ipl.server;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.management.AttributeDescription;

public interface ManagementServiceInterface {

    public abstract Object[] getAttributes(IbisIdentifier ibis,
            AttributeDescription... descriptions) throws Exception;

}