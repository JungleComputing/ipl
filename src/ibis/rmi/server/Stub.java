package ibis.rmi.server;

import ibis.ipl.*;

public class Stub extends RemoteStub {

    protected int stubID;
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
    
}