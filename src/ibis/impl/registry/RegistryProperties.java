package ibis.impl.registry;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class RegistryProperties {
    
    public static final int DEFAULT_SERVER_PORT = 9826;

    public static final String PREFIX = "ibis.registry.";

    // client-only properties

    public static final String IMPL = PREFIX + "impl";

    public static final String SERVER_ADDRESS = PREFIX + "serverAddress";

    public static final String POOL = PREFIX + "pool";

    // server-only properties

    public static final String SERVER_PREFIX = PREFIX + "server.";

    public static final String SERVER_PORT = SERVER_PREFIX + "port";

    public static final String SERVER_IMPL = SERVER_PREFIX + "impl";

    public static final String SERVER_SINGLE = SERVER_PREFIX + "single";
    
    // address of smart sockets hub
    public static final String SERVER_HUB_ADDRESS = SERVER_PREFIX
            + "hubAddress";


    // "tcp" and "smartsockets" registry specific properties

    public static final String CLASSIC_PREFIX = PREFIX + "classic.";

    public static final String CLASSIC_PINGER_TIMEOUT = CLASSIC_PREFIX
            + "pingerTimeout";

    public static final String CLASSIC_CHECKER_INTERVAL = CLASSIC_PREFIX
            + "checkerInterval";

    public static final String CLASSIC_CONNECT_TIMEOUT = CLASSIC_PREFIX
            + "connectTimeout";

    public static final String CLASSIC_JOINER_INTERVAL = CLASSIC_PREFIX
            + "joinerInterval";

    public static final String CLASSIC_MAX_THREADS = CLASSIC_PREFIX
            + "maxThreads";

    // "central" registry specific properties

    public static final String CENTRAL_PREFIX = PREFIX + "central.";

    public static final String CENTRAL_SMARTSOCKETS = CENTRAL_PREFIX
            + "smartSockets";

    public static final String CENTRAL_GOSSIP = CENTRAL_PREFIX + "gossip";

    public static final String CENTRAL_KEEP_NODE_STATE = CENTRAL_PREFIX
            + "keepNodeState";
    
    public static final String CENTRAL_SERVER_CONNECT_TIMEOUT = CENTRAL_PREFIX
    + "serverConnectTimeout";
    
    // list of decriptions and defaults
    private static final String[][] propertiesList = new String[][] {
            { IMPL, "ibis.impl.registry.central.Registry",
                    "Implementation of registry(client)" },

            { SERVER_ADDRESS, "localhost:9826",
                    "Socket Address of server to connect to" },

            { POOL, "DEFAULT", "Pool to join" },

            { SERVER_PORT, "9826", "Int: Port which the server binds to" },

            { SERVER_IMPL, "ibis.impl.registry.central.Server",
                    "Implementation of the registry server (must match client)" },

            { SERVER_SINGLE, "false",
                    "Boolean: Stop server after one pool has been served" },

            { CLASSIC_PINGER_TIMEOUT, "60",
                    "Int: seconds until we delare a ping attempt failed" },

            {
                    CLASSIC_CHECKER_INTERVAL,
                    "0",
                    "Int(seconds): How often do we check if each Ibis in the "
                            + "pool is still alive (0 = never)" },

            { CLASSIC_CONNECT_TIMEOUT, "10",
                    "Int(seconds): How long may creating a connection take" },

            { CLASSIC_JOINER_INTERVAL, "5",
                    "Int(seconds): How often do we forward joins to nodes" },

            { CLASSIC_MAX_THREADS, "8",
                    "Int: Maximum number of forwarder threads" },
                    
            { CENTRAL_SMARTSOCKETS, "true",
                    "Boolean: if true use smartsockets, if false just plain tcp" },

            { CENTRAL_GOSSIP, "false",
                    "Boolean: do we gossip, or send events centrally" },

            { CENTRAL_KEEP_NODE_STATE, "false",
                    "Boolean: do we keep track of which events nodes have" },
                    
            { CENTRAL_SERVER_CONNECT_TIMEOUT, "120",
                "int(seconds): how long do we attempt to connect to the server" },

    };

    public static Properties getHardcodedProperties() {
        Properties properties = new Properties();

        for (String[] element : propertiesList) {
            if (element[1] != null) {
                properties.setProperty(element[0], element[1]);
            }
        }

        return properties;
    }

    public static Map<String, String> getDescriptions() {
        Map<String, String> result = new HashMap<String, String>();

        for (String[] element : propertiesList) {
            result.put(element[0], element[2]);
        }

        return result;
    }

}
