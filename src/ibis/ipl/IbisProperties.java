package ibis.ipl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Properties management for Ibis. The
 * {@link #getDefaultProperties()} method obtains the properties in the
 * following order: first, some hardcoded properties are set. Next, a file
 * <code>ibis.properties</code> is searched for in the current directory,
 * the classpath, or the user home directory, in that order.
 * If found, it is read as a properties file, and the properties contained in
 * it are set, possibly overriding the hardcoded properties.
 * Finally, the system properties are obtained. These, too, may override
 * the properties set so far.
 */
public final class IbisProperties {

    /** Filename for the properties. */
    public static final String PROPERTIES_FILENAME = "ibis.properties";

    /** All our own properties start with this prefix. */
    public static final String PREFIX = "ibis.";

    /** Property name for selecting an Ibis. */
    public static final String IMPLEMENTATION = PREFIX + "implementation";

    /** Last resort Ibis starters that are loaded with Class.forName if others fail */
    public static final String STARTER = PREFIX + "starter";
    
    /** Property name of the property file. */
    public static final String PROPERTIES_FILE = PREFIX + "properties.file";

    /** Property name for the path used to find Ibis implementations. */
    public static final String IMPLEMENTATION_PATH = PREFIX + "implementation.path";

    /** Property name for verbosity. */
    public static final String VERBOSE = PREFIX + "verbose";

    /** Property name for setting the name of the pool. */
    public static final String POOL_NAME = PREFIX + "pool.name";

    /** Property name for setting the size of the pool. */
    public static final String POOL_SIZE = PREFIX + "pool.size";

    /** Property name for setting the address of the ipl-server. */
    public static final String SERVER_ADDRESS = PREFIX + "server.address";

    /** Property name for specifying a comma seperated list of hubs. */
    public static final String HUB_ADDRESSES = PREFIX + "hub.addresses";

    /** Property name for specifying the implementation of the registry. */
    public static final String REGISTRY_IMPLEMENTATION = PREFIX + "registry.implementation";
    
    /** 
     * Property name for specifying ibis starter classes in case they cannot
     * be read from a jar manifest.
     */
    public static final String IBIS_STARTERS = PREFIX + "starters";

    /** Property name for location. */
    public static final String LOCATION = PREFIX + "location";

    /**
     * Property name for specifying a postfix for an automatically generated location.
     */
    public static final String LOCATION_POSTFIX =
        PREFIX + "location.postfix";

    /** List of {NAME, DESCRIPTION, DEFAULT_VALUE} for properties. */
    private static final String[][] propertiesList =
            new String[][] {
                { POOL_NAME, null, "String: name of the pool this ibis belongs to" },
                { POOL_SIZE, null,
                    "Integer: size of the pool this ibis belongs to" },
                { SERVER_ADDRESS, null, "Address of the central ibis server" },
                {
                    HUB_ADDRESSES,
                    null,
                    "Comma seperated list of hub addresses."
                            + " The server address is appended to this list,"
                            + " and thus is the default hub if no hub is specified" },

                { IMPLEMENTATION, null,
                    "Nickname or classname of an Ibis implementation. " +
                    "The specified implementation is used, bypassing the automatic" +
                    " selection mechanism of Ibis."},

                { PROPERTIES_FILE, null,
                    "Name of the property file used for the configuration of Ibis" },

                { IMPLEMENTATION_PATH, null, "Path used to find Ibis implementations" },

                { VERBOSE, "false",
                    "Boolean: If true, makes Ibis more verbose, if false, does not" },

                { LOCATION, null,
                    "Set the location of Ibis. Specified as multiple levels, "
                            + "seperated by a '@', e.g. machine@cluster@site@grid@world."
                            + " Defaults to a single level location with the hostname of the machine" },

                { LOCATION_POSTFIX, null,
                    "Set a string that will be appended to the automatically generated location." },

                { REGISTRY_IMPLEMENTATION, "central",
                    "Nickname or classname of the implementation of the registry. Not all Ibis implementations use this property" },

                { IBIS_STARTERS, null, "List of ibis starter classes in case they cannot be read from jar manifests" },
            };

    private static Properties defaultProperties;

    /**
     * Private constructor, to prevent construction of an IbisProperties object.
     */
    private IbisProperties() {
        // nothing
    }

    /**
     * Returns the hard-coded properties of Ibis.
     * 
     * @return
     *          the resulting properties.
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
     * @return
     *          the name/description map.
     */
    public static Map<String, String> getDescriptions() {
        Map<String, String> result = new LinkedHashMap<String, String>();

        for (String[] element : propertiesList) {
            result.put(element[0], element[2]);
        }

        return result;
    }

    /**
     * Adds the properties as loaded from the specified stream to the specified
     * properties.
     * 
     * @param inputStream
     *            the input stream.
     * @param properties
     *            the properties.
     */
    private static void load(InputStream inputStream, Properties properties) {
        if (inputStream != null) {
            try {
                properties.load(inputStream);
            } catch (IOException e) {
                // ignored
            } finally {
                try {
                    inputStream.close();
                } catch (Throwable e1) {
                    // ignored
                }
            }
        }
    }

    /**
     * Loads properties from the standard configuration file locations.
     */
    @SuppressWarnings("unchecked")
	public static synchronized Properties getDefaultProperties() {
        if (defaultProperties == null) {
            defaultProperties = getHardcodedProperties();

            // Load properties from the classpath
            ClassLoader classLoader = ClassLoader.getSystemClassLoader();
            InputStream inputStream =
                classLoader.getResourceAsStream(PROPERTIES_FILENAME);
            load(inputStream, defaultProperties);

            // See if there is an ibis.properties file in the current
            // directory.
            try {
                inputStream =
                    new FileInputStream(PROPERTIES_FILENAME);
                load(inputStream, defaultProperties);
            } catch (FileNotFoundException e) {
                // ignored
            }

            Properties systemProperties = System.getProperties();

            // Then see if the user specified an properties file.
            String file =
                systemProperties.getProperty(PROPERTIES_FILE);
            if (file != null) {
                try {
                    inputStream = new FileInputStream(file);
                    load(inputStream, defaultProperties);
                } catch (FileNotFoundException e) {
                    System.err.println("User specified preferences \"" + file
                            + "\" not found!");
                }
            }

            // Finally, add the properties from the command line to the result,
            // possibly overriding entries from file or the defaults.
            for (Enumeration<String> e = (Enumeration<String>)systemProperties.propertyNames(); e.hasMoreElements();) {
                String key = e.nextElement();
                String value = systemProperties.getProperty(key);
                defaultProperties.setProperty(key, value);
            }
        }

        return new Properties(defaultProperties);
    }

}
