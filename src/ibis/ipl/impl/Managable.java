package ibis.ipl.impl;

import ibis.ipl.NoSuchPropertyException;

import java.util.HashMap;
import java.util.Map;

public class Managable implements ibis.ipl.Managable {

    /** Map for implementing the dynamic properties. */
    private HashMap<String, String> properties = new HashMap<String, String>();

    public synchronized Map<String, String> dynamicProperties() {
        return new HashMap(properties);
    }

    public synchronized void setDynamicProperties(
            Map<String, String> properties) throws NoSuchPropertyException {
        properties.putAll(properties);
    }
    
    public synchronized String getDynamicProperty(String key)
            throws NoSuchPropertyException {
        return properties.get(key);
    }
    
    public synchronized void setDynamicProperty(String key, String val)
            throws NoSuchPropertyException {
        properties.put(key, val);
    }
}
