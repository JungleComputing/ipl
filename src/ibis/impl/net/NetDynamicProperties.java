package ibis.impl.net;

import ibis.ipl.DynamicProperties;
import ibis.ipl.IbisException;

import java.util.Enumeration;
import java.util.Hashtable;

public class NetDynamicProperties extends DynamicProperties { 
    Hashtable props = new Hashtable();
    
    /**
     * Stores the association of <code>key</code> and <code>value</code>
     * in our database.
     */
    public void set(String key, Object value) throws IbisException {
	// throw new IbisException("Invalid dynamic property key: " + key);
	props.put(key, value);
    }

    /**
     * Lookup the value of a given key.
     */
    public Object find(String key) {
	return props.get(key);
    }

    /**
     * Return an {@link Enumeration} for all current keys.
     */
    public Enumeration keys() {
	return props.keys();
    }
} 
