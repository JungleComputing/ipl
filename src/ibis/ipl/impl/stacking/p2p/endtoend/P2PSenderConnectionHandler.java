package ibis.ipl.impl.stacking.p2p.endtoend;

import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.impl.stacking.p2p.P2PIbis;
import ibis.ipl.impl.stacking.p2p.util.P2PConfig;

import java.io.IOException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * besides sendBase, sendMax, seqNum, implement queue of messages
 * 
 * @author Delia
 * 
 */
public class P2PSenderConnectionHandler {
	private int sendBase, sendMax, seqNum;
	private ArrayList<P2PMessage> messages;
	private P2PIbis ibis;
	//private ReceivePortIdentifier rid;
	
	private static final Logger logger = LoggerFactory.getLogger(P2PSenderConnectionHandler.class);
	
	public P2PSenderConnectionHandler(P2PIbis ibis) {
		sendBase = 0;
		sendMax = P2PConfig.WINDOW_SIZE - 1;
		seqNum = 0;
		messages = new ArrayList<P2PMessage>();
		this.ibis = ibis;
	}

	public synchronized void putMessage(P2PMessage message) {
		message.setSeqNum(getSeqNum());
		messages.add(message.getSeqNum(), message);
		
		logger.debug("queued message with seq num " + message.getSeqNum() + " for sid" + message.getSid());
	}

	public synchronized void processAck(int ackNum) {
		logger.debug("Processing message ack with seq num: " + ackNum);
	
		if (ackNum > sendBase) {
			sendMax = (sendMax + (ackNum - sendBase)) % P2PConfig.MAX_SEQ_NUM;
			sendBase = ackNum % P2PConfig.MAX_SEQ_NUM;
			
			logger.debug("Send base and send max adjusted to: " + sendBase + " " + sendMax);
		}
	}

	/**
	 * @param sendBase
	 *            the sendBase to set
	 */
	public synchronized void setSendBase(int sendBase) {
		this.sendBase = sendBase;
	}

	/**
	 * @return the sendBase
	 */
	public synchronized int getSendBase() {
		return sendBase;
	}

	/**
	 * @param sendMax
	 *            the sendMax to set
	 */
	public synchronized void setSendMax(int sendMax) {
		this.sendMax = sendMax;
	}

	/**
	 * @return the sendMax
	 */
	public synchronized int getSendMax() {
		return sendMax;
	}

	/**
	 * @param seqNum
	 *            the seqNum to set
	 */
	public synchronized void setSeqNum(int seqNum) {
		this.seqNum = seqNum;
	}

	/**
	 * @return the seqNum
	 */
	public synchronized int getSeqNum() {
		int currSeqNum = seqNum;
		seqNum = (seqNum + 1) % P2PConfig.WINDOW_SIZE;

		if (currSeqNum == 0) {
			messages.clear();
		}

		return currSeqNum;
	}

	public synchronized void sendNextMessage() throws IOException, ClassNotFoundException {
		// send all messages between sendBase and sendMax
		logger.debug("Send base: " + sendBase + " Send max: " + sendMax);
		
		for (int i = sendBase; i<sendMax && i<messages.size(); i++) {
			P2PMessage message = messages.get(i);
			logger.debug("Sending message with seq num " + i);
			ibis.send(message);
		}
	}
}
