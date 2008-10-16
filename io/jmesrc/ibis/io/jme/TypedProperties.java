/* $Id: TypedProperties.java 6348M 2007-10-15 09:47:58Z (local) $ */

package ibis.io.jme;

import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Utility to extract and check typed properties.
 */
public class TypedProperties extends Properties {

    private static final long serialVersionUID = 1L;

    /** Constructs an empty typed properties object. */
    public TypedProperties() {
        super();
    }

    /**
     * Constructs a typed properties object with the specified defaults.
     * 
     * @param defaults
     *            the defaults.
     */
    public TypedProperties(Properties defaults) {
        super(defaults);
    }

    public void putAll(Properties properties) {
        addProperties(properties);
    }

    /**
     * Tries to load properties from a properties file on the classpath
     * TODO: We need load in properties

    public void loadFromClassPath(String resourceName) {
        InputStream inputStream = Class.getResourceAsStream(resourceName);

        if (inputStream != null) {
            try {
                load(inputStream);
            } catch (Exception e) {
                // IGNORE
            } finally {
                try {
                    inputStream.close();
                } catch (Exception e2) {
                    // IGNORE
                }
            }
        }
    }
         */

    /**
     * Tries to load properties from a file. Does not throw any exceptions if
     * unsuccesfull
	 * TODO: We need load in properties for this
    public void loadFromFile(String file) {
        if (file == null) {
            return;
        }

        try {
            FileInputStream inputStream = new FileInputStream(file);

            try {
                load(inputStream);
            } catch (IOException e) {
                // IGNORE
            }

            try {
                inputStream.close();
            } catch (IOException e) {
                // IGNORE
            }

        } catch (FileNotFoundException e) {
            // IGNORE
        }
    }

    public void loadFromHomeFile(String fileName) {
        loadFromFile(System.getProperty("user.home") + File.separator
                + fileName);
    }
         */

    /**
     * Returns true if property <code>name</code> is defined and has a value
     * that is conventionally associated with 'true' (as in Ant): any of 1, on,
     * true, yes, or nothing.
     * 
     * @return true if property is defined and set
     * @param name
     *            property name
     */
    public boolean getBooleanProperty(String name) {
        return getBooleanProperty(name, false);
    }

    /**
     * Returns true if property <code>name</code> has a value that is
     * conventionally associated with 'true' (as in Ant): any of 1, on, true,
     * yes, or nothing. If the property is not defined, return the specified
     * default value.
     * 
     * @return true if property is defined and set
     * @param key
     *            property name
     * @param defaultValue
     *            the value that is returned if the property is absent
     */
    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = getProperty(key);

        if (value != null) {
            return value.equals("1") || value.equals("on") || value.equals("")
                    || value.equals("true") || value.equals("yes");
        }

