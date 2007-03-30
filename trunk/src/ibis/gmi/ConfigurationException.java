/* $Id$ */

package ibis.gmi;

/**
 * The {@link ConfigurationException} class describes exceptions caused by
 * illegal group method configurations.
 */
public class ConfigurationException extends Exception {
    /** 
     * Generated
     */
    private static final long serialVersionUID = -7899875713676822277L;

    /**
     * Constructor.
     *
     * @param message the details of the configuration fault
     */
    public ConfigurationException(String message) {
        super(message);
    }
}
