package ibis.ipl;

import java.util.Map;

/**
 * A <code>Managable</code> class is able to read and set
 * implementation-dependant dynamic properties.
 */
public interface Managable {
    /**
     * Returns the dynamic properties.
     * @return the dynamic properties.
     */
    public Map<String, String> dynamicProperties();

    /**
     * Sets the specified dynamic properties.
     * @param properties the dynamic properties to set.
     */
    public void setDynamicProperties(Map<String, String> properties); 
    
    /**
     * Returns the value of the specified dynamic property.
     * @param key the key for the requested property.
     * @return the value associated with the property, or <code>null</code>.
     */
    public String getDynamicProperty(String key);
    
    /**
     * Sets a specified dynamic property to a specified value.
     * @param key the key for the property.
     * @param value the value associated with the property.
     */
    public void setDynamicProperty(String key, String value);
}
