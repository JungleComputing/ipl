package ibis.ipl.impl.mx;

public abstract class Matching {
	static final long DATA =            0x8000000000000000L; 
	static final long CONNECT =         0x0100000000000000L;
	static final long CONNECT_REPLY =   0x0200000000000000L;
	static final long DISCONNECT =      0x0300000000000000L;
	static final long CLOSE =           0x0400000000000000L;
	static final long PORT_MASK =       0x00000000FFFF0000L;
	static final long CHANNEL_MASK =    0x000000000000FFFFL;
	static final long CONNECTION_MASK = 0x00000000FFFFFFFFL;
	static final long PROTOCOL_MASK =   0xFF00000000000000L;
	static final long MASK_NONE =       0x0000000000000000L;
	
	protected long matchData = MASK_NONE;
		
	long getProtocol() {
		return matchData & PROTOCOL_MASK;
	}
	
	void setProtocol(long protocol) {
		matchData = (protocol & PROTOCOL_MASK) | (matchData & ~PROTOCOL_MASK);
	}
	
	short getReceivePort() {
		return (short)((matchData & PORT_MASK) >>> 16);
	}
	
	void setReceivePort(short port) {
		matchData = (( ((long)port) << 16) & PORT_MASK) | (matchData & ~PORT_MASK);
	}
	
	short getChannel() {
		return (short)(matchData & CHANNEL_MASK);
	}
	
	void setChannel(short conn) {
		matchData = (conn & CHANNEL_MASK) | (matchData & ~CHANNEL_MASK);
	}
	
	int getConnection() {
		return (int)(matchData & CONNECTION_MASK);
	}
	
	void setConnection(int conn) {
		matchData = (conn & CONNECTION_MASK) | (matchData & ~CONNECTION_MASK);
	}
	
}