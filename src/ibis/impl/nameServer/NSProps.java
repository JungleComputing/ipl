/* $Id$ */

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

    public static final String s_single = PROPERTY_PREFIX + "single";

    public static final String s_pinger_timeout = PROPERTY_PREFIX + "pingerTimeout";

    public static final String s_connect_timeout = PROPERTY_PREFIX + "connectTimeout";

    public static final String s_joiner_interval = PROPERTY_PREFIX + "joinerInterval";

    public static final String s_keychecker_interval = PROPERTY_PREFIX + "checkerInterval";

    private static final String[] sysprops = { s_impl, s_host, s_key, s_port,
            s_single, s_pinger_timeout, s_connect_timeout, s_joiner_interval,
            s_keychecker_interval};

    static {
        TypedProperties.checkProperties(PROPERTY_PREFIX, sysprops, null);
    }
}
