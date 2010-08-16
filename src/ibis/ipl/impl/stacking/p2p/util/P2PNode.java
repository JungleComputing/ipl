package ibis.ipl.impl.stacking.p2p.util;

import ibis.ipl.ConnectionFailedException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.support.vivaldi.Coordinates;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * encapsulation of node identification in overlay network node ID - ID in
 * overlay network - compute SHA-1 of IP address basis node ID - ID in
 * underlying network distance according with proximity metric
 * 
 * @author delia
 * 
 */
public class P2PNode implements Serializable, Comparable<P2PNode> {
	private static final long serialVersionUID = 1L;
	private IbisIdentifier ibisID;
	private Coordinates coords;
	private P2PIdentifier p2pID;
	private double distance;
	private Vector<ReceivePortIdentifier> receivePortNames;

	private transient SendPort sendPort;
	private transient boolean connected, failed;
	private transient long lastAckTime;

	private transient static final Logger logger = LoggerFactory
			.getLogger(P2PNode.class);

	public P2PNode() {
		receivePortNames = new Vector<ReceivePortIdentifier>();
	}

	public P2PNode(P2PNode other) {
		ibisID = other.getIbisID();
		p2pID = other.getP2pID();
		coords = other.getCoords();
		receivePortNames = new Vector<ReceivePortIdentifier>();
		this.lastAckTime = System.currentTimeMillis();

		setConnected(false);
	}

	public P2PNode(P2PIdentifier p2pID, IbisIdentifier ibisID) {
		this.p2pID = p2pID;
		this.ibisID = ibisID;
		this.lastAckTime = System.currentTimeMillis();

		receivePortNames = new Vector<ReceivePortIdentifier>();

		setConnected(false);
	}

	public P2PNode(IbisIdentifier ibisID) {
		this.p2pID = new P2PIdentifier(P2PHashTools.MD5(ibisID.name()));
		this.ibisID = ibisID;
		this.lastAckTime = System.currentTimeMillis();

		receivePortNames = new Vector<ReceivePortIdentifier>();

		setConnected(false);
	}

	public P2PNode(ReceivePortIdentifier rid) {
		this.ibisID = rid.ibisIdentifier();
		this.p2pID = new P2PIdentifier(P2PHashTools.MD5(ibisID.name()));
		this.lastAckTime = System.currentTimeMillis();

		receivePortNames = new Vector<ReceivePortIdentifier>();

		addReceivePortName(rid);
		setConnected(false);
	}

	public synchronized void setIbisID(IbisIdentifier ibisID) {
		this.ibisID = ibisID;
	}

	public synchronized IbisIdentifier getIbisID() {
		return ibisID;
	}

	public synchronized void setCoords(Coordinates coords) {
		this.coords = coords;
	}

	public synchronized Coordinates getCoords() {
		return coords;
	}

	public synchronized void setP2pID(P2PIdentifier p2pID) {
		this.p2pID = p2pID;
	}

	public synchronized P2PIdentifier getP2pID() {
		return p2pID;
	}

	@Override
	public synchronized int compareTo(P2PNode o) {
		return p2pID.compareTo(o.getP2pID());
	}

	public synchronized void setDistance(double distance) {
		this.distance = distance;
	}

	public synchronized void setDistance(P2PNode other) {
		distance = coords.distance(other.getCoords());
	}

	public synchronized double getDistance() {
		return distance;
	}

	public synchronized void sendObject(Object msg) throws IOException {
		WriteMessage writeMsg = getSendPort().newMessage();
		writeMsg.writeObject(msg);
		writeMsg.finish();
	}

