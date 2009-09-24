package ibis.ipl.server;

import ibis.ipl.IbisFactory;
import ibis.ipl.registry.Registry;
import ibis.util.TypedProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Properties valid for the Ibis server
 * 
 * @ibis.experimental
 */
public final class ServerProperties {

    public static final String PREFIX = "ibis.server.";

    public static final String START_HUB = PREFIX + "start.hub";

    public static final String HUB_ONLY = PREFIX + "hub.only";

    public static final String HUB_ADDRESS_FILE = PREFIX + "hub.address.file";

    public static final String PORT = PREFIX + "port";

    public static final String PRINT_EVENTS = PREFIX + "print.events";

    public static final String PRINT_STATS = PREFIX + "print.stats";

    public static final String PRINT_ERRORS = PREFIX + "print.errors";

    public static final String REMOTE = PREFIX + "remote";

    public static final String SERVICES = PREFIX + "services";

    public static final String VIZ_INFO = PREFIX + "viz.info";

    /** Property name for specifying a comma separated list of hubs. */
    public static final String HUB_ADDRESSES = PREFIX + "ibis.hub.addresses";

    public static final String implementationVersion;

    public static final int DEFAULT_PORT = 8888;

    static {
        String version = Registry.class.getPackage().getImplementationVersion();

        if (version == null || version.equals("0.0")) {
            // try to get version from IPL_MANIFEST file
            version = IbisFactory.getManifestProperty("support.version");
        }

        if (version == null) {
            throw new Error("Cannot get version for server");
        }

        implementationVersion = version;
    }

    private static final String[][] propertiesList = new String[][] {
            { HUB_ADDRESSES, null, "Comma separated list of hubs." },

            { START_HUB, "true",
                    "Boolean: if true, also start a hub at the server" },

            { HUB_ONLY, "false",
                    "Boolean: if true, only start a hub, not the rest of the server" },

            { HUB_ADDRESS_FILE, null,
                    "String: file where the address of the hub is printed to (and deleted on exit)" },

            { PORT, Integer.toString(DEFAULT_PORT),
                    "Port which the server binds to" },

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

            { VIZ_INFO, null, "String: info for smartsockets visualization" },

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
