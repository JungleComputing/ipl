package ibis.ipl.impl;

import java.util.HashMap;
import java.util.Map;

public class Managable implements ibis.ipl.Managable {
    /** Map for implementing the dynamic properties. */
    private HashMap<String, Object> properties = new HashMap<String, Object>();

    @SuppressWarnings("unchecked")
    public synchronized Map<String, Object> dynamicProperties() {
        return (Map<String, Object>) properties.clone();
    }

    public synchronized void setDynamicProperties(
            Map<String, Object> properties) {
        properties.putAll(properties);
    }
    
    public synchronized Object getDynamicProperty(String key) {
        return properties.get(key);
    }
    
    public synchronized void setDynamicProperty(String key, Object val) {
        properties.put(key, val);
    }
}
