package ibis.impl.tcp;

import ibis.ipl.DynamicProperties;
import ibis.ipl.IbisException;

import java.util.Enumeration;
import java.util.Hashtable;

public class TcpDynamicProperties extends DynamicProperties { 
    Hashtable props = new Hashtable();
    
    /**
     * {@inheritDoc}
     */
    public void set(String key, Object value) throws IbisException {
	if (key.equals("InputBufferSize")) {
	}
	else if (key.equals("OutputBufferSize")) {
	}
	else {
	    throw new IbisException("Invalid dynamic property key: " + key);
	}
	if (value instanceof Integer) {
	}
	else if (value instanceof String) {
	    try {
		int n = Integer.parseInt((String) value);
		value = new Integer(n);
	    } catch(NumberFormatException e) {
		throw new IbisException("Invalid dynamic property value for " + key + ": " + (String) value);
	    }
	}
	else {
	    throw new IbisException("Invalid dynamic property value for " + key);
	}
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
