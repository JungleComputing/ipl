package ibis.impl.net.multi;

import ibis.impl.net.NetIbisIdentifier;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public interface MultiPlugin {

    public String getSubContext(boolean isOutgoing, NetIbisIdentifier localId,
            NetIbisIdentifier remoteId, ObjectOutputStream oos,
            ObjectInputStream ois) throws IOException;
}

