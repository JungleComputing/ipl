package ibis.ipl.impl.messagePassing;

import java.util.Vector;

import ibis.ipl.IbisIOException;

class ShadowSendPort extends SendPort {

    boolean	connected;
    boolean	connect_allowed;
    ibis.ipl.impl.messagePassing.ReadMessage cachedMessage = null;
    private ReadFragment cachedFragment = null;
    ibis.ipl.impl.messagePassing.ByteInputStream in;

    int messageCount;
    int quitCount;
    int msgSeqno = -1;	/* Count messages to do fragmentation */

    ReceivePort receivePort;

    static ShadowSendPort createShadowSendPort(String type,
					       String name,
					       String ibisId,
					       int send_cpu,
					       int send_port,
					       int rcve_port,
					       int serializationType)
	    throws IbisIOException {
	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.err.println("********** Create a ShadowSendPort, my type is " + serializationType);
	}
	switch (serializationType) {
	case ibis.ipl.impl.messagePassing.PortType.SERIALIZATION_NONE:
	    return new ShadowSendPort(type, name, ibisId, send_cpu, send_port, rcve_port);

	case ibis.ipl.impl.messagePassing.PortType.SERIALIZATION_SUN:
	    return new SerializeShadowSendPort(type, name, ibisId, send_cpu, send_port, rcve_port);

	case ibis.ipl.impl.messagePassing.PortType.SERIALIZATION_MANTA:
	    return new MantaShadowSendPort(type, name, ibisId, send_cpu, send_port, rcve_port);

	default:
	    throw new IbisIOException("No such serialization type " + serializationType);
	}
    }


    /* Create a shadow SendPort, used by the local ReceivePort to refer to */
    ShadowSendPort(String type,
		   String name,
		   String ibisId,
		   int send_cpu,
		   int send_port,
		   int rcve_port)
	    throws IbisIOException {
	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.err.println("In ShadowSendPort.<init>");
	}
	ident = new SendPortIdentifier(name, type, ibisId, send_cpu, send_port);
	ibis.ipl.impl.messagePassing.Ibis.myIbis.bindSendPort(this, ident.cpu, ident.port);
	receivePort = ibis.ipl.impl.messagePassing.Ibis.myIbis.lookupReceivePort(rcve_port);
	if (! receivePort.type.name().equals(type)) {
	    System.err.println("********************** ShadowSendPort type does not equal connected ReceivePort type");
	    throw new IbisIOException("************************** Want to connect send port and receive port of different types: " + type + " <-> " + receivePort.type.name());
	}
	this.type = receivePort.type;
	connected = false;

	connect_allowed = receivePort.connect(this);
	if (! connect_allowed) {
	    ibis.ipl.impl.messagePassing.Ibis.myIbis.unbindSendPort(this);
	}
	in = ibis.ipl.impl.messagePassing.Ibis.myIbis.createByteInputStream();
	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.err.println(Thread.currentThread() + "Created shadow send port (" + send_cpu + "," + send_port + "), connect to local port " + rcve_port);
	}
    }


    ibis.ipl.impl.messagePassing.ReadMessage getMessage(int msgSeqno)
	    throws IbisIOException {
	ibis.ipl.impl.messagePassing.ReadMessage msg = cachedMessage;

	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.err.println("Get a NONE ReadMessage ");
	}

	if (msg != null) {
	    cachedMessage = null;

	} else {
	    msg = new ibis.ipl.impl.messagePassing.ReadMessage(this, receivePort);
	    if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
		System.err.println("Create a -none- ReadMessage " + msg); 
	    }
	}

	msg.msgSeqno = msgSeqno;
	return msg;
    }


    /* Serialize streams need a complicated x-phase startup because they start reading
     * in the constructor. Provide a handle to create the Serialize streams after we
     * have deposited the first msg/fragment in the queue. */
    boolean checkStarted(ibis.ipl.impl.messagePassing.ReadMessage msg)
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


    protected void ibmp_connect(int cpu,
				int port,
				int my_port,
				String type,
				String ibisId,
				Syncer syncer,
				int serializationType) throws IbisIOException {
	throw new IbisIOException("ShadowSendPort cannot (dis)connect");
    }


    protected void ibmp_disconnect(int cpu,
				   int port,
				   int receiver_port,
				   int messageCount) throws IbisIOException {
	throw new IbisIOException("ShadowSendPort cannot (dis)connect");
    }


    void tickReceive() {
	messageCount++;
	if (messageCount == quitCount) {
	    ibis.ipl.impl.messagePassing.Ibis.myIbis.unbindSendPort(this);
	    receivePort.disconnect(this);
	}
    }


    static void disconnect(int cpu, int port, int receiver_port, int count) {
	ShadowSendPort sp = ibis.ipl.impl.messagePassing.Ibis.myIbis.lookupSendPort(cpu, port);
	ReceivePort rp = ibis.ipl.impl.messagePassing.Ibis.myIbis.lookupReceivePort(receiver_port);
	if (ibis.ipl.impl.messagePassing.Ibis.myIbis.DEBUG) {
	    System.err.println(Thread.currentThread() + "Receive a disconnect call from SendPort " + sp + ", disconnect from RcvePort " + rp + " count " + count + " sp.messageCount " + sp.messageCount);
	}
	if (rp != sp.receivePort) {
	    System.err.println("Try to disconnect from a receive port we're not connected to...");
	    Thread.dumpStack();
	}
	if (sp.messageCount == count) {
	    ibis.ipl.impl.messagePassing.Ibis.myIbis.unbindSendPort(sp);
	    rp.disconnect(sp);
	} else if (sp.messageCount > count) {
	    System.err.println("This cannot happen...");
	    Thread.dumpStack();
	} else {
	    sp.quitCount = count;
	}
    }

}
