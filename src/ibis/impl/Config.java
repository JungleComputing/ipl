/* $Id: Config.java 4910 2006-12-13 09:01:33Z ceriel $ */

package ibis.impl;

import ibis.util.TypedProperties;

interface Config {
    static final String PROPERTY_PREFIX = "ibis.impl.";

    static final String s_debug = PROPERTY_PREFIX + "debug";

    static final boolean DEBUG = TypedProperties.booleanProperty(s_debug);

    static final String[] sysprops = { s_debug};
}
