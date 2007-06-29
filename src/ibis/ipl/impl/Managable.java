package ibis.ipl.impl;

import java.util.HashMap;
import java.util.Map;

public class Managable implements ibis.ipl.Managable {
    /** Map for implementing the dynamic properties. */
    private HashMap<String, String> properties = new HashMap<String, String>();

    @SuppressWarnings("unchecked")
    public synchronized Map<String, String> dynamicProperties() {
        return (Map<String, String>) properties.clone();
    }

    public synchronized void setDynamicProperties(
            Map<String, String> properties) {
        properties.putAll(properties);
    }
    
    public synchronized String getDynamicProperty(String key) {
        return properties.get(key);
    }
    
    public synchronized void setDynamicProperty(String key, String val) {
        properties.put(key, val);
    }
}
