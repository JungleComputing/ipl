package ibis.ipl;

import java.util.Enumeration;
import java.util.Properties;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.ArrayList;

import java.io.InputStream;
import java.io.IOException;

/**
 * Container for the properties of an {@link ibis.ipl.Ibis Ibis} or a
 * {@link ibis.ipl.PortType PortType}.
 *
 * A property consists of two strings: its name (key), and its value,
 * for instance: name: ibis.serialization, value: sun.
 */
public class StaticProperties extends Properties {

    /**
     * HashMap mapping category names to hashmaps that map property
     * names to values.
     */
    private static HashMap categories = new HashMap();

    /**
     * Array containing the category names.
     */
    private static ArrayList category_names = new ArrayList();

    /**
     * User defined properties.
     */
    private static final StaticProperties user_properties;

    /**
     * Default properties.
     */
    private final StaticProperties defaults;

    /**
     * Initialize from the global ibis-properties file, which defines
     * the properties.
     */
    static {
	// First, read the ibis-properties file.
	InputStream in = ClassLoader.getSystemClassLoader().
				getResourceAsStream("ibis-properties");
	if (in == null) {
	    System.err.println("could not open ibis-properties");
	    System.exit(1);
	}
	StaticProperties p = new StaticProperties();
	try {
	    p.load(in);
	    in.close();
	} catch(IOException e) {
	    System.err.println("IO exception during ibis-properties read");
	    e.printStackTrace();
	    System.exit(1);
	}

	// Then, find the property categories.
	String property_categories = p.getProperty("PropertyCategories");
	if (property_categories == null) {
	    System.err.println("no PropertyCategories in ibis-properties!");
	    System.exit(1);
	}
	StringTokenizer st = new StringTokenizer(property_categories,
						 " ,\t\n\r\f");
	while (st.hasMoreTokens()) {
	    // While there are categories, find properties.
	    long propval = 1;

	    String category = st.nextToken().toLowerCase();
	    String values = p.getProperty(category);
	    if (values == null) {
		System.err.println("no property category " + category +
				    " in ibis-properties");
		System.exit(1);
	    }

	    category_names.add(category);

//	    System.out.println("category name = " + category);

	    HashMap v = new HashMap();
	    StringTokenizer st2 = new StringTokenizer(values, " ,\t\n\r\f");
	    while (st2.hasMoreTokens()) {
		// Associate each property with a bit.
		String prop = st2.nextToken().toLowerCase();
		v.put(prop, new Long(propval));
		propval <<= 1;
	    }

	    // now search for implied dependencies
	    st2 = new StringTokenizer(values, " ,\t\n\r\f");
	    while (st2.hasMoreTokens()) {
		String prop = st2.nextToken().toLowerCase();
		String implied = p.getProperty(prop);
//		System.out.print("property = " + prop);
		long val = ((Long) v.get(prop)).longValue();
		if (implied != null) {
		    StringTokenizer st3 = new StringTokenizer(implied,
							      " ,\t\n\r\f");
		    while (st3.hasMoreTokens()) {
			String prop2 = st3.nextToken().toLowerCase();
			Long val2 = (Long) v.get(prop2);
			if (val2 == null) {
			    System.err.println("Property " + prop2 + " not found in implied dependency");
			    System.exit(1);
			}
			val |= val2.longValue();
		    }
		    v.put(prop, new Long(val));
		}
//		System.out.println(", value = " + val);
	    }

	    categories.put(category, v);
	}

	// Now compute user-defined properties.
	// These are derived from system properties with names starting with
	// "ibis.". The "ibis." prefix is stripped, and if the rest of the
	// name is a category name, the property is added to the user
	// properties.

	user_properties = new StaticProperties();
	Properties sysprops = System.getProperties();
	Enumeration e = sysprops.propertyNames();

	while (e.hasMoreElements()) {
	    String name = ((String) e.nextElement());
	    String prop = sysprops.getProperty(name);

	    name = name.toLowerCase();
	    if (name.substring(0,5).equals("ibis.")) {
		String n = name.substring(5);

		if (categories.containsKey(n) || n.equals("name")) {
		    user_properties.add(n, prop);
		}
	    }
	}
    }

    /**
     * Creates an emtpy property set.
     */
    public StaticProperties() {
	defaults = null;
    }

    /**
     * Creates an empty property set with defaults.
     */
    public StaticProperties(StaticProperties sp) {
	super(sp);
	defaults = sp;
    }

    /**
     * Returns the static properties as derived from the system properties
     * provided by the user running the application.
     * @return the user-provided static properties.
     */
    public static StaticProperties userProperties() {
	return user_properties;
    }

