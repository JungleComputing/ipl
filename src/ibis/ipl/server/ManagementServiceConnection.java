package ibis.ipl.server;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.support.Connection;
import ibis.ipl.support.management.AttributeDescription;
import ibis.smartsockets.virtual.VirtualSocketAddress;
import ibis.smartsockets.virtual.VirtualSocketFactory;

class ManagementServiceConnection implements ManagementServiceInterface {

    public static final int TIMEOUT = 10000;

    private VirtualSocketAddress address;
    private VirtualSocketFactory socketFactory;

    ManagementServiceConnection(VirtualSocketAddress address,
            VirtualSocketFactory socketFactory) {
        this.address = address;
        this.socketFactory = socketFactory;
    }

    @Override
    public Object[] getAttributes(IbisIdentifier ibis,
            AttributeDescription... descriptions) throws Exception {
        Connection connection = new Connection(address, TIMEOUT, true,
                socketFactory);
        try {

            connection.out().writeByte(ServerConnectionProtocol.MAGIC_BYTE);
            connection.out().writeByte(
                    ServerConnectionProtocol.OPCODE_MANAGEMENT_GET_ATTRIBUTES);
            connection.writeObject(ibis);
            connection.writeObject(descriptions);
            connection.getAndCheckReply();

            return (Object[]) connection.readObject();
        } finally {
            connection.close();
        }
    }

}
