package ibis.ipl.registry.central.monitor;

import ibis.ipl.registry.Connection;
import ibis.ipl.registry.central.Protocol;
import ibis.ipl.registry.central.RegistryProperties;
import ibis.ipl.registry.central.server.CentralRegistryService;
import ibis.ipl.server.Client;
import ibis.smartsockets.hub.servicelink.ServiceLink;
import ibis.smartsockets.virtual.VirtualSocketAddress;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class used to get monitoring information from the registry server.
 * 
 * @author ndrost
 * 
 */
public class RegistryMonitorClient {

    private static final Logger logger = LoggerFactory
            .getLogger(RegistryMonitorClient.class);

    /**
     * Maximum connection time before we give up
     */
    public static final int CONNECTION_TIMEOUT = 60000;

    VirtualSocketFactory factory;

    VirtualSocketAddress serverAddress;
    
    /**
     * Creates a monitor client
     * 
     * @param properties
     *            properties for settings (mostly server address)
     * @param addSystemProperties
     *            add system properties to given properties
     * @throws Exception
     *             in case creating a socket factory and the server address
     *             fails
     */
    public RegistryMonitorClient(Properties properties,
            boolean addSystemProperties) throws Exception {
        TypedProperties typedProperties = RegistryProperties
                .getHardcodedProperties();

        if (addSystemProperties) {
            typedProperties.addProperties(System.getProperties());
        }

        typedProperties.addProperties(properties);

        factory = Client.getFactory(typedProperties);

        try {
            ServiceLink sl = factory.getServiceLink();
            if (sl != null) {
                sl.registerProperty("smartsockets.viz", "M^Ibis monitor");
            } else {
                logger
                        .warn("could not set smartsockets viz property: could not get smartsockets service link");
            }
        } catch (Throwable e) {
            logger.warn("cannot set smartsockets viz tag", e);
        }

        serverAddress = Client.getServiceAddress(CentralRegistryService.VIRTUAL_PORT,
            typedProperties);
        
        logger.debug("server address = " + serverAddress);

    }

    /**
     * Returns a map containing the current size of each pool of the registry
     * server.
     * 
     * @return a map containing the current size of each pool of the registry
     *         server.
     * @throws IOException
     */
    public Map<String, Integer> getPoolSizes() throws IOException {
        Map<String, Integer> result = new HashMap<String, Integer>();

        TypedProperties data = getStats();

        for (String poolName : data.getStringList("pool.names")) {
            if (poolName != null && poolName.length() > 0) {
                result.put(poolName, data.getIntProperty(poolName + ".size"));
            }
        }

        return result;
    }

    /**
     * Returns a list of all locations used by Ibises in the given pool.
     * 
     * @param poolName
     *            name of pool.
     * @return a list of all locations in the given pool.
     * @throws IOException in case the server cannot be reached
     */
    public String[] getLocations(String poolName) throws IOException {
        String[] result = null;

        Connection connection = new Connection(serverAddress,
                CONNECTION_TIMEOUT, true, factory);

        try {

            connection.out().write(Protocol.MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_GET_LOCATIONS);
            connection.out().writeUTF(poolName);
            connection.out().flush();

            connection.getAndCheckReply();

            int nrOfEntries = connection.in().readInt();

            result = new String[nrOfEntries];
            for (int i = 0; i < nrOfEntries; i++) {
                result[i] = connection.in().readUTF();
            }

            connection.close();

            logger.debug("done getting locations");

            return result;
        } catch (IOException e) {
            connection.close();
            throw e;
        }
    }

    /**
     * Returns a map containing statistics of the registry server. For
     * convenience, its in the form of a TypedProperties object
     * 
     * @return a map containing statistics of the registry server.
     * @throws IOException
     */
    public TypedProperties getStats() throws IOException {
        TypedProperties result = new TypedProperties();

        Connection connection = new Connection(serverAddress,
                CONNECTION_TIMEOUT, true, factory);

        try {

            connection.out().write(Protocol.MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_GET_STATS);
            connection.out().flush();

            connection.getAndCheckReply();

            int nrOfEntries = connection.in().readInt();

            for (int i = 0; i < nrOfEntries; i++) {
                String key = connection.in().readUTF();
                String value = connection.in().readUTF();

                result.put(key, value);
            }

            connection.close();

            logger.debug("done getting pool sizes");

            return result;

        } catch (IOException e) {
            connection.close();
            throw e;
        }

    }

}
