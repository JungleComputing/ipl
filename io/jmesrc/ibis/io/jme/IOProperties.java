/* $Id$ */

package ibis.io.jme;

import java.util.Hashtable;
import java.util.Vector;

// Log4JME doesn't actually work on real devices so commenting out for now.

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

/**
 * Collects all system properties used by the ibis.io package.
 */
public class IOProperties implements Constants {
    static final TypedProperties properties;

    static final String PREFIX = "ibis.io.";

    /** Filename for the properties. */
    private static final String PROPERTIES_FILENAME = "ibis.properties";

    /** Property name of the property file. */
    public static final String PROPERTIES_FILE = PREFIX + "properties.file";
    
    static final Logger logger = new Logger();
    
    /*
    static final Logger logger;
    
	static {
		logger = Logger.getInstance("ibis.io.jme");
	    PatternLayout layout = new PatternLayout();
	    ConsoleAppender appender = new ConsoleAppender(layout);

		logger.addAppender(appender);
		
		if (IOProperties.DEBUG) {
			logger.setPriority(Priority.DEBUG);
			logger.debug("Logger Initialized.");
		}
	}
     */

    static final String s_stats_nonrewritten = PREFIX
            + "stats.nonrewritten";

    static final String s_stats_written = PREFIX + "stats.written";

    static final String s_classloader = PREFIX
            + "serialization.classloader";

    static final String s_timer = PREFIX + "serialization.timer";

    static final String s_no_array_buffers = PREFIX + "noarraybuffers";

    static final String s_conversion = PREFIX + "conversion";

    static final String s_buffer_size = PREFIX + "buffer.size";

    static final String s_array_buffer = PREFIX + "array.buffer";

    static final String s_debug = PREFIX + "debug";

    static final String s_asserts = PREFIX + "assert";

    static final String s_small_array_bound = PREFIX
            + "smallarraybound";

    static final String s_hash_asserts = PREFIX + "hash.assert";

    static final String s_hash_stats = PREFIX + "hash.stats";

    static final String s_hash_timings = PREFIX + "hash.timings";

    static final String s_hash_resize = PREFIX + "hash.resize";

    static final String s_deepcopy_ser = PREFIX + "deepcopy.serialization";

    private static final String[][] propertiesList = 
        new String[][] {
            { PROPERTIES_FILE, PROPERTIES_FILENAME,
                "String: determines the file name of the Ibis IO properties "
                    + "file"},
            { s_stats_nonrewritten, "false",
                "Boolean: if true, print non-rewritten object statistics"},
            { s_stats_written, "false",
                "Boolean: if true, print statistics about objects written"},
            { s_classloader, null,
                "String: the name of a classloader to be used when a class "
                    + "cannot be found"},
            { s_timer, "false",
                "Boolean: if true, enables various serialization timers"},
            { s_no_array_buffers, "false",
                "Boolean: if true, leaves all buffering of Ibis serialization "
                    + "to the layers below it" },
            { s_conversion, "hybrid",
                "String: determines the conversion used" },
            { s_buffer_size, "4096",
                "Integer: determines the size of the buffers used in Ibis "
                    + "serialization"},
            { s_array_buffer, "32",
                "Integer: determines the size of the buffer for arrays"},
            { s_debug, "false",
                "Boolean: if true, enables log4j calls"},
            { s_asserts, "false",
                "Boolean: if true, enables some assertions"},
            { s_small_array_bound, "256",
                "Integer: determines the bound beyond which arrays of a "
                    + "basic type are written as an array instead of as "
                    + "individual elements"},
            { s_hash_asserts, "false",
                "Boolean: if true, enables some assertions in the ibis hash"},
            { s_hash_stats, "false",
                "Boolean: if true, enables statistics in the ibis hash"},
            { s_hash_timings, "false",
                "Boolean: if true, enables various timers in the ibis hash"},
            { s_hash_resize, "100",
                "Integer: determines the fill-percentage before the ibis hash "
                    + " is resized; choose between 50 and 200; larger values "
                    + " mean more chaining but a smaller hash size"},
            { s_deepcopy_ser, "ibis",
                "String: determines the serialization used for DeepCopy"}
        };

    static {
        properties = new TypedProperties(getDefaultProperties());
        properties.checkProperties(PREFIX,
        		getPropertyNames(), null, true);
    }

    static final boolean DEBUG = properties.getBooleanProperty(s_debug, false);

    public static final boolean ASSERTS = properties.getBooleanProperty(s_asserts, false);

    public static final int SMALL_ARRAY_BOUND
            = properties.getIntProperty(s_small_array_bound, 256); // byte

    public static final int BUFFER_SIZE = properties.getIntProperty(
            s_buffer_size, 4 * 1024);

    public static final int ARRAY_BUFFER_SIZE
            = properties.getIntProperty(s_array_buffer, 32);

    /**
     * Returns the hard-coded Ibis IO properties.
     * 
     * @return
     *          the resulting properties.
     */
    public static Properties getHardcodedProperties() {
        Properties properties = new Properties();

        for (int i = 0; i < propertiesList.length; i++) {
        	String[] element = propertiesList[i];
            if (element[1] != null) {
                properties.setProperty(element[0], element[1]);
            }
        }

        return properties;
    }

    /**
     * Returns a Hashtable mapping hard-coded property names to their descriptions.
     * 
     * @return
     *          the name/description Hashtable.
     */
    public static Hashtable getDescriptions() {
        Hashtable result = new Hashtable();

        for (int i = 0; i < propertiesList.length; i++) {
        	String[] element = propertiesList[i];
            result.put(element[0], element[2]);
        }

        return result;
    }

    /**
     * Returns a Vector of recognized properties.
     */
    public static Vector getPropertyNames() {
        Vector result = new Vector();
        for (int i = 0; i < propertiesList.length; i++) {
        	String[] element = propertiesList[i];
            result.addElement(element[0]);
        }
        return result;
    }
/* TODO: Not needed?
    private static void addProperties(Properties props, Properties p) {
        for (Enumeration e = p.propertyNames(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            String value = p.getProperty(key);
            props.setProperty(key, value);
        }
    }
*/

    private static Properties getDefaultProperties() {

        Properties props = new Properties();
        
        /* TODO: we should load default properties from
         * some form of long term storage or from some
         * resource in the classpath or something like that.
         */

        return props;
    }
}
