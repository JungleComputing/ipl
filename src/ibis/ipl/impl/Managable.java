package ibis.ipl.impl;

import ibis.ipl.NoSuchPropertyException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class Managable implements ibis.ipl.Managable {

    private HashSet<String> validKeys = new HashSet<String>();

    /** Map for implementing the dynamic properties. */
    private HashMap<String, String> properties = new HashMap<String, String>();

    public synchronized Map<String, String> dynamicProperties() {
        return new HashMap<String,String>(properties);
    }

    public synchronized void setDynamicProperties(
            Map<String, String> properties) throws NoSuchPropertyException {
        HashSet<String> keys = new HashSet<String>(properties.keySet());

        for (String key : keys) {
            if (! validKeys.contains(key)) {
                throw new NoSuchPropertyException("Invalid key: " + key);
            }
        }
        properties.putAll(properties);
    }
    
    public synchronized String getDynamicProperty(String key)
            throws NoSuchPropertyException {
        if (! validKeys.contains(key)) {
            throw new NoSuchPropertyException("Invalid key: " + key);
        }
        return properties.get(key);
    }
    
    public synchronized void setDynamicProperty(String key, String val)
            throws NoSuchPropertyException {
        if (! validKeys.contains(key)) {
            throw new NoSuchPropertyException("Invalid key: " + key);
        }
        properties.put(key, val);
    }

    protected void addValidKey(String key) {
        validKeys.add(key);
    }
}
