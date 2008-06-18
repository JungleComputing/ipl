package ibis.server;

import ibis.util.TypedProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Properties valid for the Ibis server
 */
public final class ServerProperties {

    public static final String PREFIX = "ibis.server.";

    public static final String HUB_ADDRESSES = PREFIX + "hub.addresses";

    public static final String START_HUB = PREFIX + "start.hub";

    public static final String HUB_ONLY = PREFIX + "hub.only";

    public static final String HUB_ADDRESS_FILE = PREFIX + "hub.address.file";

    public static final String PORT = PREFIX + "port";

    public static final String PRINT_EVENTS = PREFIX + "print.events";

    public static final String PRINT_STATS = PREFIX + "print.stats";

    public static final String PRINT_ERRORS = PREFIX + "print.errors";

    public static final String REMOTE = PREFIX + "remote";

    // client side properties

    public static final String ADDRESS = PREFIX + "address";

    public static final String IS_HUB = PREFIX + "is.hub";

    private static final String[][] propertiesList = new String[][] {
            { HUB_ADDRESSES, null, "Comma seperated list of hubs." },

            { START_HUB, "true",
                    "Boolean: if true, also start a hub at the server" },

            { HUB_ONLY, "false",
                    "Boolean: if true, only start a hub, not the rest of the server" },

            { HUB_ADDRESS_FILE, null,
                    "String: file where the address of the hub is printed to (and deleted on exit)" },

            { PORT, "8888", "Port which the server binds to" },

            { PRINT_EVENTS, "false",
                    "Boolean: if true, events of services are printed to standard out." },
            { PRINT_ERRORS, "false",
                    "Boolean: if true, details of errors (like stacktraces) are printed" },
            { PRINT_STATS, "false",
                    "Boolean: if true, statistics are printed to standard out regularly." },
            {
                    REMOTE,
                    "false",
                    "Boolean: If true, the server listens to stdin for commands and responds on stdout" },
            { ADDRESS, null, "Address of the server" },
            { IS_HUB, "true", "Boolean: Is the server also a hub?" }, };

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
