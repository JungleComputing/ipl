package ibis.ipl;

import java.util.Map;

/**
 * Implementors of this interface allow for (implementation-dependent)
 * dynamic properties to be set or read.
 */
public interface Managable {
    /**
     * Returns the dynamic properties.
     * @return the dynamic properties.
     */
    public Map<String, Object> dynamicProperties();

    /**
     * Sets the specified dynamic properties.
     * @param properties the dynamic properties to set.
     */
    public void setDynamicProperties(Map<String, Object> properties); 
    
    /**
     * Returns the value of the specified dynamic property.
     * @param key the key for the requested property.
     * @return the value associated with the property, or <code>null</code>.
     */
    public Object getDynamicProperty(String key);
    
    /**
     * Sets a specified dynamic property to a specified value.
     * @param key the key for the property.
     * @param val the value associated with the property.
     */
    public void setDynamicProperty(String key, Object val);
}
