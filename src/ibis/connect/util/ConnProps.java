package ibis.connect.util;

import ibis.util.TypedProperties;

/**
 * Collects all system properties used by the ibis.connect package
 * and sub-packages.
 */
public class ConnProps {
    public static final String PROPERTY_PREFIX = "ibis.connect.";
    
    public static final String hub_port = PROPERTY_PREFIX + "hub_port";
    public static final String hub_host = PROPERTY_PREFIX + "hub_host";
    public static final String debug = PROPERTY_PREFIX + "debug";
    public static final String par_numways = PROPERTY_PREFIX + "NumWays";
    public static final String par_blocksize = PROPERTY_PREFIX + "BlockSize";
    public static final String verbose = PROPERTY_PREFIX + "verbose";
    public static final String port_range = PROPERTY_PREFIX + "port_range";
    public static final String splice_port = PROPERTY_PREFIX + "splice_port";
    public static final String hub_stats = PROPERTY_PREFIX + "controlhub.stats";
    public static final String datalinks = PROPERTY_PREFIX + "data_links";
    public static final String controllinks = PROPERTY_PREFIX + "control_links";
    public static final String sizes = PROPERTY_PREFIX + "default.sizes";

    private static final String[] sysprops = {
	hub_port,
	hub_host,
	debug,
	verbose,
	port_range,
	splice_port,
	hub_stats,
	datalinks,
	controllinks,
	par_numways,
	par_blocksize,
	sizes
    };

    static {
	System.out.println("ConnProps static initializer called ...");
	TypedProperties.checkProperties(PROPERTY_PREFIX, sysprops, null);
    }
}
