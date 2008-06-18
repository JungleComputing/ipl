package ibis.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import ibis.smartsockets.SmartSocketsProperties;
import ibis.smartsockets.direct.DirectSocketAddress;
import ibis.smartsockets.virtual.InitializationException;
import ibis.smartsockets.virtual.VirtualSocketAddress;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import ibis.util.TypedProperties;

/**
 * Convenience class to retrieve information on the server, and create a
 * suitable VirtualSocketFactory.
 */
public class Client {

    private static final Logger logger = Logger.getLogger(Client.class);

    private static VirtualSocketFactory defaultFactory = null;

    private static Map<String, VirtualSocketFactory> factories = new HashMap<String, VirtualSocketFactory>();

    private Client() {
        // DO NOT USE
    }

    private static DirectSocketAddress createAddressFromString(
            String serverString, int defaultPort) throws ConfigurationException {

        if (serverString == null) {
            throw new ConfigurationException("serverString undefined");
        }

        // maybe it is a DirectSocketAddress?
        try {
            return DirectSocketAddress.getByAddress(serverString);
        } catch (Throwable e) {
            // IGNORE
        }

        Throwable throwable = null;
        // or only a host address
        try {
            return DirectSocketAddress.getByAddress(serverString, defaultPort);
        } catch (Throwable e) {
            throwable = e;
            // IGNORE
        }

        throw new ConfigurationException(
                "could not create server address from given string: "
                        + serverString, throwable);
    }

    /**
     * Get the address of a service running on a given port
     * 
     * @param port
     *            the port the service is running on
     * @param properties
     *            object containing any server properties needed (such as the
     *            servers address)
     */
    public static VirtualSocketAddress getServiceAddress(int port,
            Properties properties) throws ConfigurationException {
        TypedProperties typedProperties = ServerProperties
                .getHardcodedProperties();
        typedProperties.addProperties(properties);

        String serverAddressString = typedProperties
                .getProperty(ServerProperties.ADDRESS);
        if (serverAddressString == null || serverAddressString.equals("")) {
            throw new ConfigurationException(ServerProperties.ADDRESS
                    + " undefined, cannot locate server");
        }

        logger.debug("server address = \"" + serverAddressString + "\"");

        int defaultPort = typedProperties.getIntProperty(ServerProperties.PORT);

        DirectSocketAddress serverMachine = createAddressFromString(
                serverAddressString, defaultPort);

        if (serverMachine == null) {
            throw new ConfigurationException("cannot get address of server");
        }

        return new VirtualSocketAddress(serverMachine, port);
    }

    public static synchronized VirtualSocketFactory getFactory(Properties p)
            throws ConfigurationException, IOException {
        TypedProperties typedProperties = ServerProperties
                .getHardcodedProperties();
        typedProperties.addProperties(p);

        String hubs = typedProperties
                .getProperty(ServerProperties.HUB_ADDRESSES);

        // did the server also start a hub?
        boolean serverIsHub = typedProperties
                .getBooleanProperty(ServerProperties.IS_HUB);

        String server = typedProperties.getProperty(ServerProperties.ADDRESS);
        if (server != null && !server.equals("") && serverIsHub) {
            // add server to hub addresses
            DirectSocketAddress serverAddress = createAddressFromString(server,
                    typedProperties.getIntProperty(ServerProperties.PORT));
            if (hubs == null || hubs.equals("")) {
                hubs = serverAddress.toString();
            } else {
                hubs = hubs + "," + serverAddress.toString();
            }
        }

        if (hubs == null) {
            // return the default factory

            if (defaultFactory == null) {
                Properties smartProperties = new Properties();
                smartProperties.put(SmartSocketsProperties.DISCOVERY_ALLOWED,
                        "false");
                try {
                    defaultFactory = VirtualSocketFactory.createSocketFactory(
                            smartProperties, true);
                } catch (InitializationException e) {
                    throw new IOException(e.getMessage());
                }
            }
            return defaultFactory;
        }

        VirtualSocketFactory factory = factories.get(hubs);

        if (factory == null) {
            // return a factory for the specified "hubs" string
            Properties smartProperties = new Properties();
            smartProperties.put(SmartSocketsProperties.DISCOVERY_ALLOWED,
                    "false");
            smartProperties.put(SmartSocketsProperties.HUB_ADDRESSES, hubs);

            try {
                factory = VirtualSocketFactory.createSocketFactory(
                        smartProperties, true);
            } catch (InitializationException e) {
                throw new IOException(e.getMessage());
            }

            factories.put(hubs, factory);
        }

        return factory;
    }

}
