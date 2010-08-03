package ibis.ipl.impl.stacking.p2p.util;

import java.math.BigInteger;

import ibis.ipl.PortType;

public class P2PConfig {
	public final static int N = 100;
	public final static int b = 16;
	public final static int MAX_PREFIX = 32;
	public final static int MAX_DIGITS = 16;
	public final static int LEAF_SIZE = 8;
	public final static int NEIGHBOORHOOD_SIZE = 10;
	public final static PortType portType = new PortType(PortType.COMMUNICATION_RELIABLE,
			PortType.SERIALIZATION_OBJECT_SUN, PortType.RECEIVE_AUTO_UPCALLS, PortType.RECEIVE_EXPLICIT,
			PortType.CONNECTION_MANY_TO_MANY);

	public final static int TIMEOUT = 5000;
	public final static String PORT_NAME = "p2p";
	public final static String TRACKER_PORT = "p2ptracker";
	public final static String ELECTION_JOIN = "p2pjoin";
	public final static String ELECTION_GUI = "p2pgui";
	public final static String ELECTION_TRACKER = "p2ptracker";
	
	public final static int NEARBY_REQUESTS = 5;
	public final static int NEARBY_TIMEOUT = 10000;
	public final static int REPAIR_TIMEOUT = 10000;
	
	public final static BigInteger MAX = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16);
	
	public static final long ACK_THRESHOLD = 5000;
	
	public static final int WINDOW_SIZE = 5;	
	public static final int MAX_SEQ_NUM = 20;
}
