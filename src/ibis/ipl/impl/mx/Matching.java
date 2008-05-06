package ibis.ipl.impl.mx;

public abstract class Matching {
	static final long DATA =          0x8000000000000000L; 
	static final long CONNECT =       0x0100000000000000L;
	static final long CONNECT_REPLY = 0x0200000000000000L;
	static final long DISCONNECT =    0x0300000000000000L;
	static final long CLOSE =         0x0400000000000000L;
	static final long PORT_MASK =     0x00000000FFFFFFFFL;
	static final long PROTOCOL_MASK = 0xFF00000000000000L;
	
	protected long matchData;
	
	int getPort() {
		return (int)(matchData & PORT_MASK);
	}
	
	long getProtocol() {
		return matchData & PROTOCOL_MASK;
	}
	
	void setPort(int port) {
		matchData = (port & PORT_MASK)| (matchData & PROTOCOL_MASK);
	}
	
	void setProtocol(long protocol) {
		matchData = (matchData & PORT_MASK)| (protocol & PROTOCOL_MASK);
	}
}