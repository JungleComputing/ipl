package ibis.connect.socketFactory;

/**
 * Interface for passing on connection properties.
 */
public interface ConnectProperties {
    /**
     * Returns the string associated with the specified name.
     * @param name the property name
     * @return the string associated with the specified name, or
     * <code>null</code> if not present.
     */
    String getProperty(String name);
}