	public synchronized void sendObjects(P2PMessageHeader msg,
			Object... objects) {
		try {
			WriteMessage writeMsg = getSendPort().newMessage();
			writeMsg.writeObject(msg);
			for (Object obj : objects) {
				writeMsg.writeObject(obj);
			}
			writeMsg.finish();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public synchronized void close() throws IOException {
		getSendPort().close();
		setConnected(false);
	}

	public synchronized void setSendPort(SendPort sendPort) {
		this.sendPort = sendPort;
	}

	public synchronized SendPort getSendPort() {
		return sendPort;
	}

	public synchronized BigInteger idDistance(P2PNode other) {
		BigInteger number1 = new BigInteger(p2pID.getP2pID(), P2PConfig.b);
		BigInteger number2 = new BigInteger(other.getP2pID().getP2pID(),
				P2PConfig.b);
		return number1.subtract(number2).abs();
	}

	public synchronized double vivaldiDistance(P2PNode other) {
		return coords.distance(other.getCoords());
	}

	public synchronized String toString() {
		return p2pID.getP2pID();
		// return ibisID.name();

	}

	public synchronized int prefixLength(P2PNode other) {
		return p2pID.prefixLength(other.getP2pID());
	}

	public synchronized int digit(int position) {
		char value = p2pID.charAt(position);
		if (Character.isDigit(value)) {
			return Character.digit(value, 10);
		}
		return (Character.toUpperCase(value) - 'A' + 10);
	}

	public synchronized void addReceivePortName(
			ReceivePortIdentifier recvPortName) {
		receivePortNames.add(recvPortName);
	}

	public synchronized Vector<ReceivePortIdentifier> getReceivePortNames() {
		return receivePortNames;
	}

	public boolean equals(P2PNode other) {
		if (this.compareTo(other) == 0) {
			return true;
		}
		return false;
	}

	public synchronized boolean connect(SendPort sendPort) {
		try {
			if (!isConnected()) {
				this.sendPort = sendPort;
				this.sendPort.connect(ibisID, P2PConfig.PORT_NAME);
				setConnected(true);
			}
			return true;
		} catch (ConnectionFailedException ex) {
			ex.printStackTrace();
			return false;
		}
	}

	public synchronized void setConnected(boolean connected) {
		this.connected = connected;
	}

	public synchronized boolean isConnected() {
		return connected;
	}

	public synchronized void copyObject(P2PNode other) {
		this.ibisID = other.getIbisID();
		this.coords = other.getCoords();
		this.p2pID = other.getP2pID();
		this.sendPort = other.getSendPort();
		this.distance = other.getDistance();
		this.lastAckTime = System.currentTimeMillis();
		this.connected = other.isConnected();
	}

	public synchronized void copyObject(P2PNode other, SendPort sendPort) {
		this.ibisID = other.getIbisID();
		this.coords = other.getCoords();
		this.p2pID = other.getP2pID();
		this.lastAckTime = System.currentTimeMillis();

		if (other.getSendPort() != null) {
			this.sendPort = other.sendPort;
			logger.debug("Other was already connected.");
		} else {
			this.sendPort = sendPort;
			try {
				this.sendPort.connect(ibisID, P2PConfig.PORT_NAME);
			} catch (ConnectionFailedException e) {
				e.printStackTrace();
			}
		}
		setConnected(true);
	}

	public synchronized void setAckTime() {
		lastAckTime = System.currentTimeMillis();
	}

	public synchronized long getAckTimeout() {
		return (System.currentTimeMillis() - lastAckTime);
	}

	public synchronized boolean isFailed() {
		long timeout = System.currentTimeMillis() - lastAckTime;

		if (timeout > P2PConfig.ACK_THRESHOLD) {
			return true;
		}

		return false;
	}

	/**
	 * before sending the node, determine if it is failed or not the other party
	 * will not consider failed nodes as a valid replacement
	 */
	public void setFailed() {
		failed = isFailed();
	}

	public boolean getFailed() {
		return failed;
	}

	// FIXME: bad practice!
	protected void finalize() throws Throwable {
		try {
			close();
		} catch (Exception e) {
		} finally {
			super.finalize();
		}
	}
}