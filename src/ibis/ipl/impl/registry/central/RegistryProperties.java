package ibis.ipl.impl.registry.central;

import ibis.util.TypedProperties;

import java.util.LinkedHashMap;
import java.util.Map;

public class RegistryProperties {

    public static final String PREFIX = "ibis.registry.central.";

    public static final String HEARTBEAT_INTERVAL = PREFIX
            + "heartbeat.interval";

    public static final String EVENT_PUSH_INTERVAL = PREFIX
            + "event.push.interval";

    public static final String GOSSIP = PREFIX + "gossip";

    public static final String GOSSIP_INTERVAL = PREFIX + "gossip.interval";

    public static final String ADAPT_GOSSIP_INTERVAL = PREFIX
            + "adapt.gossip.interval";

    public static final String TREE = PREFIX + "tree";

    public static final String PEER_BOOTSTRAP = PREFIX + "peer.bootstrap";

    public static final String CLIENT_CONNECT_TIMEOUT = PREFIX
            + "connect.timeout";

    public static final String SERVER_PREFIX = PREFIX + "server.";

    public static final String SERVER_CONNECT_TIMEOUT = SERVER_PREFIX
            + "connect.timeout";

    public static final String SERVER_STANDALONE = SERVER_PREFIX + "standalone";

    public static final String SERVER_ADDRESS = SERVER_PREFIX + "address";

    public static final String SERVER_PORT = SERVER_PREFIX + "port";

    public static final String SERVER_PRINT_EVENTS = SERVER_PREFIX
            + "print.events";

    public static final String SERVER_PRINT_ERRORS = SERVER_PREFIX
            + "print.errors";

    public static final String SERVER_PRINT_STATS = SERVER_PREFIX
            + "print.stats";

    // list of decriptions and defaults
    private static final String[][] propertiesList = new String[][] {

            {
                    HEARTBEAT_INTERVAL,
                    "120",
                    "Int(seconds): how often is a member "
                            + "of a pool expected to report it is still alive" },

            {
                    EVENT_PUSH_INTERVAL,
                    "60",
                    "Int(seconds): how often does the server "
                            + "send out events any member may have missed" },

            { GOSSIP, "false",
                    "Boolean: do we gossip, or send events centrally" },

            { GOSSIP_INTERVAL, "1", "Int(seconds): how often do we gossip" },

            {
                    ADAPT_GOSSIP_INTERVAL,
                    "false",
                    "Boolean: if true, the server gossips more often if there are "
                            + "more nodes in a pool" },

            {
                    TREE,
                    "false",
                    "Boolean: use a broadcast tree instead of "
                            + "serial send or gossiping" },

            { PEER_BOOTSTRAP, "true",
                    "Boolean: bootstrap with peers, not just with the server" },

            { SERVER_STANDALONE, "false",
                    "Boolean: if true, a stand-alone server is used/expected" },

            { SERVER_ADDRESS, null,
                    "Socket Address of standalone server to connect to" },

            { SERVER_PORT, "7777",
                    "Int: Port which the standalone server binds to" },

            {
                    CLIENT_CONNECT_TIMEOUT,
                    "120",
                    "Int(seconds): how long do clients attempt to connect to the server and peers before giving up" },

            {
                    SERVER_CONNECT_TIMEOUT,
                    "10",
                    "Int(seconds): how long does the server attempt to connect to a client before giving up" },

            { SERVER_PRINT_EVENTS, "false",
                    "Boolean: if true, events are printed to standard out" },
            { SERVER_PRINT_ERRORS, "false",
                    "Boolean: if true, details of errors are printed to standard out" },

            { SERVER_PRINT_STATS, "false",
                    "Boolean: if true, statistics are printed to standard out regularly" },

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
        Map<String, String> result = new LinkedHashMap<String, String>();

        for (String[] element : propertiesList) {
            result.put(element[0], element[2]);
        }

        return result;
    }

}
