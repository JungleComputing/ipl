package ibis.util;

public class TypedProperties {

    /**
     * Returns true if property name is defined and has a value that is
     * conventionally associated with 'true' (as in Ant): 1, on, true.
     *
     * @return true if property is defined and set
     * @param name property name
     */
    public static boolean booleanProperty(String name) {
	String prop = System.getProperty(name);
	
	if (prop == null) {
	    return false;
	}

	return prop.equals("1") || prop.equals("on") || prop.equals("true");
    }

    /**
     * Returns true if property name is defined and has an integer value
     *
     * @return true if property is defined and integer
     * @param name property name
     */
    public static boolean intProperty(String name) {
	String prop = System.getProperty(name);
	
	if (prop == null) {
	    return false;
	}

	try {
	    int x = Integer.parseInt(prop);
	    return true;
	} catch (NumberFormatException e) {
	    return false;
	}
    }

    /**
     * Returns the integer value of property
     *
     * @return the integer value of property
     * @param name property name
     * @throws NumberFormatException if the property is undefined or not an
     * 		integer
     */
    public static int intPropertyValue(String name) {
	String prop = System.getProperty(name);
	if (prop == null) {
	    throw new NumberFormatException("Property " + name + " undefined");
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
