package ibis.rmi.server;

import ibis.ipl.*;
import ibis.io.*;
import ibis.rmi.*;

public class Stub extends RemoteStub {

	transient protected int stubID;
	transient protected SendPort send;
	transient protected ReceivePort reply;
	protected ReceivePortIdentifier skeletonPortId;

	public Stub() {};
    
	public void init(SendPort s, ReceivePort r, int id, ReceivePortIdentifier rpi) {
	
		stubID = id;
		send = s;
		reply = r;
//	setRef(this, new UnicastRef(rpi));
		skeletonPortId = rpi;
	}
    
	//serialize & deserialize

	private void readObject(java.io.ObjectInputStream in) throws java.io.IOException { 
		try { 
			in.defaultReadObject();

			send = RTS.createSendPort();
			send.connect(skeletonPortId);
			
			reply = RTS.createReceivePort();
			reply.enableConnections();
// System.err.println("rmi.server.Stub: create receive port " + reply);
			
			WriteMessage wm = send.newMessage();
			
			wm.writeInt(-1);
			wm.writeInt(0);
			wm.writeObject(reply.identifier());
			wm.send();
			wm.finish();
// System.err.println("rmi.server.Stub: sent id");
			
			ReadMessage rm = reply.receive();
// System.err.println("rmi.server.Stub: received reply " + rm);
			stubID = rm.readInt();
			String stubType = (String) rm.readObject();
			rm.finish();		
		} catch (Exception e) { 
			throw new java.io.IOException("EEK in readObject " + e);
		} 
	} 
}
