package ibis.ipl;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Utility methods for setting and getting configuration properties. The
 * {@link #getConfigurationProperties()} method obtains the properties in the
 * following order: first, some hardcoded properties are set. Next, a file
 * <code>ibis.properties</code> is obtained from the classpath, if present. If
 * so, it is read as a properties file, and the properties contained in it are
 * set, possibly overriding already set properties. Next, the same is done for
 * the property file <code>ibis.properties</code> in the user home directory,
 * if present. And finally, the same is done for the property file
 * <code>ibis.properties</code> in the current directory. This allows the user
 * to set some properties that he/she wants for all his/her applications in a
 * property file in his/her home directory, and to also set some
 * application-specific properties in the current directory.
 */
public final class IbisProperties {

    /** Filename for the properties. */
    public static final String PROPERTIES_FILENAME = "ibis.properties";

    /** All our own properties start with this prefix. */
    public static final String PREFIX = "ibis.";

    /** Property name for selecting an Ibis. */
    public static final String NAME = PREFIX + "name";

    /** Property name of the property file. */
    public static final String PROPERTIES_FILE = PREFIX + "properties.file";

    /** Property name for the path used to find Ibis implementations. */
    public static final String IMPL_PATH = PREFIX + "impl.path";

    /** Property name for verbosity. */
    public static final String VERBOSE = PREFIX + "verbose";

    public static final String POOL_NAME = PREFIX + "pool.name";

    public static final String POOL_SIZE = PREFIX + "pool.size";

    public static final String SERVER_ADDRESS = PREFIX + "server.address";

    public static final String HUB_ADDRESSES = PREFIX + "hub.addresses";

    public static final String REGISTRY_IMPL = PREFIX + "registry.impl";

    /**
     * Property name for location.
     */
    public static final String LOCATION = PREFIX + "location";

    public static final String LOCATION_AUTOMATIC = PREFIX
            + "location.automatic";

    /** List of {NAME, DESCRIPTION, DEFAULT_VALUE} for properties. */
    private static final String[][] propertiesList = new String[][] {
            { POOL_NAME, null, "name of the pool this ibis belongs to" },
            { POOL_SIZE, null, "size of the pool this ibis belongs to" },
            { SERVER_ADDRESS, null, "address of the central ibis server" },
            { HUB_ADDRESSES, null, "addresses of additional hubs" },

            { NAME, null, "Nickname or classname of the Ibis implementation" },

            { PROPERTIES_FILE, null,
                    "Name of the property file used for the configuration of Ibis" },

            { IMPL_PATH, null, "Path used to find Ibis implementations" },

            { VERBOSE, "false",
                    "Boolean: If true, makes Ibis more verbose, if false, does not" },

            {
                    LOCATION,
                    null,
                    "Set the location of Ibis. Specified as multiple levels, "
                            + "seperated by a '@', e.g. machine@cluster@site@grid@world."
                            + "Defaults to a single level location with the hostname of the machine" },

            { LOCATION_AUTOMATIC, "false",
                    "Boolean: If true, a multi level location is automatically created" },

            {
                    REGISTRY_IMPL,
                    "ibis.ipl.impl.registry.central.Registry",
                    "implementation of the registry. Not all Ibis implementations use this property" },

    };

    /**
     * Private constructor, to prevent construction of an IbisProperties object.
     */
    private IbisProperties() {
        // nothing
    }

    /**
     * Returns the built-in properties of Ibis.
     * 
     * @return the resulting properties.
     */
    public static Properties getHardcodedProperties() {
        Properties properties = new Properties();

        for (String[] element : propertiesList) {
            if (element[1] != null) {
                properties.setProperty(element[0], element[1]);
            }
        }

        return properties;
    }

    /**
     * Returns a map mapping hard-coded property names to their descriptions.
     * 
     * @return the name/description map.
     */
    public static Map<String, String> getDescriptions() {
        Map<String, String> result = new HashMap<String, String>();

        for (String[] element : propertiesList) {
            result.put(element[0], element[2]);
        }

        return result;
    }
}
