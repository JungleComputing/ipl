package ibis.ipl;

import java.util.Enumeration;
import java.util.Properties;

/**
 * Container for the properties of a {@link ibis.ipl.PortType PortType}.
 *
 * A property consists of two strings: its name (key), and its value,
 * for instance: name: ibis.serialization, value: sun.
 */
public class StaticProperties extends Properties {

    /**
     * Creates an emtpy property set.
     */
    public StaticProperties() {
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
	if (containsKey(key)) { 
	    throw new IbisRuntimeException("Property " + key +
					   " already exists");
	} else { 
	    super.setProperty(key, value);
	} 
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
     * Creates and returns a clone of this.
     * @return a clone.
     */
    public Object clone() {
	StaticProperties sp = new StaticProperties();
	Enumeration e = keys();
	while (e.hasMoreElements()) {
	    String key = (String) e.nextElement();
	    String value = getProperty(key);
	    sp.add(key, value);
	}
	return sp;
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
