/* $Id$ */

package ibis.impl.registry;

import ibis.util.TypedProperties;

/**
 * Collects all system properties used by the ibis.impl.registry package
 * and sub-packages.
 */
public class NSProps {
    static final String PROPERTY_PREFIX = "ibis.registry.";

    public static final String s_impl = PROPERTY_PREFIX + "impl";

    public static final String s_host = PROPERTY_PREFIX + "host";

    public static final String s_pool = PROPERTY_PREFIX + "pool";

    public static final String s_port = PROPERTY_PREFIX + "port";

    public static final String s_single = PROPERTY_PREFIX + "single";

    public static final String s_pinger_timeout = PROPERTY_PREFIX + "pingerTimeout";

    public static final String s_connect_timeout = PROPERTY_PREFIX + "connectTimeout";

    public static final String s_joiner_interval = PROPERTY_PREFIX + "joinerInterval";

    public static final String s_poolchecker_interval = PROPERTY_PREFIX + "checkerInterval";

    public static final String s_max_threads = PROPERTY_PREFIX + "maxThreads";

    private static final String[] sysprops = { s_impl, s_host, s_pool, s_port,
            s_single, s_pinger_timeout, s_connect_timeout, s_joiner_interval,
            s_poolchecker_interval, s_max_threads};

    static {
        TypedProperties.checkProperties(PROPERTY_PREFIX, sysprops, null);
    }
}
