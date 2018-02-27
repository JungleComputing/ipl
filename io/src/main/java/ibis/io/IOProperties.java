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
/* $Id$ */

package ibis.io;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import ibis.util.TypedProperties;

/**
 * Collects all system properties used by the ibis.io package.
 */
public final class IOProperties {
    static final TypedProperties properties;

    static final String PREFIX = "ibis.io.";

    /** Filename for the properties. */
    private static final String PROPERTIES_FILENAME = "ibis.properties";

    /** Property name of the property file. */
    public static final String PROPERTIES_FILE = PREFIX + "properties.file";

    static final String s_stats_nonrewritten = PREFIX + "stats.nonrewritten";

    static final String s_stats_written = PREFIX + "stats.written";

    static final String s_serialization_default = PREFIX
            + "serialization.object.default";

    static final String s_classloader = PREFIX + "serialization.classloader";

    static final String s_timer_data = PREFIX + "serialization.timer.data";

    static final String s_timer_ibis = PREFIX + "serialization.timer.ibis";

    static final String s_no_array_buffers = PREFIX + "noarraybuffers";

    static final String s_conversion = PREFIX + "conversion";

    static final String s_conversion_buf_size = s_conversion + ".buffer.size";

    static final String s_typed_buffer_size = PREFIX + "buffer.size.typed";

    static final String s_buffer_size = PREFIX + "buffer.size";

    static final String s_array_buffer = PREFIX + "array.buffer";

    static final String s_debug = PREFIX + "debug";

    static final String s_asserts = PREFIX + "assert";

    static final String s_small_array_bound = PREFIX + "smallarraybound";

    static final String s_hash_asserts = PREFIX + "hash.assert";

    static final String s_hash_stats = PREFIX + "hash.stats";

    static final String s_hash_timings = PREFIX + "hash.timings";

    static final String s_hash_resize = PREFIX + "hash.resize";

    static final String s_deepcopy_ser = PREFIX + "deepcopy.serialization";

    private static final String[][] propertiesList = new String[][] {
            { PROPERTIES_FILE, PROPERTIES_FILENAME,
                    "String: determines the file name of the Ibis IO properties "
                            + "file" },
            { s_stats_nonrewritten, "false",
                    "Boolean: if true, print non-rewritten object statistics" },
            { s_stats_written, "false",
                    "Boolean: if true, print statistics about objects written" },
            { s_classloader, null,
                    "String: the name of a classloader to be used when a class "
                            + "cannot be found" },
            { s_timer_data, "false",
                    "Boolean: if true, enables data serialization timers" },
            { s_timer_ibis, "false",
                    "Boolean: if true, enables ibis serialization timers" },
            { s_no_array_buffers, "false",
                    "Boolean: if true, leaves all buffering of Ibis serialization "
                            + "to the layers below it" },
            { s_conversion, "hybrid",
                    "String: determines the conversion used" },
            { s_buffer_size, "8192",
                    "Integer: determines the size of the buffers used in Ibis "
                            + "serialization" },
            { s_typed_buffer_size, "8192",
                    "Integer: determines the size of the typed buffers used "
                            + "in Ibis data serialization streams" },
            { s_conversion_buf_size, "8192",
                    "Integer: determines the size of the conversion buffers "
                            + "used in Ibis serialization" },
            { s_array_buffer, "32",
                    "Integer: determines the size of the buffer for arrays" },
            { s_debug, "false", "Boolean: if true, enables log4j calls" },
            { s_asserts, "false", "Boolean: if true, enables some assertions" },
            { s_small_array_bound, "256",
                    "Integer: determines the bound beyond which arrays of a "
                            + "basic type are written as an array instead of as "
                            + "individual elements" },
            { s_hash_asserts, "false",
                    "Boolean: if true, enables some assertions in the ibis hash" },
            { s_hash_stats, "false",
                    "Boolean: if true, enables statistics in the ibis hash" },
            { s_hash_timings, "false",
                    "Boolean: if true, enables various timers in the ibis hash" },
            { s_hash_resize, "100",
                    "Integer: determines the fill-percentage before the ibis hash "
                            + " is resized; choose between 50 and 200; larger values "
                            + " mean more chaining but a smaller hash size" },
            { s_serialization_default, "ibis",
                    "String: either \"ibis\" or \"sun\", determines the default object serialization" },
            { s_deepcopy_ser, "ibis",
                    "String: determines the serialization used for DeepCopy" } };

    static {
        properties = new TypedProperties(getDefaultProperties());
        properties.checkProperties(PREFIX,
                getPropertyNames().toArray(new String[0]), null, true);
    }

    public static final boolean DEBUG = properties.getBooleanProperty(s_debug,
            false);

    public static final boolean ASSERTS = properties
            .getBooleanProperty(s_asserts, false);

    public static final int SMALL_ARRAY_BOUND = properties
            .getIntProperty(s_small_array_bound, 256); // byte

    public static final int BUFFER_SIZE = properties
            .getIntProperty(s_buffer_size, 8 * 1024);

    public static final int TYPED_BUFFER_SIZE = properties
            .getIntProperty(s_typed_buffer_size, 8 * 1024);

    public static final int ARRAY_BUFFER_SIZE = properties
            .getIntProperty(s_array_buffer, 32);

    public static final int CONVERSION_BUFFER_SIZE = properties
            .getIntProperty(s_conversion_buf_size, 32 * 1024);

    /**
     * Returns the hard-coded Ibis IO properties.
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
        Map<String, String> result = new LinkedHashMap<String, String>();

        for (String[] element : propertiesList) {
            result.put(element[0], element[2]);
        }

        return result;
    }

    /**
     * Returns a list of recognized properties.
     * 
     * @return the list of recognized properties
     */
    public static List<String> getPropertyNames() {
        ArrayList<String> result = new ArrayList<String>();
        for (String[] element : propertiesList) {
            result.add(element[0]);
        }
        return result;
    }

    @SuppressWarnings("resource")
    private static Properties getPropertyFile(String file) {

        InputStream in = null;

        try {
            in = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            ClassLoader loader = ClassLoader.getSystemClassLoader();
            in = loader.getResourceAsStream(file);
            if (in == null) {
                return null;
            }
        }

        try {
            Properties p = new Properties();
            p.load(in);
            return p;
        } catch (IOException e) {
            // ignore
        } finally {
            try {
                in.close();
            } catch (Throwable x) {
                // ignore
            }
        }
        return null;
    }

    private static void addProperties(Properties props, Properties p) {
        for (Enumeration<?> e = p.propertyNames(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            String value = p.getProperty(key);
            props.setProperty(key, value);
        }
    }

    private static Properties getDefaultProperties() {

        Properties props = new Properties();

        // Get the properties from the commandline.
        Properties system = System.getProperties();

        // Check what property file we should load.
        String file = system.getProperty(PROPERTIES_FILE, PROPERTIES_FILENAME);

        // If the file is not explicitly set to null, we try to load it.
        // First try the filename as is, if this fails try with the
        // user home directory prepended.
        if (file != null) {
            Properties fromFile = getPropertyFile(file);
            if (fromFile != null) {
                addProperties(props, fromFile);
            } else {
                if (!file.equals(PROPERTIES_FILENAME)) {
                    // If we fail to load the user specified file,
                    // we give an error, since only the default file
                    // may fail silently.
                    System.err.println("User specified preferences \"" + file
                            + "\" not found!");
                }
            }
        }

        // Finally, add the system properties (also from the command line)
        // to the result, possibly overriding entries from file or the
        // defaults.
        addProperties(props, system);

        return props;
    }
}
