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

	public void generated_WriteObject(IbisSerializationOutputStream mantaoutputstream)
		throws IbisIOException
		{
			super.generated_WriteObject(mantaoutputstream);
			mantaoutputstream.writeObject(skeletonPortId);
		}

	public Stub(IbisSerializationInputStream mantainputstream)
		throws IbisIOException
		{
			super(mantainputstream);
			skeletonPortId = (ReceivePortIdentifier)mantainputstream.readObject();

			//after deserialization connect the stub to the skeleton	
			send = RTS.createSendPort();
			send.connect(skeletonPortId);
	
			reply = RTS.createReceivePort();
			reply.enableConnections();
	
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
		}

	private void readObject(java.io.ObjectInputStream in) throws java.io.IOException { 
		try { 
			in.defaultReadObject();
		
			send = RTS.createSendPort();
			send.connect(skeletonPortId);
			
			reply = RTS.createReceivePort();
			reply.enableConnections();
			
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
		} catch (Exception e) { 
			throw new java.io.IOException("EEK in readObject " + e);
		} 
	} 
}
