package ibis.impl.registry.central;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Properties;

import org.apache.log4j.Logger;

import smartsockets.direct.DirectSocketAddress;
import smartsockets.virtual.InitializationException;
import smartsockets.virtual.VirtualServerSocket;
import smartsockets.virtual.VirtualSocketAddress;
import smartsockets.virtual.VirtualSocketFactory;

import ibis.impl.IbisIdentifier;
import ibis.io.Conversion;
import ibis.util.IPUtils;

public class ConnectionFactory {
    public static final int CONNECTION_BACKLOG = 50;

    private static final Logger logger = Logger
            .getLogger(ConnectionFactory.class);

    private final boolean smart;

    // smart socket fields

    private final VirtualSocketFactory virtualSocketFactory;

    private final VirtualServerSocket virtualServerSocket;

    private final VirtualSocketAddress virtualServerAddress;

    private final ServerSocket plainServerSocket;

    private final InetAddress plainLocalAddress;

    private final InetSocketAddress plainServerAddress;

    private static InetSocketAddress plainAddressFromBytes(byte[] bytes)
            throws IOException {
        int port = Conversion.defaultConversion.byte2int(bytes, 0);
        byte[] addressBytes = new byte[bytes.length - 4];
        System.arraycopy(bytes, 4, addressBytes, 0, addressBytes.length);
        InetAddress address = InetAddress.getByAddress(addressBytes);

        return new InetSocketAddress(address, port);
    }

    private static byte[] plainAddressToBytes(InetAddress address, int port) {
        byte[] addressBytes = address.getAddress();
        byte[] result = new byte[addressBytes.length + 4];

        System.arraycopy(addressBytes, 0, result, 4, addressBytes.length);
        Conversion.defaultConversion.int2byte(port, result, 0);

        return result;
    }

    ConnectionFactory(boolean smart, String serverString,
            int defaultServerPort, Properties defaults) throws IOException {
        this.smart = smart;

        if (smart) {
            plainServerSocket = null;
            plainServerAddress = null;
            plainLocalAddress = null;

            try {
                virtualSocketFactory = VirtualSocketFactory
                        .getOrCreateSocketFactory("ibis", defaults, true);
                // virtualSocketFactory = VirtualSocketFactory
                //         .getDefaultSocketFactory();
            } catch (InitializationException e) {
                throw new IOException("could not initialize socket factory: "
                        + e);
            }

            // serverSocket = socketFactory.createServerSocket(0,
            // CONNECTION_BACKLOG,
            // null);

            virtualServerSocket = virtualSocketFactory.createServerSocket(0,
                    CONNECTION_BACKLOG, null);

            virtualServerAddress = createAddressFromString(serverString,
                    defaultServerPort);

            logger.debug("local address = "
                    + virtualServerSocket.getLocalSocketAddress());
            logger.debug("server address = " + virtualServerAddress);
        } else {
            virtualSocketFactory = null;
            virtualServerSocket = null;
            virtualServerAddress = null;

            plainServerSocket = new ServerSocket(0, CONNECTION_BACKLOG);
            plainLocalAddress = IPUtils.getLocalHostAddress();

            if (serverString != null) {
                try {
                    String[] addressParts = serverString.split(":", 2);

                    String serverHost = addressParts[0];
                    int serverPort;

                    if (addressParts.length < 2) {
                        serverPort = defaultServerPort;
                    } else {
                        serverPort = Integer.parseInt(addressParts[1]);
                    }

                    plainServerAddress = new InetSocketAddress(serverHost,
                            serverPort);
                } catch (Throwable t) {
                    throw new IOException("illegal server address ("
                            + serverString + ") : " + t.getMessage());
                }
            } else {
                plainServerAddress = null;
            }
        }
    }

    ConnectionFactory(int port, boolean smart, String hubAddress,
            Properties defaults) throws IOException {
        this.smart = smart;

        if (port < 0) {
            throw new IOException("port number cannot be negative " + port);
        }
        logger.debug("port = " + port);

        if (smart) {
            plainServerSocket = null;
            plainServerAddress = null;
            plainLocalAddress = null;

            try {
                Properties smartProperties = new Properties(defaults);
                if (hubAddress != null) {
                    smartProperties.setProperty(
                            smartsockets.Properties.HUB_ADDRESS, hubAddress);
                }
                smartProperties.setProperty(
                        smartsockets.Properties.DIRECT_PORT, Integer
                                .toString(port));

                virtualSocketFactory = VirtualSocketFactory
                        .createSocketFactory(smartProperties, true);
            } catch (InitializationException e) {
                throw new IOException("could not initialize socket factory: "
                        + e);
            }

            // serverSocket = socketFactory.createServerSocket(0,
            // CONNECTION_BACKLOG,
            // null);

            virtualServerSocket = virtualSocketFactory.createServerSocket(port,
                    CONNECTION_BACKLOG, null);

            virtualServerAddress = null;

            logger.debug("local address = "
                    + virtualServerSocket.getLocalSocketAddress());
            logger.debug("server address = " + virtualServerAddress);
        } else {
            virtualSocketFactory = null;
            virtualServerSocket = null;
            virtualServerAddress = null;

            plainServerSocket = new ServerSocket(port, CONNECTION_BACKLOG);
            plainLocalAddress = IPUtils.getLocalHostAddress();
            plainServerAddress = null;
        }
    }

