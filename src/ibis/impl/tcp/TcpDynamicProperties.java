package ibis.impl.tcp;

import ibis.ipl.DynamicProperties;
import ibis.ipl.IbisException;

import java.util.Enumeration;
import java.util.Hashtable;

public class TcpDynamicProperties extends DynamicProperties { 
    Hashtable props = new Hashtable();
    
    public void set(String key, Object value) throws IbisException {
	// throw new IbisException("Invalid dynamic property key: " + key);
	props.put(key, value);
    }

    public Object find(String key) {
	return props.get(key);
    }

    public Enumeration keys() {
	return props.keys();
    }
} 