    /**
     * Combines the specified properties with system properties as
     * specified by the user.
     * @param sp the static properties
     * @return the combined static properties.
     */
    static StaticProperties combineWithUserProps(StaticProperties sp) {
	StaticProperties combined = new StaticProperties(sp);
	StaticProperties u = userProperties();

	Enumeration e = u.keys();

	while (e.hasMoreElements()) {
	    String name = ((String) e.nextElement());
	    String prop = u.getProperty(name);

	    combined.add(name, prop);
	}
	return combined;
    }

    /**
     * Returns a summary of the specified properties.
     * @param sp the static properties.
     * @return the properties, as an int.
     */
    private static long[] getSummary(StaticProperties sp) {
	int len = category_names.size();
	long [] retval = new long[len];
	for (int i = 0; i < len; i++) {
	    retval[i] = 0;
	    String values = sp.find((String) category_names.get(i));
	    HashMap m = (HashMap)categories.get((String) category_names.get(i));
	    if (values != null) {
		StringTokenizer t = new StringTokenizer(values, ", \t\n\r\f");
		while (t.hasMoreTokens()) {
		    String s = t.nextToken();
		    Object o = m.get(s.toLowerCase());
		    if (o != null) {
			retval[i] |= ((Long) o).longValue();
		    }
		}
		// Unrecognized properties silently ignored?
	    }
	}
	return retval;
    }

    /**
     * Matches the current required properties with the static properties
     * supplied.
     * @param sp the static properties to be matched with.
     * @return true if we have a match, false otherwise.
     */
    public boolean matchProperties(StaticProperties sp) {
	// Maybe build a cache of computed summaries?
	long[] props = getSummary(sp);
	long[] mysummary = getSummary(this);

	for (int i = 0; i < props.length; i++) {
	    if ((mysummary[i] & props[i]) != mysummary[i]) {
		return false;
	    }
	}
	return true;
    }
    /**
     * Adds a key/value pair to the properties.
     * If the key is already bound, an
     * {@link ibis.ipl.IbisRuntimeException IbisRuntimeException}
     * is thrown. If either the key or the value is <code>null</code>,
     * a <code>NullPointer</code> is thrown.
     *
     * @param key the key to be bound.
     * @param value the value to bind to the key.
     * @exception IbisRuntimeException is thrown when the key is already bound.
     * @exception NullPointerException is thrown when either key or value
     *  is <code>null</code>.
     */
    public void add(String key, String value) { 
	key = key.toLowerCase();
	if (containsKey(key)) {
	    throw new IbisRuntimeException("Property " + key +
					   " already exists");
	}
	super.setProperty(key, value);
    }

    /**
     * See {@link #add(String, String)}.
     * @return <code>null</code>.
     */
    public Object setProperty(String key, String value) {
	add(key, value);
	return null;
    }

    /**
     * Returns the value associated with the specified key,
     * or <code>null</code>.
     * @return the value associated with the specified key.
     */
    public String find(String key) {
	return getProperty(key);
    }

    /**
     * Returns the value associated with the specified key,
     * or <code>null</code>.
     * @return the value associated with the specified key.
     */
    public String getProperty(String key) {
	key = key.toLowerCase();
	return super.getProperty(key);
    }

    /**
     * Creates and returns a clone of this.
     * @return a clone.
     */
    public Object clone() {
	StaticProperties sp = new StaticProperties(defaults);
	Enumeration e = keys();
	while (e.hasMoreElements()) {
	    String key = (String) e.nextElement();
	    String value = getProperty(key);
	    sp.add(key, value);
	}
	return sp;
    }

    /**
     * Reads the properties from an inputstream.
     * @param in the input stream.
     * @exception java.io.IOException on IO error.
     */
    public void load(InputStream in) throws IOException {
	Properties p = new Properties();
	p.load(in);
	Enumeration e = p.keys();
	while (e.hasMoreElements()) {
	    String key = (String) e.nextElement();
	    String value = p.getProperty(key);
	    add(key.toLowerCase(), value);
	}
    }

    /**
     * Returns all key/value pairs as a string.
     * The format is: a newline-separated list of
     * key = value pairs.
     * @return the key/value pairs as a string, or an
     * empty string if there are no key/value pairs.
     */
    public String toString() { 

	StringBuffer result = new StringBuffer("");

	Enumeration e = keys();

	while (e.hasMoreElements()) { 
	    String key = (String) e.nextElement();		       			
	    String value = getProperty(key);

	    result.append(key);
	    result.append(" = ");
	    result.append(value);
	    result.append("\n");			
	} 

	return result.toString();
    } 
}
