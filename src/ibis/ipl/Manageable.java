package ibis.ipl;

import java.io.PrintStream;
import java.util.Map;

/**
 * A <code>Manageable</code> class is able to read and set
 * implementation-dependant management properties.
 */
public interface Manageable {
    /**
     * Returns the management properties.
     * 
     * @return the management properties.
     */
    public Map<String, String> managementProperties();

    /**
     * Sets the specified management properties.
     * 
     * @param properties
     *            the management properties to set.
     * @exception NoSuchPropertyException
     *                is thrown if one or more of the specified property keys
     *                are not recognized.
     */
    public void setManagementProperties(Map<String, String> properties)
            throws NoSuchPropertyException;

    /**
     * Returns the value of the specified management property.
     * 
     * @param key
     *            the key for the requested property.
     * @return the value associated with the property, or <code>null</code>.
     * @exception NoSuchPropertyException
     *                is thrown if the specified property key is not recognized.
     */
    public String getManagementProperty(String key)
            throws NoSuchPropertyException;

    /**
     * Sets a specified management property to a specified value.
     * 
     * @param key
     *            the key for the property.
     * @param value
     *            the value associated with the property.
     * @exception NoSuchPropertyException
     *                is thrown if the specified property key is not recognized.
     */
    public void setManagementProperty(String key, String value)
            throws NoSuchPropertyException;

    /**
     * Prints the management properties to the specified output stream
     * 
     * @param stream
     *            the stream used to print the management properties.
     */
    public void printManagementProperties(PrintStream stream);
}
