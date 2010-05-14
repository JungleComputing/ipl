package ibis.ipl.impl.stacking.p2p;

import ibis.ipl.ConnectionFailedException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;
import ibis.ipl.support.vivaldi.Coordinates;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Vector;

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
	private Vector<String> receivePortNames;
	private transient SendPort sendPort;
	private transient boolean connected;

	public P2PNode() {
		receivePortNames = new Vector<String>();
	}

	public P2PNode(P2PNode other) {
		ibisID = other.getIbisID();
		p2pID = other.getP2pID();
		coords = other.getCoords();

		receivePortNames = new Vector<String>();

		setConnected(false);
	}

	public P2PNode(P2PIdentifier p2pID, IbisIdentifier ibisID) {
		this.p2pID = p2pID;
		this.ibisID = ibisID;

		receivePortNames = new Vector<String>();

		setConnected(false);
	}

	public P2PNode(IbisIdentifier ibisID) {
		this.p2pID = new P2PIdentifier(P2PHashTools.MD5(ibisID.name()));
		this.ibisID = ibisID;

		receivePortNames = new Vector<String>();

		setConnected(false);
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

	public void sendObject(Object msg) throws IOException {
		WriteMessage writeMsg = getSendPort().newMessage();
		writeMsg.writeObject(msg);
		writeMsg.finish();
	}

	public void sendObjects(Object... objects) {
		try {
			WriteMessage writeMsg = getSendPort().newMessage();
			for (Object obj : objects) {
				writeMsg.writeObject(obj);
			}
			writeMsg.finish();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public void close() throws IOException {
		getSendPort().close();
		setConnected(false);
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

	public void addReceivePortName(String recvPortName) {
		receivePortNames.add(recvPortName);
	}

	public Vector<String> getReceivePortNames() {
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

	protected void finalize() throws Throwable {
		try {
			close();
		} catch (Exception e) {
		} finally {
			super.finalize();
		}
	}
}
