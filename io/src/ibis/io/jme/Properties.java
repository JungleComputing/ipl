package ibis.io.jme;

import java.util.Hashtable;
import java.util.Enumeration;

public class Properties extends Hashtable {
	
	public Properties(){
		super();
	}
	
	public Properties(Properties p) {
		super();
		addProperties(p);
	}
	
    /**
     * Adds the specified properties to the current ones.
     * 
     * @param properties
     *            the properties to add.
     */
    public void addProperties(Properties properties) {
        if (properties == null) {
            return;
        }
        for (Enumeration e = properties.propertyNames(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            String value = properties.getProperty(key);
            setProperty(key, value);
        }
    }
	
    public Enumeration propertyNames() {
    	return keys();
    }
    
	public void setProperty(String key, String value) {
		put(key, value);
	}
	
	public String getProperty(String key) {
		String value = (String)get(key);
		// TODO: IOProperties expects an override here. So we should check
		// the system property for now.
		if (null == value) {
			value = System.getProperty(key);
		}
		return value;
	}
	
	public String getProperty(String key, String def) {
		String value = (String)get(key);
		// TODO: IOProperties expects an override here. So we should check
		// the system property for now.
		if (null == value) {
			value = System.getProperty(key);
		}
		if (null == value) {
			value = def;
		}
		return value;
	}
}
