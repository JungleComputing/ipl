package ibis.ipl.impl.messagePassing;

interface ReceivePortNameServerProtocol {

    static final byte
	PORT_NEW = 20,
	PORT_ACCEPTED = 21,
	PORT_REFUSED = 22,
	PORT_LOOKUP = 23,
	PORT_LEAVE = 24, /* a port is disconnected */
	PORT_FREE = 25,	/* a port is destroyed */
	PORT_KNOWN = 26,
	PORT_UNKNOWN = 27;
}
