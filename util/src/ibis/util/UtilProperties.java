package ibis.util;

import java.util.Properties;

public class UtilProperties extends TypedProperties {

    private static final long serialVersionUID = 1L;

    private static String[] prefs = { "ibis.util.ip.", "ibis.util.monitor.",
            "ibis.util.socketfactory." };
 
    /** Constructs an empty typed properties object. */
    public UtilProperties() {
        super();
    }

    /**
     * Constructs a typed properties object with the specified defaults.
     * @param defaults the defaults.
     */
    public UtilProperties(Properties defaults, String prefix, String[] props) {
        super(defaults);
        checkProperties("ibis.util.", null, prefs, true);
        checkProperties(prefix, props, null, true);
    }
}
