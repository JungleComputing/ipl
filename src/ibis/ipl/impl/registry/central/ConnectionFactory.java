package ibis.ipl.impl.registry.central;

import ibis.ipl.IbisConfigurationException;
import ibis.ipl.IbisProperties;
import ibis.ipl.impl.IbisIdentifier;
import ibis.util.IPUtils;
import ibis.util.TypedProperties;
import ibis.util.io.Conversion;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import org.apache.log4j.Logger;

import ibis.smartsockets.virtual.InitializationException;
import ibis.smartsockets.virtual.VirtualServerSocket;
import ibis.smartsockets.virtual.VirtualSocketAddress;
import ibis.smartsockets.virtual.VirtualSocketFactory;

import ibis.server.Client;

final class ConnectionFactory {
    private static final int CONNECTION_BACKLOG = 50;

    private static final Logger logger = Logger
            .getLogger(ConnectionFactory.class);

    private final boolean standalone;

    private final int timeout;

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

    /**
     * Create a connectionfactory for the registry client
     */
    ConnectionFactory(TypedProperties properties) throws IOException {

        standalone = properties
                .getBooleanProperty(RegistryProperties.SERVER_STANDALONE);
        timeout = properties.getIntProperty(RegistryProperties.CONNECT_TIMEOUT) * 1000;

        if (standalone) {

            virtualSocketFactory = null;
            virtualServerSocket = null;
            virtualServerAddress = null;

            plainServerSocket = new ServerSocket(0, CONNECTION_BACKLOG);
            plainLocalAddress = IPUtils.getLocalHostAddress();

            String serverString = properties
                    .getProperty(RegistryProperties.SERVER_ADDRESS);

            int defaultServerPort = properties
                    .getIntProperty(RegistryProperties.SERVER_PORT);

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
                    throw new IbisConfigurationException("illegal server address ("
                            + serverString + ") : " + t.getMessage());
                }
            } else {
                plainServerAddress = null;
            }

        } else {
            plainServerSocket = null;
            plainServerAddress = null;
            plainLocalAddress = null;
            
            // check if the server address is set...
            if (properties.getProperty(IbisProperties.SERVER_ADDRESS) == null) {
                throw new IbisConfigurationException("cannot initialize registry, property "
                        + IbisProperties.SERVER_ADDRESS + " is not specified");
            }

            try {
                virtualSocketFactory = Client.getFactory(properties);
            } catch (InitializationException e) {
                throw new IOException("Could not create socket factory: " + e);
            }

            virtualServerSocket = virtualSocketFactory.createServerSocket(0,
                    CONNECTION_BACKLOG, null);

            virtualServerAddress = Client.getServiceAddress(
                    Server.VIRTUAL_PORT, properties);

            logger.debug("local address = "
                    + virtualServerSocket.getLocalSocketAddress());
            logger.debug("server address = " + virtualServerAddress);
        }
    }

    /**
     * Create a standalone server connection factory
     */
    ConnectionFactory(int port, int timeout) throws IOException {
        this.standalone = true;
        this.timeout = timeout;

        if (port < 0) {
            throw new IOException("port number cannot be negative " + port);
        }
        logger.debug("port = " + port);

        virtualSocketFactory = null;
        virtualServerSocket = null;
        virtualServerAddress = null;

        plainServerSocket = new ServerSocket(port, CONNECTION_BACKLOG);
        plainLocalAddress = IPUtils.getLocalHostAddress();
        plainServerAddress = null;
    }

    /**
     * Create a connectionfactory for a registry which is part of an IbisServer
     */
    ConnectionFactory(VirtualSocketFactory factory, int virtualPort, int timeout)
            throws IOException {
        this.standalone = false;
        this.timeout = timeout;

        plainServerSocket = null;
        plainServerAddress = null;
        plainLocalAddress = null;

        virtualSocketFactory = factory;

        virtualServerSocket = virtualSocketFactory.createServerSocket(
                virtualPort, CONNECTION_BACKLOG, null);

        virtualServerAddress = null;

        logger.debug("local address = "
                + virtualServerSocket.getLocalSocketAddress());
        logger.debug("server address = " + virtualServerAddress);
    }

    Connection accept() throws IOException {
        if (standalone) {
            return new Connection(plainServerSocket);
        } else {
            return new Connection(virtualServerSocket);
        }
    }

    Connection connect(IbisIdentifier ibis, byte opcode, boolean fillTimeout) throws IOException {
        if (standalone) {
            InetSocketAddress address = plainAddressFromBytes(ibis
                    .getRegistryData());

            return new Connection(address, opcode, timeout, false);

        } else {
            VirtualSocketAddress address = VirtualSocketAddress.fromBytes(ibis
                    .getRegistryData(), 0);
            return new Connection(address, virtualSocketFactory, opcode,
                    timeout, fillTimeout);

        }
    }

    void end() {
        try {
            if (standalone) {
                plainServerSocket.close();
            } else {
                virtualServerSocket.close();
            }
        } catch (IOException e) {
            // IGNORE
        }
    }

    byte[] getLocalAddress() {
        if (standalone) {
            return plainAddressToBytes(plainLocalAddress, plainServerSocket
                    .getLocalPort());
        } else {
            return virtualServerSocket.getLocalSocketAddress().toBytes();
        }
    }

    String getAddressString() {
        if (standalone) {
            return plainLocalAddress.getHostAddress() + ":"
                    + plainServerSocket.getLocalPort();
        } else {
            return virtualServerSocket.getLocalSocketAddress().toString();
        }
    }

    Connection connectToServer(byte opcode) throws IOException {
        if (standalone) {
            if (plainServerAddress == null) {
                throw new IOException(
                        "could not connect to server, address not specified");
            }
            return new Connection(plainServerAddress, opcode, timeout, true);
        } else {

            if (virtualServerAddress == null) {
                throw new IOException(
                        "could not connect to server, address not specified");
            }
            return new Connection(virtualServerAddress, virtualSocketFactory,
                    opcode, timeout, true);

        }
    }

    boolean serverIsLocalHost() {
        if (standalone) {

            if (plainServerAddress == null) {
                return false;
            }
            if (plainServerAddress.getAddress().isLoopbackAddress()) {
                return true;
            }
            return plainServerAddress.equals(plainLocalAddress);

        } else {
            if (virtualServerAddress == null) {
                return false;
            }

            return virtualServerSocket.getLocalSocketAddress().machine()
                    .sameMachine(virtualServerAddress.machine());
        }
    }

    int getServerPort() {
        if (standalone) {
            return plainServerAddress.getPort();
        } else {
            return virtualServerAddress.port();
        }
    }    
}
