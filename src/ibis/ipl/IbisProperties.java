package ibis.ipl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Utility methods for setting and getting configuration properties. The
 * {@link #getConfigurationProperties()} method obtains the properties in the following
 * order: first, some hardcoded properties are set. Next, a file
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
    private static final String PROPERTIES_FILENAME = "ibis.properties";

    /** All our own properties start with this prefix. */
    public static final String PREFIX = "ibis.";

    /** Property name for selecting an Ibis. */
    public static final String NAME = PREFIX + "name";

    /** Property name of the property file. */
    public static final String PROPERTIES_FILE = PREFIX + "properties.file";

    /** Property name for the native library path. */
    public static final String LIBRARY_PATH = PREFIX + "library.path";

    /** Property name for the path used to find Ibis implementations. */
    public static final String IMPL_PATH = PREFIX + "impl.path";

    /** Property name for verbosity. */
    public static final String VERBOSE = PREFIX + "verbose";

    /** List of {NAME, DESCRIPTION, DEFAULT_VALUE} for properties. */
    private static final String[][] propertiesList = new String[][] {
            { NAME, null, 
                    "Nickname or classname of the Ibis implementation" },

            { PROPERTIES_FILE, null,
                    "Name of the property file used for the configuration of Ibis" },

            { LIBRARY_PATH, null, "Native library path" },

            { IMPL_PATH, null, "Path used to find Ibis implementations", },

            { VERBOSE, "false",
                    "Boolean: If true, makes Ibis more verbose, if false, does not" },

    };

    /**
     * Private constructor, to prevent construction of an IbisProperties object.
     */
    private IbisProperties() {
        // nothing
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
     * Returns properties set in configuration files and system properties.
     * 
     * @return the properties.
     */
    public static Properties getConfigurationProperties() {
        return getConfigurationProperties(null);
    }

    /**
     * Returns the properties set in configuration files and system properties,
     * overriding the specified defaults.
     * 
     * @param defaults
     *            the default properties.
     * @return the resulting properties.
     */
    public static Properties getConfigurationProperties(Properties defaults) {
        InputStream inputStream = null;

        Properties configurationProperties = new Properties(defaults);
        // Get the properties from the commandline.
        Properties systemProperties = System.getProperties();

        // Then get the default properties from the classpath:
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        inputStream = classLoader.getResourceAsStream(PROPERTIES_FILENAME);

        load(inputStream, configurationProperties);

        // Then see if there is an ibis.properties file in the users
        // home directory.
        String fileName = systemProperties.getProperty("user.home")
                + systemProperties.getProperty("file.separator") + PROPERTIES_FILENAME;
        try {
            inputStream = new FileInputStream(fileName);
            load(inputStream, configurationProperties);
        } catch (FileNotFoundException e) {
            // ignored
        }

        // Then see if there is an ibis.properties file in the current
        // directory.
        try {
            inputStream = new FileInputStream(PROPERTIES_FILENAME);
            load(inputStream, configurationProperties);
        } catch (FileNotFoundException e) {
            // ignored
        }

        // Then see if the user specified an properties file.
        String file = systemProperties.getProperty(PROPERTIES_FILE);
        if (file != null) {
            try {
                inputStream = new FileInputStream(file);
                load(inputStream, configurationProperties);
            } catch (FileNotFoundException e) {
                System.err.println("User specified preferences \"" + file
                        + "\" not found!");
            }
        }

        // Finally, add the properties from the command line to the result,
        // possibly overriding entries from file or the defaults.
        for (Enumeration e = systemProperties.propertyNames(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            String value = systemProperties.getProperty(key);
            configurationProperties.setProperty(key, value);
        }

        return configurationProperties;
    }

    /**
     * Returns the built-in properties of Ibis.
     * 
     * @return the resulting properties.
     */
    public static Properties getHardcodedProperties() {
        return getHardcodedProperties(null);
    }

    /**
     * Returns the properties that are the result of having the hardcoded
     * properties override the specified properties.
     * 
     * @param defaultProperties
     *            the default properties
     * @return the resulting properties.
     */
    public static Properties getHardcodedProperties(Properties defaultProperties) {
        Properties properties = new Properties(defaultProperties);

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
