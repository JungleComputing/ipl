package ibis.util;

import java.util.Enumeration;
import java.util.Properties;

/**
 * Utility to extract some typed system properties.
 */
public class TypedProperties {

    /** Add property prefixes here ... */
    private static String[] prefs = {
	"ibis.util.ip.",
	"ibis.util.monitor.",
	"ibis.util.socketfactory."
    };

    static {
	checkProperties("ibis.util.", null, prefs);
    }

    /**
     * Prevent construction ...
     */
    private TypedProperties() {
    	/* do nothing */
    }

    /**
     * Returns true if property <code>name</code> is defined and has a value
     * that is conventionally associated with 'true' (as in Ant): any of
     * 1, on, true, yes.
     *
     * @return true if property is defined and set
     * @param name property name
     */
    public static boolean booleanProperty(String name) {
	return booleanProperty(name, false);
    }

    /**
     * Returns true if property <code>name</code> has a value that is
     * conventionally associated with 'true' (as in Ant): any of
     * 1, on, true, yes. If the property is not defined, return the specified
     * default value.
     *
     * @return true if property is defined and set
     * @param name property name
     * @param defaultVal the value that is returned if the property is absent
     */
    public static boolean booleanProperty(String name, boolean defaultVal) {
	String prop = System.getProperty(name);

	if (prop == null) {
	    return defaultVal;
	}

	return prop.equals("1")
	    || prop.equals("on")
	    || prop.equals("")
	    || prop.equals("true")
	    || prop.equals("yes");
    }

    /**
     * Returns the integer value of property
     *
     * @return the integer value of property
     * @param name property name
     * @throws NumberFormatException if the property is undefined or not an
     * 		integer
     */
    public static int intProperty(String name) {
	String prop = System.getProperty(name);
	if (prop == null) {
	    throw new NumberFormatException("Property " + name + " undefined");
	}
	return Integer.parseInt(prop);
    }

    /**
     * Returns the integer value of property
     *
     * @return the integer value of property
     * @param name property name
     * @param dflt default value if the property is undefined
     * @throws NumberFormatException if the property defined and not an
     * 		integer
     */
    public static int intProperty(String name, int dflt) {
	String prop = System.getProperty(name);
	if (prop == null) {
	    return dflt;
	}
	return Integer.parseInt(prop);
    }

    /**
     * Returns the long value of property
     *
     * @return the long value of property
     * @param name property name
     * @throws NumberFormatException if the property is undefined or not a
     * 		long
     */
    public static long longProperty(String name) {
	String prop = System.getProperty(name);
	if (prop == null) {
	    throw new NumberFormatException("Property " + name + " undefined");
	}
	return Long.parseLong(prop);
    }

    /**
     * Returns the long value of property
     *
     * @return the long value of property
     * @param name property name
     * @param dflt default value if the property is undefined
     * @throws NumberFormatException if the property defined and not a
     * 		long
     */
    public static long longProperty(String name, long dflt) {
	String prop = System.getProperty(name);
	if (prop == null) {
	    return dflt;
	}
	return Long.parseLong(prop);
    }

    /**
     * Returns true if property name is defined and has a string value
     * that equals match.
     *
     * @return true if property is defined and equals match
     * @param name property name
     * @param match value to be matched
     */
    public static boolean stringPropertyMatch(String name, String match) {
	String prop = System.getProperty(name);
	return prop != null && prop.equals(match);
    }

    /**
     * Get value of property
     *
     * @return the value of the property or <code>null</code> if it is not set
     * @param name property name
     */
    public static String stringProperty(String name) {
	return System.getProperty(name);
    }

    /**
     * Get value of property
     *
     * @return the value of the property or <code>null</code> if it is not set
     * @param name property name
     */
    public static String stringProperty(String name, String dflt) {
	String prop = System.getProperty(name);
	if (prop == null) {
	    return dflt;
	}
	return prop;
    }

    /**
     * Check validity of a System property.
     * All system properties are checked; when the name starts with the
     * specified prefix, it should be in the specified list of property names,
     * unless it starts with one of the exclude members.
     * If the property is not found, a warning is printed.
     *
     * @param prefix prefix of checked property names, for instance "satin.".
     * @param propnames list of accepted property names.
     * @param excludes list of property prefixes that should not be checked.
     */
    public static void checkProperties(String prefix, String[] propnames, String[] excludes) {
	Properties p = System.getProperties();
	for (Enumeration e = p.propertyNames(); e.hasMoreElements();) {
	    String name = (String) e.nextElement();
	    if (name.startsWith(prefix)) {
		boolean found = false;
		if (excludes != null) {
		    for (int i = 0; i < excludes.length; i++) {
			if (name.startsWith(excludes[i])) {
			    found = true;
			    break;
			}
		    }
		}
		if (! found) {
		    if (propnames != null) {
			for (int i = 0; i < propnames.length; i++) {
			    if (name.equals(propnames[i])) {
				found = true;
				break;
			    }
			}
		    }
		}
		if (! found) {
		    System.err.println("Warning: property \"" + name + "\" has prefix \"" + prefix + "\" but is not recognized");
		}
	    }
	}
    }
}
