package ibis.ipl.impl.stacking.p2p;

import java.io.IOException;

import ibis.ipl.ConnectionFailedException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

public class P2PInternalNode implements Comparable<P2PInternalNode>{
	private P2PNode node;
	private SendPort sendPort;
	
	public P2PInternalNode(P2PNode node) {
		this.node = node;
	}
	
	public void setNode(P2PNode node) {
		this.node = node;
	}
	
	public P2PNode getNode() {
		return node;
	}
	
	public void connect(SendPort sendPort) throws ConnectionFailedException {
		this.sendPort = sendPort;
		sendPort.connect(node.getIbisID(), "p2p");
	}

	public void sendObject(Object msg) throws IOException {
		WriteMessage writeMsg = getSendPort().newMessage();
		writeMsg.writeObject(msg);
		writeMsg.finish();
	}
	
	public void sendObjects(Object ... msg) throws IOException {
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

	@Override
	public int compareTo(P2PInternalNode o) {
		return node.compareTo(getNode());
	}
	
}
