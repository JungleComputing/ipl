package ibis.impl.messagePassing;

import ibis.ipl.PortMismatchException;

import ibis.io.Conversion;

import java.io.IOException;
import java.io.StreamCorruptedException;

class ShadowSendPort extends SendPort {

    ReadMessage cachedMessage = null;

    private boolean	connect_allowed;
    private ReadFragment cachedFragment = null;

    ByteInputStream in;

    private int messageCount;
    private int quitCount;

    int msgSeqno = -1;	/* Count messages to do fragmentation */

    protected ReceivePort receivePort;

    static ShadowSendPort createShadowSendPort(byte[] rcvePortBuf,
					       byte[] sendPortBuf,
					       int group)
	    throws IOException {

// System.err.println("createShadowSendPort: rcvePortBuf[" + rcvePortBuf.length + "] sendPortBuf[" + sendPortBuf.length + "]");
	ReceivePortIdentifier rId;
	SendPortIdentifier sId;

	try {
	    rId = (ReceivePortIdentifier)Conversion.byte2object(rcvePortBuf);
	    sId = (SendPortIdentifier)Conversion.byte2object(sendPortBuf);
// System.err.println("Create shadow SendPort (group " + group + ") for sendPort that belongs to Ibis " + sId);
	} catch (ClassNotFoundException e) {
	    throw new IOException("Cannot read Ids from stream " + e);
	}

	PortType portType = Ibis.myIbis.getPortTypeLocked(sId.type());
// System.err.println("Sender port type " + sId.type + " = " + portType);
	int serializationType = portType.serializationType;
	if (Ibis.DEBUG) {
	    System.err.println("********** Create a ShadowSendPort, my type is " + serializationType);
	}
	switch (serializationType) {
	case PortType.SERIALIZATION_NONE:
	    return new ShadowSendPort(rId, sId, group);

	case PortType.SERIALIZATION_SUN:
	    return new SerializeShadowSendPort(rId, sId, group);

	case PortType.SERIALIZATION_IBIS:
	    return new IbisShadowSendPort(rId, sId, group);

	default:
	    throw new Error("No such serialization type " + serializationType);
	}
    }


    /* Create a shadow SendPort, used by the local ReceivePort to refer to */
    ShadowSendPort(ReceivePortIdentifier rId, SendPortIdentifier sId, int group)
	    throws IOException {

	if (Ibis.DEBUG) {
	    System.err.println("In ShadowSendPort.<init>");
	}

	ident = sId;
	Ibis.myIbis.bindSendPort(this, ident.cpu, ident.port);

	receivePort = Ibis.myIbis.lookupReceivePort(rId.port);
	if (! rId.type().equals(ident.type())) {
	    System.err.println("********************** ShadowSendPort type does not equal connected ReceivePort type");
	    throw new PortMismatchException("Cannot connect send port and receive port of different types: " + type + " <-> " + receivePort.identifier().type());
	}
	this.type = receivePort.type();

	connect_allowed = receivePort.connect(this);
	if (! connect_allowed) {
	    Ibis.myIbis.unbindSendPort(ident.cpu, ident.port);
System.err.println(this + ": cannot connect to local ReceivePort " + receivePort);
	} else if (group != NO_BCAST_GROUP) {
	    if (Ibis.DEBUG) {
		System.err.println("Bind group " + group + " to port " + rId.port + "; sender " + sId.cpu + " port " + sId.port);
	    }
	    Ibis.myIbis.bindGroup(group, receivePort, this);
	}

	in = new ByteInputStream();
	if (Ibis.DEBUG) {
	    System.err.println(Thread.currentThread() + "Created shadow send port " + this + " (" + sId.cpu + "," + sId.port + "), connect to local port " + rId.port);
	}
    }


    /* Called from native */
    static void bindGroup(int src_cpu, byte[] sendPortBuf, int group)
	    throws IOException {

	SendPortIdentifier sId;

	try {
	    sId = (SendPortIdentifier)Conversion.byte2object(sendPortBuf);
	} catch (ClassNotFoundException e) {
	    throw new IOException("Cannot read Ids from stream " + e);
	}
	ShadowSendPort ssp = Ibis.myIbis.lookupSendPort(sId.cpu, sId.port);
	if (ssp == null) {
	    throw new IOException("Cannot locate ShadowSendPort " + sId);
	}
	if (Ibis.DEBUG) {
	    System.err.println("Bind/later group " + group + " to port " + ((ReceivePortIdentifier)ssp.receivePort.identifier()).port + "; sender " + sId.cpu + " port " + sId.port);
	}
	Ibis.myIbis.bindGroup(group, ssp.receivePort, ssp);
    }


    ReadMessage getMessage(int msgSeqno) throws IOException {
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


    /* Sun-Serialize streams need a complicated x-phase startup because they
     * start reading in the constructor. Provide a handle to create the
     * Sun-Serialize streams after we have deposited the first msg/fragment
     * in the queue. */
    boolean checkStarted(ReadMessage msg) throws IOException {
	return true;
    }


    void setMsgHandle(ReadMessage msg) {
	in.setMsgHandle(msg);
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
				Syncer syncer,
				int group)
	    throws IOException {
	throw new IOException("ShadowSendPort cannot (dis)connect");
    }


    protected void ibmp_disconnect(int remoteCPU,
				   byte[] rcvePortId,
				   byte[] sendPortId,
				   int messageCount)
	    throws IOException {
	throw new IOException("ShadowSendPort cannot (dis)connect");
    }


    void disconnect() throws IOException {
    }


    void tickReceive() {
	messageCount++;
	if (messageCount == quitCount) {
	    Ibis.myIbis.unbindSendPort(ident.cpu, ident.port);
	    receivePort.disconnect(this);
	}
    }


    static void disconnect(byte[] rcvePortId, byte[] sendPortId, int count)
	    throws IOException {
	ReceivePortIdentifier rId = null;
	SendPortIdentifier sId = null;
	try {
	    rId = (ReceivePortIdentifier)Conversion.byte2object(rcvePortId);
	    sId = (SendPortIdentifier)Conversion.byte2object(sendPortId);
	} catch (ClassNotFoundException e) {
	    throw new IOException("Cannot read Ids from stream " + e);
	}

	ShadowSendPort sp = Ibis.myIbis.lookupSendPort(sId.cpu, sId.port);
	ReceivePort rp = Ibis.myIbis.lookupReceivePort(rId.port);

	if (Ibis.DEBUG) {
	    System.err.println(Thread.currentThread() + "Receive a disconnect call from SendPort " + sp + ", disconnect from RcvePort " + rp + " count " + count + " sp.messageCount " + sp.messageCount);
	}
	if (rp != sp.receivePort) {
	    System.err.println("Try to disconnect from a receive port we're not connected to...");
	    Thread.dumpStack();
	}
	if (sp.messageCount == count) {
	    sp.disconnect();
	    Ibis.myIbis.unbindSendPort(sp.ident.cpu, sp.ident.port);
	    rp.disconnect(sp);
	} else if (sp.messageCount > count) {
	    throw new StreamCorruptedException("This cannot happen...");
	} else {
	    sp.quitCount = count;
	}
    }

}