    private static VirtualSocketAddress createAddressFromString(
            String serverString, int defaultPort) throws IOException {

        if (serverString == null) {
            return null;
        }

        VirtualSocketAddress serverAddress = null;

        // first, try to create a complete virtual socket address
        try {
            serverAddress = new VirtualSocketAddress(serverString);
        } catch (IllegalArgumentException e) {
            logger.debug("could not create server address", e);
        }

        // maybe it is a socketaddressset without a virtual port?
        if (serverAddress == null) {
            try {
                DirectSocketAddress directAddress = DirectSocketAddress
                        .getByAddress(serverString);
                int[] ports = directAddress.getPorts(false);
                if (ports.length == 0) {
                    throw new IOException(
                            "cannot determine port from server address: "
                                    + serverString);
                }
                int port = ports[0];
                for (int p : ports) {
                    if (p != port) {
                        throw new IOException(
                                "cannot determine port from server address: "
                                        + serverString);
                    }
                }
                serverAddress = new VirtualSocketAddress(directAddress, port);
            } catch (IllegalArgumentException e) {
                logger.debug("could not create server address", e);
            }
        }

        // maybe it is only a hostname?
        if (serverAddress == null) {
            try {
                DirectSocketAddress directAddress = DirectSocketAddress
                        .getByAddress(serverString, defaultPort);
                serverAddress = new VirtualSocketAddress(directAddress,
                        defaultPort);
            } catch (Exception e) {
                logger.debug("could not create server address", e);
            }
        }

        if (serverAddress == null) {
            throw new IOException("Invalid server address: " + serverString);
        }

        return serverAddress;
    }

    Connection accept() throws IOException {
        if (smart) {
            return new Connection(virtualServerSocket);
        } else {
            return new Connection(plainServerSocket);
        }
    }

    Connection connect(IbisIdentifier ibis, byte opcode, int timeout)
            throws IOException {
        if (smart) {
            VirtualSocketAddress address = VirtualSocketAddress.fromBytes(ibis
                    .getRegistryData(), 0);
            return new Connection(address, virtualSocketFactory, opcode,
                    timeout, false);
        } else {
            InetSocketAddress address = plainAddressFromBytes(ibis
                    .getRegistryData());

            return new Connection(address, opcode, timeout, false);
        }
    }

    public void end() {
        try {
            if (smart) {
                virtualServerSocket.close();
            } else {
                plainServerSocket.close();
            }
        } catch (IOException e) {
            // IGNORE
        }
    }

    public byte[] getLocalAddress() {
        if (smart) {
            return virtualServerSocket.getLocalSocketAddress().toBytes();
        } else {
            return plainAddressToBytes(plainLocalAddress, plainServerSocket
                    .getLocalPort());
        }
    }

    public String getAddressString() {
        if (smart) {
            return virtualServerSocket.getLocalSocketAddress().toString();
        } else {
            return plainLocalAddress.getHostAddress() + ":"
                    + plainServerSocket.getLocalPort();
        }
    }

    public Connection connectToServer(byte opcode, int timeout)
            throws IOException {
        if (smart) {
            if (virtualServerAddress == null) {
                throw new IOException(
                        "could not connect to server, address not specified");
            }
            return new Connection(virtualServerAddress, virtualSocketFactory,
                    opcode, timeout, true);
        } else {
            if (plainServerAddress == null) {
                throw new IOException(
                        "could not connect to server, address not specified");
            }
            return new Connection(plainServerAddress, opcode, timeout, true);
        }
    }

    public boolean serverIsLocalHost() {
        if (smart) {
            if (virtualServerAddress == null) {
                return false;
            }

            return virtualServerSocket.getLocalSocketAddress().machine()
                    .sameMachine(virtualServerAddress.machine());
        } else {
            if (plainServerAddress == null) {
                return false;
            }
            if (plainServerAddress.getAddress().isLoopbackAddress()) {
                return true;
            }
            return plainServerAddress.equals(plainLocalAddress);
        }
    }

    public int getServerPort() {
        if (smart) {
            return virtualServerAddress.port();
        } else {
            return plainServerAddress.getPort();
        }
    }
}
