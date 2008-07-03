package ibis.ipl.impl.mx;

class Matching {
	static final long PROTOCOL_DATA =            0x8000000000000000L;
	static final long PROTOCOL_CONNECT =         0x4100000000000000L;
	static final long PROTOCOL_CONNECT_REPLY =   0x4800000000000000L;
	static final long PROTOCOL_DISCONNECT =      0x4300000000000000L;
	static final long PROTOCOL_CLOSE =           0x4400000000000000L;
	static final long PROTOCOL_CONTROL_BIT =     0x4000000000000000L;
	
	static final long PORT_MASK =                0x00000000FFFF0000L;
	static final long CHANNEL_MASK =             0x000000000000FFFFL;
	static final long CONNECTION_MASK =          0x00000000FFFFFFFFL; //Connection := port + channel
	static final long PROTOCOL_MASK =            0xFF00000000000000L;
	static final long PROTOCOL_CONTROL_MASK =    0x0F00000000000000L;
	static final long PROTOCOL_DATA_MASK =       0xF000000000000000L;
	static final long FACTORY_THREAD_MATCH =     0x4000000000000000L;
	static final long FACTORY_THREAD_MASK =      0xF800000000000000L;
	
	static final long MASK_ALL =                 0xFFFFFFFFFFFFFFFFL;
	static final long NONE =                     0x0000000000000000L;

	
	static long construct(long protocol, short receivePort, short channel) {
		return (protocol & PROTOCOL_MASK) | ( (((long)receivePort) << 16) & PORT_MASK) | (channel & CHANNEL_MASK);
	}
	
	static long construct(long protocol, int connection) {
		return (protocol & PROTOCOL_MASK) | (connection & CONNECTION_MASK);
	}
	
	static long getProtocol(long matchData) {
		return matchData & PROTOCOL_MASK;
	}
	
	static long setProtocol(long matchData, long protocol) {
		return (protocol & PROTOCOL_MASK) | (matchData & ~PROTOCOL_MASK);
	}
	
	static short getReceivePort(long matchData) {
		return (short)((matchData & PORT_MASK) >>> 16);
	}
	
	static long setReceivePort(long matchData, short port) {
		return (( ((long)port) << 16) & PORT_MASK) | (matchData & ~PORT_MASK);
	}
	
	static short getChannel(long matchData) {
		return (short)(matchData & CHANNEL_MASK);
	}
	
	static long setChannel(long matchData, short conn) {
		return (conn & CHANNEL_MASK) | (matchData & ~CHANNEL_MASK);
	}
	
	static int getConnection(long matchData) {
		return (int)(matchData & CONNECTION_MASK);
	}
	
	static long setConnection(long matchData, int conn) {
		return (conn & CONNECTION_MASK) | (matchData & ~CONNECTION_MASK);
	}
	
}