package ibis.ipl.impl.messagePassing;

import java.util.Vector;

import ibis.ipl.IbisIOException;

class ShadowSendPort extends SendPort {

    boolean	connected;
    boolean	connect_allowed;
    ReadMessage cachedMessage = null;
    private ReadFragment cachedFragment = null;
    ByteInputStream in;

    int messageCount;
    int quitCount;
    int msgSeqno = -1;	/* Count messages to do fragmentation */

    ReceivePort receivePort;

    static ShadowSendPort createShadowSendPort(byte[] rcvePortBuf,
					       byte[] sendPortBuf)
	    throws IbisIOException {

// System.err.println("createShadowSendPort: rcvePortBuf[" + rcvePortBuf.length + "] sendPortBuf[" + sendPortBuf.length + "]");
	ReceivePortIdentifier rId;
	SendPortIdentifier sId;
	rId = (ReceivePortIdentifier)SerializeBuffer.readObject(rcvePortBuf);
	sId = (SendPortIdentifier)SerializeBuffer.readObject(sendPortBuf);

	PortType portType = Ibis.myIbis.getPortTypeLocked(sId.type);
// System.err.println("Sender port type " + sId.type + " = " + portType);
	int serializationType = portType.serializationType;
	if (Ibis.DEBUG) {
	    System.err.println("********** Create a ShadowSendPort, my type is " + serializationType);
	}
	switch (serializationType) {
	case PortType.SERIALIZATION_NONE:
	    return new ShadowSendPort(rId, sId);

	case PortType.SERIALIZATION_SUN:
	    return new SerializeShadowSendPort(rId, sId);

	case PortType.SERIALIZATION_IBIS:
	    return new IbisShadowSendPort(rId, sId);

	default:
	    throw new IbisIOException("No such serialization type " + serializationType);
	}
    }


    /* Create a shadow SendPort, used by the local ReceivePort to refer to */
    ShadowSendPort(ReceivePortIdentifier rId, SendPortIdentifier sId)
	    throws IbisIOException {

	if (Ibis.DEBUG) {
	    System.err.println("In ShadowSendPort.<init>");
	}

	ident = sId;
	Ibis.myIbis.bindSendPort(this, ident.cpu, ident.port);

	receivePort = Ibis.myIbis.lookupReceivePort(rId.port);
	if (! rId.type().equals(ident.type())) {
	    System.err.println("********************** ShadowSendPort type does not equal connected ReceivePort type");
	    throw new IbisIOException("************************** Want to connect send port and receive port of different types: " + type + " <-> " + receivePort.type.name());
	}
	this.type = receivePort.type;
	connected = false;

	connect_allowed = receivePort.connect(this);
	if (! connect_allowed) {
	    Ibis.myIbis.unbindSendPort(ident.cpu, ident.port);
	}
	in = new ByteInputStream();
	if (Ibis.DEBUG) {
	    System.err.println(Thread.currentThread() + "Created shadow send port (" + sId.cpu + "," + sId.port + "), connect to local port " + rId.port);
	}
    }


    ReadMessage getMessage(int msgSeqno) throws IbisIOException {
	ReadMessage msg = cachedMessage;

	if (Ibis.DEBUG) {
	    System.err.println("Get a NONE ReadMessage ");
	}

	if (msg != null) {
	    cachedMessage = null;

	} else {
	    msg = new ReadMessage(this, receivePort);
	    if (Ibis.DEBUG) {
		System.err.println("Create a -none- ReadMessage " + msg); 
	    }
	}

	msg.msgSeqno = msgSeqno;
	return msg;
    }


    /* Serialize streams need a complicated x-phase startup because they start reading
     * in the constructor. Provide a handle to create the Serialize streams after we
     * have deposited the first msg/fragment in the queue. */
    boolean checkStarted(ReadMessage msg)
	    throws IbisIOException {
	return true;
    }


    ReadFragment getFragment() {
	ReadFragment f = cachedFragment;
	if (f == null) {
	    return new ReadFragment();
	}

	cachedFragment = f.next;
	f.next = null;

	return f;
    }


    void putFragment(ReadFragment f) {
	f.next = cachedFragment;
	cachedFragment = f;
    }


    protected void ibmp_connect(int remoteCPU,
				byte[] rcvePortId,
				byte[] sendPortId,
				Syncer syncer)
	    throws IbisIOException {
	throw new IbisIOException("ShadowSendPort cannot (dis)connect");
    }


    protected void ibmp_disconnect(int remoteCPU,
				   byte[] rcvePortId,
				   byte[] sendPortId,
				   int messageCount)
	    throws IbisIOException {
	throw new IbisIOException("ShadowSendPort cannot (dis)connect");
    }


    void tickReceive() {
	messageCount++;
	if (messageCount == quitCount) {
	    Ibis.myIbis.unbindSendPort(ident.cpu, ident.port);
	    receivePort.disconnect(this);
	}
    }


    static void disconnect(byte[] rcvePortId, byte[] sendPortId, int count) {
	ReceivePortIdentifier rId = null;
	SendPortIdentifier sId = null;
	try {
	    rId = (ReceivePortIdentifier)SerializeBuffer.readObject(rcvePortId);
	    sId = (SendPortIdentifier)SerializeBuffer.readObject(sendPortId);
	} catch (IbisIOException e) {
	    System.err.println("Cannot deserialize PortIdentifiers");
	    Thread.dumpStack();
	    return;
	}

	ShadowSendPort sp = Ibis.myIbis.lookupSendPort(sId.cpu, sId.port);
	ReceivePort rp = Ibis.myIbis.lookupReceivePort(rId.port);

	if (Ibis.myIbis.DEBUG) {
	    System.err.println(Thread.currentThread() + "Receive a disconnect call from SendPort " + sp + ", disconnect from RcvePort " + rp + " count " + count + " sp.messageCount " + sp.messageCount);
	}
	if (rp != sp.receivePort) {
	    System.err.println("Try to disconnect from a receive port we're not connected to...");
	    Thread.dumpStack();
	}
	if (sp.messageCount == count) {
	    Ibis.myIbis.unbindSendPort(sp.ident.cpu, sp.ident.port);
	    rp.disconnect(sp);
	} else if (sp.messageCount > count) {
	    System.err.println("This cannot happen...");
	    Thread.dumpStack();
	} else {
	    sp.quitCount = count;
	}
    }

}
