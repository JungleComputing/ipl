package ibis.impl.nameServer;

import ibis.util.TypedProperties;

/**
 * Collects all system properties used by the ibis.impl.nameServer package
 * and sub-packages.
 */
public class NSProps {
    static final String PROPERTY_PREFIX = "ibis.name_server.";

    public static final String s_impl = PROPERTY_PREFIX + "impl";

    public static final String s_host = PROPERTY_PREFIX + "host";

    public static final String s_key = PROPERTY_PREFIX + "key";

    public static final String s_port = PROPERTY_PREFIX + "port";

    public static final String s_retry = PROPERTY_PREFIX + "retry";

    public static final String s_debug = PROPERTY_PREFIX + "debug";

    public static final String s_single = PROPERTY_PREFIX + "single";

    public static final String s_verbose = PROPERTY_PREFIX + "verbose";

    public static final String s_timeout = PROPERTY_PREFIX + "timeout";

    private static final String[] sysprops = { s_impl, s_host, s_key, s_port,
            s_retry, s_debug, s_single, s_verbose, s_timeout };

    static {
        TypedProperties.checkProperties(PROPERTY_PREFIX, sysprops, null);
    }
}