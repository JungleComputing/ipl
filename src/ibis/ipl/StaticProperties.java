package ibis.ipl;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Container for the properties of a {@link ibis.ipl.PortType PortType}.
 *
 * A property consists of two strings: its name (key), and its value,
 * for instance: name: ibis.serialization, value: sun.
 */
public class StaticProperties {

    /**
     * A hashtable, with property names as key.
     */
    private final Hashtable data = new Hashtable();

    public StaticProperties() {
    }

    /**
     * Constructor creating a copy.
     * @param sp the properties to be copied.
     */
    public StaticProperties(StaticProperties sp) {

	Enumeration e = sp.keys();

	while (e.hasMoreElements()) { 
	    String key = (String) e.nextElement();		       			
	    String value = (String) sp.find(key);
	    try {
		add(key, value);
	    } catch(IbisException ex) {
		// Should not happen.
	    }
	}
    }

    /**
     * Adds a key/value pair to the properties.
     * If the key is already bound, an {@link ibis.ipl.IbisException IbisException}
     * is thrown. If either the key or the value is <code>null</code>,
     * a <code>NullPointer</code> is thrown.
     *
     * @param key the key to be bound.
     * @param value the value to bind to the key.
     * @exception IbisException is thrown when the key is already bound.
     * @exception NullPointerException is thrown when either key or value is <code>null</code>.
     */
    public void add(String key, String value) throws IbisException { 
	if (data.containsKey(key)) { 
	    throw new IbisException("Property already exists");
	} else { 
	    try { 
		data.put(key, value);
	    } catch (NullPointerException e) { 
		if (key == null) { 
		    throw new IbisException("Property needs a key");
		} else { 
		    throw new IbisException("Property needs a value");
		}
	    } 
	} 
    }

    /**
     * Returns the value of the property associated with a key.
     *
     * @param key the key of the requested property, i.e. "ibis.serialization".
     * @return the value of the property, i.e. "sun".
     */
    public String find(String key) { 
	return (String) data.get(key);
    } 

    /**
     * Returns an Enumeration of all bound property keys.
     * @return the keys.
     */
    public Enumeration keys() { 
	return data.keys();
    } 		      

    /**
     * Returns the number of bound property keys.
     * @return the number of property keys.
     */
    public int size() { 
	return data.size();
    }

    /**
     * Comparison for equality.
     *
     * @param other the object to compare to.
     * @return <code>true</code> if both have the same keys, bound to the same values.
     */
    public boolean equals(Object other) {
	if (!(other instanceof StaticProperties)) { 
	    return false;
	}

	StaticProperties temp = (StaticProperties) other;

	if (data.size() != temp.data.size()) { 
	    return false;
	}

	Enumeration e = data.keys();

	while (e.hasMoreElements()) { 
	    String key = (String) e.nextElement();		       			
	    String value1 = (String) data.get(key);
	    String value2 = (String) temp.data.get(key);

	    if (value1 == null) {
		if (value2 != null) return false;
	    }
	    else if (value2 == null) {
		return false;
	    }
	    else if (!value1.equals(value2)) { 
		return false;
	    }
	} 

	return true;
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

	Enumeration e = data.keys();

	while (e.hasMoreElements()) { 
	    String key = (String) e.nextElement();		       			
	    String value = (String) data.get(key);

	    result.append(key);
	    result.append(" = ");
	    result.append(value);
	    result.append("\n");			
	} 

	return result.toString();
    } 
}
