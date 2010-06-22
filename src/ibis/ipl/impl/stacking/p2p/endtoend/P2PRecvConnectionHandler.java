package ibis.ipl.impl.stacking.p2p.endtoend;

import ibis.ipl.impl.stacking.p2p.util.P2PConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class P2PRecvConnectionHandler {
	private int seqNum;
	
	private static final Logger logger = LoggerFactory.getLogger(P2PRecvConnectionHandler.class);
	
	public P2PRecvConnectionHandler() {
		seqNum = 0;
	}
	
	public synchronized boolean processSeqNum(int seqNum) {
		if (this.seqNum == seqNum) {
			this.seqNum = (this.seqNum + 1) % P2PConfig.MAX_SEQ_NUM;
			return true;
		}
		return false;
	}
	
	public synchronized int getSeqNum() {
		return seqNum;
	}
}
