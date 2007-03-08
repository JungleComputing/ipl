package ibis.ipl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class IbisProperties {

    private static final String PROPERTIES_FILENAME = "ibis.properties";

    /** All our own properties start with this prefix. */
    public static final String PREFIX = "ibis.";

    /** Property name of the property file. */
    public static final String PROPERTIES_FILE = PREFIX + "properties.file";

    /** Property name for the native library path. */
    public static final String LIBRARY_PATH = PREFIX + "library.path";

    /** Property name for the path used to find Ibis implementations. */
    public static final String IMPL_PATH = PREFIX + "impl.path";

    public static final String VERBOSE = PREFIX + "verbose";

    // list of {NAME, DESCRIPTION, DEFAULT_VALUE} for properties
    private static final String[][] propertiesList = new String[][] {
            { PROPERTIES_FILE, null,
                    "Name of the property file used for the configuration of Ibis" },

            { LIBRARY_PATH, null, "Native library path" },

            { IMPL_PATH, null, "Path used to find Ibis implementations", },

            { VERBOSE, "false",
                    "Boolean: If true, makes Ibis more verbose, if false, does not" },

    };

    private static void load(InputStream in, Properties properties) {
        if (in != null) {
            try {
                properties.load(in);
            } catch (IOException e) {
                // ignored
            } finally {
                try {
                    in.close();
                } catch (Throwable e1) {
                    // ignored
                }
            }
        }
    }

    public static Properties getConfigProperties() {
        return getConfigProperties(null);
    }

    /**
     * Returns properties set in config files and system properties
     */
    public static Properties getConfigProperties(Properties defaults) {
        InputStream in = null;

        Properties configProperties = new Properties(defaults);
        // Get the properties from the commandline.
        Properties system = System.getProperties();

        // Then get the default properties from the classpath:
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        in = loader.getResourceAsStream(PROPERTIES_FILENAME);

        load(in, configProperties);

        // Then see if there is an ibis.properties file in the users
        // home directory.
        String fn = system.getProperty("user.home")
                + system.getProperty("file.separator") + PROPERTIES_FILENAME;
        try {
            in = new FileInputStream(fn);
            load(in, configProperties);
        } catch (FileNotFoundException e) {
            // ignored
        }

        // Then see if there is an ibis.properties file in the current
        // directory.
        try {
            in = new FileInputStream(PROPERTIES_FILENAME);
            load(in, configProperties);
        } catch (FileNotFoundException e) {
            // ignored
        }

        // Then see if the user specified an properties file.
        String file = system.getProperty(PROPERTIES_FILE);
        if (file != null) {
            try {
                in = new FileInputStream(file);
                load(in, configProperties);
            } catch (FileNotFoundException e) {
                System.err.println("User specified preferences \"" + file
                        + "\" not found!");
            }
        }

        // Finally, add the properties from the command line to the result,
        // possibly overriding entries from file or the defaults.
        for (Enumeration e = system.propertyNames(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            String value = system.getProperty(key);
            configProperties.setProperty(key, value);
        }

        return configProperties;
    }

    public static Properties getHardcodedProperties() {
        return getHardcodedProperties(null);
    }

    public static Properties getHardcodedProperties(Properties defaults) {
        Properties properties = new Properties(defaults);

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
