package ibis.impl.net;

import ibis.ipl.DynamicProperties;
import ibis.ipl.IbisException;

import java.util.Enumeration;
import java.util.Hashtable;

public class NetDynamicProperties extends DynamicProperties { 
    Hashtable props = new Hashtable();
    
    /**
     * {@inheritDoc}
     */
    public void set(String key, Object value) throws IbisException {
	// throw new IbisException("Invalid dynamic property key: " + key);
	props.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    public Object find(String key) {
	return props.get(key);
    }

    /**
     * {@inheritDoc}
     */
    public Enumeration keys() {
	return props.keys();
    }
} 
