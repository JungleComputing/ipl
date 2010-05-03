package ibis.ipl.impl.stacking.p2p;

import ibis.ipl.ConnectionFailedException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;
import ibis.ipl.support.vivaldi.Coordinates;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;

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
	private transient SendPort sendPort;

	public P2PNode() {
	}

	public P2PNode(P2PNode other) {
		ibisID = other.getIbisID();
		p2pID = other.getP2pID();
		coords = other.getCoords();
	}

	public P2PNode(P2PIdentifier p2pID, IbisIdentifier ibisID) {
		this.p2pID = p2pID;
		this.ibisID = ibisID;
	}

	public void setIbisID(IbisIdentifier ibisID) {
		this.ibisID = ibisID;
	}

	public IbisIdentifier getIbisID() {
		return ibisID;
	}

	public void setCoords(Coordinates coords) {
		this.coords = coords;
	}

	public Coordinates getCoords() {
		return coords;
	}

	public void setP2pID(P2PIdentifier p2pID) {
		this.p2pID = p2pID;
	}

	public P2PIdentifier getP2pID() {
		return p2pID;
	}

	@Override
	public int compareTo(P2PNode o) {
		return p2pID.compareTo(o.getP2pID());
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}

	public void setDistance(P2PNode other) {
		distance = coords.distance(other.getCoords());
	}

	public double getDistance() {
		return distance;
	}

	/**
	 * tries to connect to ibisIdentifier
	 * if connection is not successful, returns false, otherwise true
	 * @param sendPort
	 * @return
	 */
	public boolean connect(SendPort sendPort) {
		try {
			this.sendPort = sendPort;
			sendPort.connect(ibisID, "p2p");
		} catch (ConnectionFailedException ex) {
			return false;
		}
		return true;
	}

	public void sendObject(Object msg) throws IOException {
		WriteMessage writeMsg = getSendPort().newMessage();
		writeMsg.writeObject(msg);
		writeMsg.finish();
	}

	public void sendObjects(Object... msg) throws IOException {
		WriteMessage writeMsg = getSendPort().newMessage();
		for (int i = 0; i < msg.length; i++) {
			writeMsg.writeObject(msg[i]);
		}
		writeMsg.finish();
	}

	public void sendArray(Object[] msg) throws IOException {
		WriteMessage writeMsg = getSendPort().newMessage();
		writeMsg.writeArray(msg);
		writeMsg.finish();
	}

	public void sendInt(int msg) throws IOException {
		WriteMessage writeMsg = getSendPort().newMessage();
		writeMsg.writeInt(msg);
		writeMsg.finish();
	}

	public void close() throws IOException {
		getSendPort().close();
	}

	public void setSendPort(SendPort sendPort) {
		this.sendPort = sendPort;
	}

	public SendPort getSendPort() {
		return sendPort;
	}

	public BigInteger idDistance(P2PNode other) {
		BigInteger number1 = new BigInteger(p2pID.getP2pID(), P2PConfig.b);
		BigInteger number2 = new BigInteger(other.getP2pID().getP2pID(),
				P2PConfig.b);
		return number1.subtract(number2).abs();
	}

	public double vivaldiDistance(P2PNode other) {
		return coords.distance(other.getCoords());
	}

	public String toString() {
		return p2pID.getP2pID();

	}

	public int prefixLength(P2PNode other) {
		return p2pID.prefixLength(other.getP2pID());
	}

	public int digit(int position) {
		char value = p2pID.charAt(position);
		if (Character.isDigit(value)) {
			return Character.digit(value, 10);
		}
		return (Character.toUpperCase(value) - 'A' + 10);
	}
}
