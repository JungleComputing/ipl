package ibis.ipl.impl.mx.channels;

final class Matching {
	static final long PROTOCOL_DATA =                0x8000000000000000L;
	static final long PROTOCOL_CONNECT =             0x4100000000000000L;
	static final long PROTOCOL_CONNECT_REPLY =       0x4800000000000000L;
	static final long PROTOCOL_DISCONNECT =          0x4300000000000000L; // WriteChannel disconnects
	static final long PROTOCOL_CLOSE =               0x4400000000000000L; // ReadChannel closes
	static final long PROTOCOL_CONTROL_BIT =         0x4000000000000000L;
	
	static final long MANAGER_MASK =                 0x00000000FFFF0000L;
	static final long CHANNEL_MASK =                 0x000000000000FFFFL;
	static final long PORT_MASK =                    0x00000000FFFFFFFFL; //port := manager + channel
	static final long SEQNO_MASK =                   0x0000FFFF00000000L;
	static final long PROTOCOL_MASK =                0xFF00000000000000L;
	static final long PROTOCOL_CONTROL_MASK =        0x0F00000000000000L;
	static final long PROTOCOL_DATA_MASK =           0xF000000000000000L;
	
	static final long ENDPOINT_TRAFFIC =             0x4000000000000000L;
	static final long ENDPOINT_THREAD_TRAFFIC_MASK = 0xF800000000000000L;
	
	static final long MASK_ALL =                     0xFFFFFFFFFFFFFFFFL;
	static final long NONE =                         0x0000000000000000L;

	
	static long construct(long protocol, short manager, short channel) {
		return (protocol & PROTOCOL_MASK) | ( (((long)manager) << 16) & MANAGER_MASK) | (channel & CHANNEL_MASK);
	}
	
	static long construct(long protocol, int connection) {
		return (protocol & PROTOCOL_MASK) | (connection & PORT_MASK);
	}
	
	static long getProtocol(long matchData) {
		return matchData & PROTOCOL_MASK;
	}
	
	static long setProtocol(long matchData, long protocol) {
		return (protocol & PROTOCOL_MASK) | (matchData & ~PROTOCOL_MASK);
	}
	
	static short getReceiveManager(long matchData) {
		return (short)((matchData & MANAGER_MASK) >>> 16);
	}
	
	static long setReceiveManager(long matchData, short manager) {
		return (( ((long)manager) << 16) & MANAGER_MASK) | (matchData & ~MANAGER_MASK);
	}
	
	static short getChannel(long matchData) {
		return (short)(matchData & CHANNEL_MASK);
	}
	
	static long setChannel(long matchData, short channel) {
		return (channel & CHANNEL_MASK) | (matchData & ~CHANNEL_MASK);
	}
	
	static int getPort(long matchData) {
		return (int)(matchData & PORT_MASK);
	}
	
	static long setPort(long matchData, int port) {
		return (port & PORT_MASK) | (matchData & ~PORT_MASK);
	}
	
	static short getSequenceNumber(long matchData) {
		return (short)(matchData & SEQNO_MASK);
	}
	
	static long setSequenceNumber(long matchData, short number) {
		return (number & SEQNO_MASK) | (matchData & ~SEQNO_MASK);
	}
	
}