package ibis.ipl.server;

import ibis.ipl.IbisConfigurationException;
import ibis.ipl.support.Connection;
import ibis.smartsockets.SmartSocketsProperties;
import ibis.smartsockets.direct.DirectSocketAddress;
import ibis.smartsockets.hub.servicelink.ServiceLink;
import ibis.smartsockets.virtual.InitializationException;
import ibis.smartsockets.virtual.VirtualSocketAddress;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import ibis.util.ThreadPool;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Properties;

/**
 * Connection to a server in another JVM (possibly on another machine).
 * Connection can be established either by connecting stdin/stdout to a server
 * started with the "--remote" option, or by passing the address of the server.
 * 
 * @author Niels Drost
 * 
 */
public class ServerConnection implements ServerInterface {

    public static final int TIMEOUT = 10000;

    private final ServerPipe pipe;

    private final VirtualSocketAddress address;

    private final ManagementServiceConnection managementConnection;

    private final RegistryServiceConnection registryConnection;

    private final VirtualSocketFactory socketFactory;

    private static VirtualSocketAddress createServerAddress(
            String serverString, int defaultPort) throws IOException {

        if (serverString == null) {
            throw new IbisConfigurationException("serverString undefined");
        }

        DirectSocketAddress address = null;

        // maybe it is a DirectSocketAddress?
        try {
            address = DirectSocketAddress.getByAddress(serverString);
        } catch (Throwable e) {
            // IGNORE
        }

        if (address == null) {

            // or only a host address
            try {
                address = DirectSocketAddress.getByAddress(serverString,
                        defaultPort);
            } catch (Throwable e) {
                throw new IOException(
                        "could not create server address from given string: "
                                + serverString);
            }
        }

        return new VirtualSocketAddress(address,
                ServerConnectionProtocol.VIRTUAL_PORT, address, null);

    }

    private static VirtualSocketFactory createSocketFactory(String hubs)
            throws IOException {
        final VirtualSocketFactory result;

        Properties properties = new Properties();

        if (hubs != null) {
            properties.setProperty(SmartSocketsProperties.HUB_ADDRESSES, hubs);
        }

        try {
            result = VirtualSocketFactory.createSocketFactory(properties, true);
        } catch (InitializationException e) {
            throw new IOException(e.getMessage());
        }

        // Make this node disappear from the smartsockets viz, but avoid a roundtrip for this in the
        // Ibis creation thread.
        Runnable r = new Runnable() {
            public void run() {
                try {
                    ServiceLink sl = result.getServiceLink();
                    if (sl != null) {
                        sl.registerProperty("smartsockets.viz", "invisible");
                    }
                } catch (Throwable e) {
                    // ignored
                }
            }
        };
        ThreadPool.createNew(r, "Service link eraser");

        return result;
    }

    /**
     * Creates a connection to the server with the given in and output stream.
     * Will parse the address of the server from the standard out of the server process.
     * Also forwards the output to the given stream (with an optional prefix to each
     * line). When this connection is terminated, the (remote) server terminates
     * as well.
     * 
     * @param stdout
     *            Standard out of server process
     * @param stdin
     *            Standard in of server process
     * @param output
     *            Stream to forward output to
     * @param outputPrefix
     *            Prefix to add to all lines of output
     * @param timeout
     *            Number of milliseconds to wait for the server to become
     *            available
     * @param socketFactory
     *            Socket factory to use for making connections. if null, a new
     *            factory will be created
     * 
     * @throws IOException
     *             if the socket factory cannot be created
     */
    public ServerConnection(InputStream stdout, OutputStream stdin,
            PrintStream output, String outputPrefix, long timeout,
            VirtualSocketFactory socketFactory) throws IOException {

        pipe = new ServerPipe(stdout, stdin, output, outputPrefix);
        address = createServerAddress(pipe.getAddress(timeout),
                ServerProperties.DEFAULT_PORT);

        if (socketFactory == null) {
            this.socketFactory = createSocketFactory(null);

        } else {
            this.socketFactory = socketFactory;
        }

        managementConnection = new ManagementServiceConnection(this.address,
                this.socketFactory);
        registryConnection = new RegistryServiceConnection(this.address,
                this.socketFactory);
    }

    /**
     * Creates a connection to the server at the given address.
     * 
     * @param address
     *            address of the server
     * @param socketFactory
     *            Socket factory to use for making connections. if null, a new
     *            factory will be created
     * @throws IOException
     *             in case the socket factory cannot be created
     */
    public ServerConnection(String address, VirtualSocketFactory socketFactory)
            throws IOException {
        pipe = null;
        this.address = createServerAddress(address,
                ServerProperties.DEFAULT_PORT);

        if (socketFactory == null) {
            this.socketFactory = createSocketFactory(null);
        } else {
            this.socketFactory = socketFactory;
        }

        managementConnection = new ManagementServiceConnection(this.address,
                this.socketFactory);
        registryConnection = new RegistryServiceConnection(this.address,
                this.socketFactory);
    }

