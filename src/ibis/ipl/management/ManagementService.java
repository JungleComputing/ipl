package ibis.ipl.management;

import ibis.io.Conversion;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.registry.Connection;
import ibis.ipl.registry.central.Protocol;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import ibis.util.TypedProperties;

public class ManagementService implements ibis.ipl.server.Service {

    private static final int CONNECT_TIMEOUT = 10000;
    private final VirtualSocketFactory factory;

    public ManagementService(TypedProperties properties,
            VirtualSocketFactory factory) {
        this.factory = factory;
    }

    public void end(long deadline) {
        // NOTHING
    }

    public String getServiceName() {
        return "management";
    }

    public Object[] getAttributes(IbisIdentifier ibis,
            AttributeDescription... descriptions) throws Exception {
        ibis.ipl.impl.IbisIdentifier identifier;
        try {
            identifier = (ibis.ipl.impl.IbisIdentifier) ibis;
        } catch (ClassCastException e) {
            throw new Exception(
                    "cannot cast given identifier to implementation identifier",
                    e);
        }

        Connection connection = new Connection(identifier, CONNECT_TIMEOUT,
                false, factory);
        connection.out().writeByte(Protocol.MAGIC_BYTE);
        connection.out().writeByte(Protocol.OPCODE_GET_MONITOR_INFO);

        connection.out().writeInt(descriptions.length);
        for (int i = 0; i < descriptions.length; i++) {
            connection.out().writeUTF(descriptions[i].getBeanName());
            connection.out().writeUTF(descriptions[i].getAttribute());
        }

        connection.getAndCheckReply();

        int length = connection.in().readInt();
        if (length < 0) {
            connection.close();
            throw new Exception("End of Stream on reading from connection");
        }

        byte[] resultBytes = new byte[length];

        connection.in().readFully(resultBytes);

        Object[] reply = (Object[]) Conversion.byte2object(resultBytes);

        connection.close();

        return reply;
    }

    public String toString() {
        return "Management service";
    }

}
