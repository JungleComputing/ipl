package ibis.rmi.server;

import ibis.ipl.SendPort;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.WriteMessage;
import ibis.ipl.ReadMessage;
import ibis.ipl.IbisError;
import ibis.rmi.RTS;

import java.io.IOException;

public class Stub extends RemoteStub {

    transient protected int stubID;
    transient protected SendPort send;
    transient protected ReceivePort reply;
    protected ReceivePortIdentifier skeletonPortId;
    transient private boolean initialized = false;

    public Stub() {
    }

    public void init(SendPort s, ReceivePort r, int id, ReceivePortIdentifier rpi, boolean initialized) throws IOException {

	stubID = id;
	skeletonPortId = rpi;
	this.initialized = initialized;
	send = s;
	reply = r;
/*
	if (send == null) {
	    send = RTS.getSendPort(skeletonPortId);
	}
	if (reply == null) {
	    reply = RTS.getReceivePort();
	}
*/
    }

    public final void initSend() throws IOException {
	if (! initialized) {
	    if (send == null) {
// System.out.println("Setting up connection for " + this);
		send = RTS.getSendPort(skeletonPortId);
		reply = RTS.getReceivePort();
	    }
	    WriteMessage wm = send.newMessage();
	    wm.writeInt(-1);
	    wm.writeInt(0);
	    wm.writeObject(reply.identifier());
	    wm.send();
	    wm.finish();

	    ReadMessage rm = reply.receive();
	    stubID = rm.readInt();
	    try {
		String stubType = (String) rm.readObject();
	    } catch(ClassNotFoundException e) {
		throw new IbisError("Class String not found", e);
	    }
	    rm.finish();		

	    initialized = true;
	}
    }

    protected void finalize() {
	// Give up resources.
// System.out.println("Finalize stub: " + this);
	try {
	    /* if (send != null) send.free(); */
	    if (reply != null) {
		RTS.putReceivePort(reply);
	    }
	} catch(Exception e) {
	}
// System.out.println("Stub finalized: " + this);
    }

/*
    private void readObject(java.io.ObjectInputStream i) throws IOException, ClassNotFoundException {
	i.defaultReadObject();

System.out.println("Setting up connection for " + this);

	send = RTS.getSendPort(skeletonPortId);
	reply = RTS.getReceivePort();
    }
*/
}