    /**
     * Connections to the server at the given address. A list of hubs to use to
     * connect to the server must also be provided.
     * 
     * @param address
     *            address of the server
     * @param hubs
     *            list of hubs to use.
     * @throws IOException
     *             in case the SocketFactory could not be created
     * 
     */
    public ServerConnection(String address, String hubs) throws IOException {
        pipe = null;

        Properties properties = new Properties();
        properties.put(SmartSocketsProperties.HUB_ADDRESSES, hubs);

        this.socketFactory = createSocketFactory(hubs);

        this.address = createServerAddress(getAddress(),
                ServerProperties.DEFAULT_PORT);

        managementConnection = new ManagementServiceConnection(this.address,
                this.socketFactory);
        registryConnection = new RegistryServiceConnection(this.address,
                this.socketFactory);

    }

    // Java 1.5 Does not allow @Override for interface methods
    //    @Override
    public String getAddress() throws IOException {
        return address.machine().toString();
    }

    //    @Override
    public String[] getServiceNames() throws IOException {
        Connection connection = new Connection(address, TIMEOUT, true,
                socketFactory);
        try {
            connection.out().writeByte(ServerConnectionProtocol.MAGIC_BYTE);
            connection.out().writeByte(
                    ServerConnectionProtocol.OPCODE_GET_SERVICE_NAMES);
            connection.getAndCheckReply();
            int nrOfServices = connection.in().readInt();
            if (nrOfServices < 0) {
                throw new IOException("Negative number of services");
            }
            String[] result = new String[nrOfServices];
            for (int i = 0; i < nrOfServices; i++) {
                result[i] = connection.in().readUTF();
            }
            return result;
        } finally {
            connection.close();
        }

    }

    //    @Override
    public String[] getHubs() throws IOException {
        Connection connection = new Connection(address, TIMEOUT, true,
                socketFactory);
        try {
            connection.out().writeByte(ServerConnectionProtocol.MAGIC_BYTE);
            connection.out()
                    .writeByte(ServerConnectionProtocol.OPCODE_GET_HUBS);
            connection.getAndCheckReply();
            int nrOfHubs = connection.in().readInt();
            if (nrOfHubs < 0) {
                throw new IOException("Negative number of hubs");
            }
            String[] result = new String[nrOfHubs];
            for (int i = 0; i < nrOfHubs; i++) {
                result[i] = connection.in().readUTF();
            }
            return result;
        } finally {
            connection.close();
        }

    }

    //    @Override
    public void addHubs(DirectSocketAddress... hubAddresses) throws IOException {
        String[] strings = new String[hubAddresses.length];
        for (int i = 0; i < strings.length; i++) {
            strings[i] = hubAddresses[i].toString();
        }
        addHubs(strings);
    }

    //    @Override
    public void addHubs(String... hubAddresses) throws IOException {
        Connection connection = new Connection(address, TIMEOUT, true,
                socketFactory);
        try {
            connection.out().writeByte(ServerConnectionProtocol.MAGIC_BYTE);
            connection.out()
                    .writeByte(ServerConnectionProtocol.OPCODE_ADD_HUBS);
            connection.out().writeInt(hubAddresses.length);
            for (String hub : hubAddresses) {
                connection.out().writeUTF(hub);
            }
            connection.getAndCheckReply();
        } finally {
            connection.close();
        }
    }

    //    @Override
    public void end(long timeout) throws IOException {
        if (pipe != null) {
            pipe.end();
        } else {
            Connection connection = new Connection(address, TIMEOUT, true,
                    socketFactory);
            try {

                connection.out().writeByte(ServerConnectionProtocol.MAGIC_BYTE);
                connection.out().writeByte(ServerConnectionProtocol.OPCODE_END);
                connection.out().writeLong(timeout);
                connection.getAndCheckReply();
            } finally {
                connection.close();
            }
        }

    }

    //    @Override
    public RegistryServiceInterface getRegistryService() {
        return registryConnection;
    }

    //    @Override
    public ManagementServiceInterface getManagementService() {
        return managementConnection;
    }

    /**
     * Closes connection to the server. Will also terminate server if
     * stdin/stdout connection is used
     */
    public void closeConnection() {
        if (pipe != null) {
            pipe.end();
        }
    }

}
