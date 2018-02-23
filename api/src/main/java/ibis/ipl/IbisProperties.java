/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ibis.ipl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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

    /** Property name for setting the address of the IPL-server. */
    public static final String SERVER_ADDRESS = PREFIX + "server.address";

    /** Property name for setting indicating if the server is also a SmartSockets hub. */
    public static final String SERVER_IS_HUB = PREFIX + "server.is.hub";
    
    /** Property name for specifying a comma separated list of hubs. */
    public static final String HUB_ADDRESSES = PREFIX + "hub.addresses";

    /** Property name for specifying the implementation of the registry. */
    public static final String REGISTRY_IMPLEMENTATION = PREFIX + "registry.implementation";
    
    /** Property name for location. */
    public static final String LOCATION = PREFIX + "location";
    
    /** Property name for location color. */
    public static final String LOCATION_COLOR = PREFIX + "location.color";

    /** List of {NAME, DESCRIPTION, DEFAULT_VALUE} for properties. */
    private static final String[][] propertiesList =
            new String[][] {
                { POOL_NAME, null, "String: name of the pool this ibis belongs to" },
                { POOL_SIZE, null,
                    "Integer: size of the pool this ibis belongs to" },
                { SERVER_ADDRESS, null, "Address of the central ibis server" },
                { SERVER_IS_HUB, "true", "Boolean: if true, the server is also used as a SmartSockets hub" },
                { HUB_ADDRESSES, null,
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
                            + " Defaults to a multi level location based on the FQDN of the machine" },
                    
                { LOCATION_COLOR, null,
                        "Color code (in html color notation e.g #545432) for this ibis."},

                { REGISTRY_IMPLEMENTATION, "central",
                    "Nickname or classname of the implementation of the registry. Not all Ibis implementations use this property" },
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
     * @return properties loaded from the standard configuration file locations.
     * 
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
