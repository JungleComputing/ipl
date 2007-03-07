package ibis.impl.registry;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class RegistryProperties {

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

    // "tcp" and "smartsockets" registry specific properties
    
    public static final String CHECKER_INTERVAL = SERVER_PREFIX
            + "checkerInterval";

    // "central" registry specific properties

    public static final String SMARTSOCKETS = PREFIX + "smartSockets";
    
    public static final String GOSSIP = PREFIX + "gossip";
    
    public static final String KEEP_NODE_STATE = PREFIX + "keepClientState";  

    // list of decriptions and defaults
    private static final String[][] propertiesList = new String[][] {
            {IMPL,
            "Implementation of registry(client)",
            "ibis.impl.registry.tcp.NameServerClient"},

            {SERVER_ADDRESS,
            "Address of server to connect to",
            "localhost:9826"},

            {POOL,
            "Pool to join",
            "DEFAULT"},

            {SERVER_PORT,
            "Int: Port which the server binds to",
            "9826"},

            {SERVER_IMPL,
            "Implementation of the registry server (must match client)",
            "ibis.impl.registry.tcp.NameServer"},
            
            {SERVER_SINGLE,
            "Boolean: Stop server after one pool has been served",
            "false"},

            {CHECKER_INTERVAL,
            "Int: How often do we check if each Ibis in the pool is still alive 0=never",
            "0"},

            {SMARTSOCKETS,
            "Boolean: if true use smartsockets, if false just plain tcp",
            "true"},
            
            {GOSSIP,
             "Boolean: do we gossip, or send events centrally",
             "false"},
             
            {KEEP_NODE_STATE,
             "Boolean: do we keep track of which events nodes have",
             "false"},
    };

    public static Properties getHardcodedProperties() {
        Properties properties = new Properties();

        for (String[] element: propertiesList) {
            properties.setProperty(element[0], element[2]);
        }

        return properties;
    }

    public static Map<String, String> getDescriptions() {
        Map<String, String> result = new HashMap<String, String>();

        for (String[] element: propertiesList) {
            result.put(element[0], element[1]);
        }

        return result;
    }

}
