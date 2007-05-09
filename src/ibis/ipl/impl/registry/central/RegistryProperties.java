package ibis.ipl.impl.registry.central;

import ibis.util.TypedProperties;

import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

public class RegistryProperties {

    public static final String PREFIX = "ibis.registry.central.";

    public static final String GOSSIP = PREFIX + "gossip";

    public static final String KEEP_NODE_STATE = PREFIX + "keep.node.state";

    public static final String PING_INTERVAL = PREFIX + "ping.interval";

    public static final String CONNECT_TIMEOUT = PREFIX + "connect.timeout";

    public static final String SERVER_PREFIX = PREFIX + "server.";

    public static final String SERVER_STANDALONE = SERVER_PREFIX + "standalone";

    public static final String SERVER_ADDRESS = SERVER_PREFIX + "address";

    public static final String SERVER_PORT = SERVER_PREFIX + "port";

    public static final String SERVER_LOG_EVENTS = SERVER_PREFIX + "log.events";

    public static final String SERVER_LOG_STATS = SERVER_PREFIX + "log.stats";

    // list of decriptions and defaults
    private static final String[][] propertiesList = new String[][] {
            { GOSSIP, "false",
                    "Boolean: do we gossip, or send events centrally" },

            { KEEP_NODE_STATE, "false",
                    "Boolean: do we keep track of which events nodes have" },

            { PING_INTERVAL, "300000",
                    "Long: how often do we check if a member of a pool is still alive" },

            { SERVER_STANDALONE, "false",
                    "Boolean: if true, a stand-alone server is used/expected" },

            { SERVER_ADDRESS, null,
                    "Socket Address of standalone server to connect to" },

            { SERVER_PORT, "7777",
                    "Int: Port which the standalone server binds to" },

            { CONNECT_TIMEOUT, "120",
                    "int(seconds): how long do we attempt to connect before giving up" },

            { SERVER_LOG_EVENTS, "false", "Boolean: if true, events are logged" },
            { SERVER_LOG_STATS, "false",
                    "Boolean: if true, statistics are printed to the log regularly" },

    };

    public static TypedProperties getHardcodedProperties() {
        TypedProperties properties = new TypedProperties();

        for (String[] element : propertiesList) {
            if (element[1] != null) {
                properties.setProperty(element[0], element[1]);
            }
        }

        return properties;
    }

    public static Map<String, String> getDescriptions() {
        Map<String, String> result = new TreeMap<String, String>();

        for (String[] element : propertiesList) {
            result.put(element[0], element[2]);
        }

        return result;
    }

}
