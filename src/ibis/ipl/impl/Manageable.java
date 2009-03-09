package ibis.ipl.impl;

import ibis.ipl.NoSuchPropertyException;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public abstract class Manageable implements ibis.ipl.Manageable {

    private HashSet<String> validKeys = new HashSet<String>();

    /** Map for implementing the dynamic properties. */
    private HashMap<String, String> properties = new HashMap<String, String>();

    public synchronized Map<String, String> managementProperties() {
        updateProperties();
        return new HashMap<String,String>(properties);
    }

    public synchronized void setManagementProperties(
            Map<String, String> properties) throws NoSuchPropertyException {
        HashSet<String> keys = new HashSet<String>(properties.keySet());

        for (String key : keys) {
            if (! validKeys.contains(key)) {
                throw new NoSuchPropertyException("Invalid key: " + key);
            }
        }
        properties.putAll(properties);
    }
    
    public synchronized String getManagementProperty(String key)
            throws NoSuchPropertyException {
        if (! validKeys.contains(key)) {
            throw new NoSuchPropertyException("Invalid key: " + key);
        }
        updateProperties();
        return properties.get(key);
    }
    
    public synchronized void setManagementProperty(String key, String val)
            throws NoSuchPropertyException {
        if (! validKeys.contains(key)) {
            throw new NoSuchPropertyException("Invalid key: " + key);
        }
        properties.put(key, val);
    }

    protected void addValidKey(String key) {
        validKeys.add(key);
    }

    protected synchronized void setProperty(String key, String val) {
        properties.put(key, val);
    }
      
    protected abstract void updateProperties();

    public void printManagementProperties(PrintStream stream) {
        updateProperties();
        for(Map.Entry<String, String> entry: properties.entrySet()) {
            stream.println(entry.getKey() + " " + entry.getValue());
        }
    }
}
