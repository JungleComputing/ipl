package ibis.ipl.impl.stacking.p2p;

import ibis.ipl.PortType;

public class P2PConfig {
	public final static int N = 100;
	public final static int b = 7;
	public final static int LEAF_SIZE = 4;
	public final static int NEIGHBOORHOOD_SIZE = 10;
	public final static PortType portType = new PortType(PortType.COMMUNICATION_RELIABLE,
			PortType.SERIALIZATION_OBJECT_SUN, PortType.RECEIVE_AUTO_UPCALLS,
			PortType.CONNECTION_MANY_TO_ONE);

}
