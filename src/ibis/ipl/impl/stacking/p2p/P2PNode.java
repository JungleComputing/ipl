package ibis.ipl.impl.stacking.p2p;

import java.io.IOException;
import java.io.Serializable;

import ibis.ipl.ConnectionFailedException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;
import ibis.ipl.support.vivaldi.Coordinates;

/**
 * encapsulation of node identification in overlay network node ID - ID in
 * overlay network - compute SHA-1 of IP address basis node ID - ID in
 * underlying network distance according with proximity metric
 * 
 * @author delia
 * 
 */
public class P2PNode implements Serializable{
	private static final long serialVersionUID = 1L;
	private IbisIdentifier ibisID;
	private Coordinates coords;
	private P2PIdentifier p2pID;
	private SendPort sendPort;
	
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
	
	public void setSendPort(SendPort sendPort) {
		this.sendPort = sendPort;
	}
	
	public void connect(IbisIdentifier myID) throws ConnectionFailedException {
		System.out.println(myID.name() + " ma conectez la: " + ibisID.name());
		sendPort.connect(ibisID, "p2p");
	}

	public void sendObject(Object msg) throws IOException {
		WriteMessage writeMsg = sendPort.newMessage();
		writeMsg.writeObject(msg);
		writeMsg.finish();
	}
	
	public void sendObjects(Object ... msg) throws IOException {
		WriteMessage writeMsg = sendPort.newMessage();
		for (int i = 0; i < msg.length; i++) {
			writeMsg.writeObject(msg[i]);
		}
		writeMsg.finish();
	}
	
	public void sendArray(Object[] msg) throws IOException {
		WriteMessage writeMsg = sendPort.newMessage();
		writeMsg.writeArray(msg);
		writeMsg.finish();
	}
	
	public void sendInt(int msg) throws IOException {
		WriteMessage writeMsg = sendPort.newMessage();
		writeMsg.writeInt(msg);
		writeMsg.finish();
	}
}
