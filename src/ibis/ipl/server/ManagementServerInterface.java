package ibis.ipl.server;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.management.AttributeDescription;

public interface ManagementServerInterface {

    public abstract Object[] getAttributes(IbisIdentifier ibis,
            AttributeDescription... descriptions) throws Exception;

}