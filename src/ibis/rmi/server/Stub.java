package ibis.rmi.server;

import ibis.ipl.*;
import ibis.io.*;
import ibis.rmi.*;

public class Stub extends RemoteStub {

	transient protected int stubID;
	transient protected SendPort send;
	transient protected ReceivePort reply;
	protected ReceivePortIdentifier skeletonPortId;
	private boolean initialized = false;

	public Stub() {};
    
	public void init(SendPort s, ReceivePort r, int id, ReceivePortIdentifier rpi, boolean initialized) {
	
		stubID = id;
		send = s;
		reply = r;
		skeletonPortId = rpi;
		this.initialized = initialized;
	}
    
	//serialize & deserialize

	public final void initSend() throws IbisIOException {
	    if (! initialized) {
		if (send == null) {
		    send = RTS.createSendPort();
		    send.connect(skeletonPortId);
		    reply = RTS.createReceivePort();
		    reply.enableConnections();
		}
		WriteMessage wm = send.newMessage();
		wm.writeInt(-1);
		wm.writeInt(0);
		wm.writeObject(reply.identifier());
		wm.send();
		wm.finish();

		ReadMessage rm = reply.receive();
		stubID = rm.readInt();
		String stubType = (String) rm.readObject();
		rm.finish();		

		initialized = true;
	    }
	}

	protected void finalize() {
	    // Give up resources.
	    try {
		if (send != null) send.free();
		if (reply != null) reply.forcedClose();
	    } catch(Exception e) {
	    }
	}

//	private void readObject(java.io.ObjectInputStream i) throws java.io.IOException, ClassNotFoundException {
//	    i.defaultReadObject();
//	    try {
//		if (! skeletonPortId.ibis().address().equals(java.net.InetAddress.getLocalHost())) {
//		    initSend();
//		}
//	    } catch (Exception e) {
//	    }
//	}
}
