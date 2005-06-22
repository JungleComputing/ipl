/* $Id$ */

package ibis.connect;

import ibis.util.TypedProperties;

/**
 * Collects all system properties used by the ibis.connect package and
 * sub-packages.
 */
public class ConnectionProperties {
    public static final String PROPERTY_PREFIX = "ibis.connect.";

    private static final String DEBUG_PROP = PROPERTY_PREFIX + "debug";

    public static final String PAR_NUMWAYS = PROPERTY_PREFIX + "NumWays";

    public static final String PAR_BLOCKSIZE = PROPERTY_PREFIX + "BlockSize";

    private static final String VERBOSE_PROP = PROPERTY_PREFIX + "verbose";

    public static final String PORT_RANGE = PROPERTY_PREFIX + "port_range";

    public static final String SPLICE_PORT = PROPERTY_PREFIX + "splice_port";

    public static final String DATA_LINKS = PROPERTY_PREFIX + "data_links";

    public static final String CONTROL_LINKS = PROPERTY_PREFIX
            + "control_links";

    public static final String ISIZE = PROPERTY_PREFIX + "InputBufferSize";

    public static final String OSIZE = PROPERTY_PREFIX + "OutputBufferSize";

    public static final String HUB_PORT = PROPERTY_PREFIX + "hub.port";

    public static final String HUB_HOST = PROPERTY_PREFIX + "hub.host";

    public static final String HUB_STATS = PROPERTY_PREFIX + "hub.stats";

    public static final boolean DEBUG = TypedProperties.booleanProperty(
            DEBUG_PROP, false);

    public static final boolean VERBOSE = TypedProperties.booleanProperty(
            VERBOSE_PROP, false);

    public static int inputBufferSize = 0;// 64 * 1024;

    public static int outputBufferSize = 0;//64 * 1024;

    private static final String[] sysprops = { HUB_PORT, HUB_HOST,
            DEBUG_PROP, VERBOSE_PROP, PORT_RANGE, SPLICE_PORT, HUB_STATS,
            DATA_LINKS, CONTROL_LINKS, PAR_NUMWAYS, PAR_BLOCKSIZE, ISIZE, OSIZE };

    static {
        TypedProperties.checkProperties(PROPERTY_PREFIX, sysprops, null);
        inputBufferSize = TypedProperties.intProperty(ISIZE, 0);
        outputBufferSize = TypedProperties.intProperty(OSIZE, 0);
    }
}