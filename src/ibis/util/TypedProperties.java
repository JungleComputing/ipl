package ibis.util;

/**
 * Utility to extract some typed system properties.
 */
public class TypedProperties {

    /**
     * Prevent construction ...
     */
    private TypedProperties() {
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
     * Returns true if property name is defined and has a string value
     * that equals match.
     *
     * @return true if property is defined and equals match
     * @param name property name
     * @param match value to be matched
     */
    public static boolean stringProperty(String name, String match) {
	String prop = System.getProperty(name);
	return prop != null && prop.equals(match);
    }

    /**
     * Get value of property
     *
     * @return the value of the property or <code>null</code> if it is not set
     * @param name property name
     */
    public static String stringPropertyValue(String name) {
	return System.getProperty(name);
    }

}
