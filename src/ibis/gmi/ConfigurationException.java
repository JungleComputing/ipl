package ibis.gmi;

/**
 * The {@link ConfigurationException} class describes exceptions caused by
 * illegal group method configurations.
 */
public class ConfigurationException extends Exception {
    /**
     * Constructor.
     *
     * @param message the details of the configuration fault
     */
    public ConfigurationException(String message) {
        super(message);
    }
}