        return defaultValue;
    }

    /**
     * Returns the integer value of property.
     * 
     * @return the integer value of property
     * @param key
     *            property name
     * @throws NumberFormatException
     *             if the property is undefined or not an integer
     */
    public int getIntProperty(String key) {
        String value = getProperty(key);

        if (value == null) {
            throw new NumberFormatException("property undefined: " + key);
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Integer expected for property "
                    + key + ", not \"" + value + "\"");
        }
    }

    /**
     * Returns the integer value of property.
     * 
     * @return the integer value of property
     * @param key
     *            property name
     * @param defaultValue
     *            default value if the property is undefined
     * @throws NumberFormatException
     *             if the property defined and not an integer
     */
    public int getIntProperty(String key, int defaultValue) {
        String value = getProperty(key);

        if (value == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Integer expected for property "
                    + key + ", not \"" + value + "\"");
        }
    }

    /**
     * Returns the long value of property.
     * 
     * @return the long value of property
     * @param key
     *            property name
     * @throws NumberFormatException
     *             if the property is undefined or not an long
     */
    public long getLongProperty(String key) {
        String value = getProperty(key);

        if (value == null) {
            throw new NumberFormatException("property undefined: " + key);
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Long expected for property " + key
                    + ", not \"" + value + "\"");
        }
    }

    /**
     * Returns the long value of property.
     * 
     * @return the long value of property
     * @param key
     *            property name
     * @param defaultValue
     *            default value if the property is undefined
     * @throws NumberFormatException
     *             if the property defined and not an Long
     */
    public long getLongProperty(String key, long defaultValue) {
        String value = getProperty(key);

        if (value == null) {
            return defaultValue;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Long expected for property " + key
                    + ", not \"" + value + "\"");
        }
    }

    /**
     * Returns the short value of property.
     * 
     * @return the short value of property
     * @param key
     *            property name
     * @throws NumberFormatException
     *             if the property is undefined or not an short
     */
    public short getShortProperty(String key) {
        String value = getProperty(key);

        if (value == null) {
            throw new NumberFormatException("property undefined: " + key);
        }

        try {
            return Short.parseShort(value);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Short expected for property "
                    + key + ", not \"" + value + "\"");
        }
    }

    /**
     * Returns the short value of property.
     * 
     * @return the short value of property
     * @param key
     *            property name
     * @param defaultValue
     *            default value if the property is undefined
     * @throws NumberFormatException
     *             if the property defined and not an Short
     */
    public short getShortProperty(String key, short defaultValue) {
        String value = getProperty(key);

        if (value == null) {
            return defaultValue;
        }

        try {
            return Short.parseShort(value);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Short expected for property "
                    + key + ", not \"" + value + "\"");
        }
    }

    /**
     * Returns the long value of a size property. Valid values for the property
     * are a long, a long followed by K, a long followed by M or a long followed
     * by G. Size modifiers multiply the value by 1024, 1024^2 and 1024^3
     * respectively.
     * 
     * @return the size value of property
     * @param key
     *            property name
     * @throws NumberFormatException
     *             if the property is undefined or not a valid size
     */
    public long getSizeProperty(String key) {
        String value = getProperty(key);

        if (value == null) {
            throw new NumberFormatException("property undefined: " + key);
        }

        return getSizeProperty(key, 0);
    }

    /**
     * Returns the long value of a size property. Valid values for the property
     * are a long, a long followed by K, a long followed by M or a long followed
     * by G. Size modifiers multiply the value by 1024, 1024^2 and 1024^3
     * respectively. Returns the default value if the property is undefined.
     * 
     * @return the size value of property
     * @param key
     *            property name
     * @param defaultValue
     *            the default value
     * @throws NumberFormatException
     *             if the property is not a valid size
     */
    public long getSizeProperty(String key, long defaultValue) {
        String value = getProperty(key);

        if (value == null) {
            return defaultValue;
        }

        try {

            if (value.endsWith("G") || value.endsWith("g")) {
                return Long.parseLong(value.substring(0, value.length() - 1)) * 1024 * 1024 * 1024;
            }

            if (value.endsWith("M") || value.endsWith("m")) {
                return Long.parseLong(value.substring(0, value.length() - 1)) * 1024 * 1024;
            }

            if (value.endsWith("K") || value.endsWith("k")) {
                return Long.parseLong(value.substring(0, value.length() - 1)) * 1024;
            }

            return Long.parseLong(value);

        } catch (NumberFormatException e) {
            throw new NumberFormatException(
                    "Long[G|g|M|m|K|k] expected for property " + key
                            + ", not \"" + value + "\"");
        }
    }

    /**
     * Returns the split-up value of a string property. The value is supposed to
     * be a comma-separated string. See {@link java.lang.String#split(String)}
     * for details of the splitting. If the property is not defined, an empty
     * array of strings is returned.
     * 
     * @param key
     *            the property name
     * @return the split-up property value.

    public String[] getStringList(String key) {
        return getStringList(key, ",", new String[0]);
    }
     */
    /**
     * Returns the split-up value of a string property. The value is split up
     * according to the specified delimiter. See
     * {@link java.lang.String#split(String)} for details of the splitting. If
     * the property is not defined, an empty array of strings is returned.
     * 
     * @param key
     *            the property name
     * @param delim
     *            the delimiter
     * @return the split-up property value.

    public String[] getStringList(String key, String delim) {
        return getStringList(key, delim, new String[0]);
    }
     */
    
    /**
     * Returns the split-up value of a string property. The value is split up
     * according to the specified delimiter. See
     * {@link java.lang.String#split(String)} for details of the splitting. If
     * the property is not defined, the specified default value is returned.
     * 
     * @param key
     *            the property name
     * @param delim
     *            the delimiter
     * @param defaultValue
     *            the default value
     * @return the split-up property value.

    public String[] getStringList(String key, String delim,
            String[] defaultValue) {
        String value = getProperty(key);

        if (value == null) {
            return defaultValue;
        }

        return value.split(delim);
    }
     */
    
    /**
     * Returns true if property name is defined and has a string value that
     * equals match.
     * 
     * @return true if property is defined and equals match
     * @param key
     *            property name
     * @param match
     *            value to be matched
     */
    public boolean stringPropertyMatch(String key, String match) {
        String value = getProperty(key);
        return value != null && value.equals(match);
    }

    /**
     * Returns true if the given element is a member of the given list.
     * 
     * @param list
     *            the given list.
     * @param element
     *            the given element.
     * @return true if the given element is a member of the given list.
     */
    private static boolean contains(Vector list, String element) {
        if (list == null) {
            return false;
        }
        for (int i = 0; i < list.size(); i++) {
            if (element.equalsIgnoreCase((String)list.elementAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the given string starts with one of the given prefixes.
     * 
     * @param string
     *            the given string.
     * @param prefixes
     *            the given prefixes.
     * @return true if the given string starts with one of the given prefixes.
     */
    private static boolean startsWith(String string, Vector prefixes) {
        if (prefixes == null) {
            return false;
        }
        for (int i = 0; i < prefixes.size(); i++) {
            if (string.startsWith((String)prefixes.elementAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks all properties with the given prefix for validity.
     * 
     * @return a Property object containing all unrecognized properties.
     * @param prefix
     *            the prefix that should be checked
     * @param validKeys
     *            the set of valid keys (all with the prefix).
     * @param validSubPrefixes
     *            if a propery starts with one of these prefixes, it is declared
     *            valid
     * @param printWarning
     *            if true, a warning is printed to standard error for each
     *            unknown property
     */
    public TypedProperties checkProperties(String prefix, Vector validKeys,
            Vector validSubPrefixes, boolean printWarning) {
        TypedProperties result = new TypedProperties();

        if (prefix == null) {
            prefix = "";
        }

        for (Enumeration e = propertyNames(); e.hasMoreElements();) {
            String key = (String) e.nextElement();

            if (key.startsWith(prefix)) {
                String suffix = key.substring(prefix.length());
                String value = getProperty(key);

                if (!startsWith(suffix, validSubPrefixes)
                        && !contains(validKeys, key)) {
                    if (printWarning) {
                        System.err.println("Warning, unknown property: " + key
                                + " with value: " + value);
                    }
                    result.put(key, value);
                }
            }
        }
        return result;
    }

    /**
     * Returns all properties who's key start with a certain prefix.
     * 
     * @return a Property object containing all matching properties.
     * @param prefix
     *            the desired prefix
     * @param removePrefix
     *            should the prefix be removed from the property name?
     * @param removeProperties
     *            should the returned properties be removed from the current
     *            properties?
     */
    public TypedProperties filter(String prefix, boolean removePrefix,
            boolean removeProperties) {

        TypedProperties result = new TypedProperties();

        if (prefix == null) {
            prefix = "";
        }

        for (Enumeration e = propertyNames(); e.hasMoreElements();) {
            String key = (String) e.nextElement();

            if (key.startsWith(prefix)) {

                String value = getProperty(key);

                if (removePrefix) {
                    result.put(key.substring(prefix.length()), value);
                } else {
                    result.put(key, value);
                }

                if (removeProperties) {
                    remove(key);
                }
            }
        }

        return result;
    }

    /**
     * Returns all properties who's key start with a certain prefix.
     * 
     * @return a Property object containing all matching properties.
     * @param prefix
     *            the desired prefix
     */
    public TypedProperties filter(String prefix) {
        return filter(prefix, false, false);
    }

    /**
     * Prints properties (including default properties) to a stream.
     * 
     * @param out
     *            The stream to write output to.
     * @param prefix
     *            Only print properties which start with the given prefix. If
     *            null, will print all properties
     */
    public void printProperties(PrintStream out, String prefix) {
        if (prefix == null) {
            prefix = "";
        }

        for (Enumeration e = propertyNames(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            String value = getProperty(key);

            if (key.toLowerCase().startsWith(prefix.toLowerCase())) {
                out.println(key + " = " + value);
            }
        }
    }

    public String toString() {
        String result = "";

        for (Enumeration e = propertyNames(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            String value = getProperty(key);

            result += key + " = " + value + "\n";
        }

        return result;
    }

    public boolean equals(Object object) {
        if (!(object instanceof TypedProperties)) {
            return false;
        }

        TypedProperties other = (TypedProperties) object;

        for (Enumeration e = propertyNames(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            String value = getProperty(key);

            String otherValue = other.getProperty(key);

            if (otherValue == null || !otherValue.equals(value)) {
                return false;
            }
        }
        return true;
    }
}
