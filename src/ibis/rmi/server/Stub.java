package ibis.rmi.server;

import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;
import ibis.rmi.RTS;

import java.io.IOException;

public class Stub extends RemoteStub {

    transient protected int stubID;
    transient protected SendPort send;
    transient protected ReceivePort reply;
    protected ReceivePortIdentifier skeletonPortId;
    protected int skeletonId;
    transient private boolean initialized = false;

    public Stub() {
    }

    public void init(SendPort s, ReceivePort r, int id, int skelId, ReceivePortIdentifier rpi, boolean initialized) throws IOException {

	stubID = id;
	skeletonPortId = rpi;
	this.initialized = initialized;
	send = s;
	reply = r;
	skeletonId = skelId;
    }

    public final void initSend() throws IOException {
	if (! initialized) {
	    synchronized(this) {
		if (! initialized) {
		    if (send == null) {
// System.out.println("Setting up connection for " + this);
			send = RTS.getSendPort(skeletonPortId);
			reply = RTS.getReceivePort();
		    }
		    WriteMessage wm = newMessage();
		    wm.writeInt(-1);
		    wm.writeInt(0);
		    wm.writeObject(reply.identifier());
		    wm.send();
		    wm.finish();

		    ReadMessage rm = reply.receive();
		    stubID = rm.readInt();
		    rm.readInt();
		    String stubType = rm.readString();
		    rm.finish();		

		    initialized = true;
		}
	    }
	}
    }

    public final WriteMessage newMessage() throws IOException {
	WriteMessage w = send.newMessage();
	w.writeInt(skeletonId);
	return w;
    }

    protected void finalize() {
	// Give up resources.
	try {
	    /* if (send != null) send.free(); */
	    if (reply != null) {
		RTS.putReceivePort(reply);
	    }
	} catch(Exception e) {
	}
    }
}
