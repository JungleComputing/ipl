/* $Id$ */

package ibis.impl.messagePassing;

import ibis.util.TypedProperties;

/**
 * messagePassing implementation of NameServer: the ReceivePort constants
 */
interface ReceivePortNameServerProtocol {

    static final byte PORT_NEW = 20;
    static final byte PORT_ACCEPTED = 21;
    static final byte PORT_REFUSED = 22;
    static final byte PORT_LOOKUP = 23;
    static final byte PORT_LEAVE = 24; /* a port is disconnected */
    static final byte PORT_FREE = 25; /* a port is destroyed */
    static final byte PORT_KNOWN = 26, PORT_UNKNOWN = 27;

    static final boolean DEBUG = TypedProperties.booleanProperty(
            MPProps.s_ns_debug);
}
