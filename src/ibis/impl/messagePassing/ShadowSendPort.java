package ibis.ipl.impl.messagePassing;

import java.util.Vector;
import java.io.IOException;

import java.io.BufferedInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectInput;

import ibis.ipl.ConditionVariable;
import ibis.ipl.IbisException;

class ShadowSendPort extends SendPort {

    ByteInputStream in;	// Only for the shadow SendPort
    ibis.io.ArrayInputStream a_in;	// Only for the shadow SendPort
    ObjectInput obj_in;		// Only for the shadow SendPort
    boolean	connected;
    boolean	connect_allowed;
    ibis.ipl.impl.messagePassing.ReadMessage cachedMessage = null;

    int messageCount;
    int quitCount;

    ReceivePort receivePort;

    /* Create a shadow SendPort, used by the local ReceivePort to refer to */
    ShadowSendPort(String type, String name, String ibisId,
			int send_cpu, int send_port,
		        int rcve_port) throws IbisException {
System.err.println("In ShadowSendPort.<init>");
	ident = new SendPortIdentifier(name, type, ibisId, send_cpu, send_port);
	ibis.ipl.impl.messagePassing.Ibis.myIbis.bindSendPort(this, ident.cpu, ident.port);
	receivePort = ibis.ipl.impl.messagePassing.Ibis.myIbis.lookupReceivePort(rcve_port);
	if (! receivePort.type.name().equals(type)) {
	    System.err.println("********************** ShadowSendPort type does not equal connected ReceivePort type");
	    throw new IbisException("************************** Want to connect send port and receive port of different types: " + type + " <-> " + receivePort.type.name());
	}
	this.type = receivePort.type;
	connected = false;

	switch (this.type.serializationType) {
	case ibis.ipl.impl.messagePassing.PortType.SERIALIZATION_NONE:
	case ibis.ipl.impl.messagePassing.PortType.SERIALIZATION_SUN:
	    in = ibis.ipl.impl.messagePassing.Ibis.myIbis.createByteInputStream();
	    break;
	case ibis.ipl.impl.messagePassing.PortType.SERIALIZATION_MANTA:
	    in = ibis.ipl.impl.messagePassing.Ibis.myIbis.createByteInputStream();
	    a_in = ibis.ipl.impl.messagePassing.Ibis.myIbis.createMantaInputStream(in);
	    break;
	default:
	    System.out.println("EEK");
	    System.exit(1);
	}

	connect_allowed = receivePort.connect(this);
	if (! connect_allowed) {
	    ibis.ipl.impl.messagePassing.Ibis.myIbis.unbindSendPort(this);
	}
System.err.println(Thread.currentThread() + "Created shadow send port (" + send_cpu + "," + send_port + "), connect to local port " + rcve_port);
    }


    protected void ibmp_connect(int cpu,
				int port,
				int my_port,
				String type,
				String ibisId,
				Syncer syncer) throws IbisException {
	throw new IbisException("ShadowSendPort cannot (dis)connect");
    }


    protected void ibmp_disconnect(int cpu,
				   int port,
				   int receiver_port,
				   int messageCount) throws IbisException {
	throw new IbisException("ShadowSendPort cannot (dis)connect");
    }


    void checkConnection(ibis.ipl.impl.messagePassing.ReadMessage msg)
	    throws IbisException {
// manta.runtime.RuntimeSystem.DebugMe(this, msg);
	switch (this.type.serializationType) {
	case ibis.ipl.impl.messagePassing.PortType.SERIALIZATION_NONE:
	case ibis.ipl.impl.messagePassing.PortType.SERIALIZATION_SUN:
	case ibis.ipl.impl.messagePassing.PortType.SERIALIZATION_MANTA:
	    in.setPandaMessage(msg.pandaMessage);
// System.err.println(Thread.currentThread() + "Set pandaMessage " + msg.pandaMessage + " in ByteInputStream " + in);
	    break;
	default:
	}

	if (! connected) {
	    switch (this.type.serializationType) {
	    case ibis.ipl.impl.messagePassing.PortType.SERIALIZATION_NONE:
		break;
	    case ibis.ipl.impl.messagePassing.PortType.SERIALIZATION_SUN:
		try {
		    obj_in = new ObjectInputStream(new BufferedInputStream(in));
// System.err.println(Thread.currentThread() + "Past create ObjectInputStream");
		    ((SerializeReadMessage)msg).obj_in = obj_in;
		} catch (IOException e) {
		    throw new IbisException("Cannot create ObjectInputStream " + e);
		}
		break;
	    case ibis.ipl.impl.messagePassing.PortType.SERIALIZATION_MANTA:
		obj_in = new ibis.io.MantaTypedBufferInputStream(a_in);
		((MantaReadMessage)msg).obj_in = (ibis.io.MantaInputStream)obj_in;
		break;
	    default:
		System.out.println("EEK");
		System.exit(1);
	    }

	    connected = true;
	}
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
	// System.err.println(Thread.currentThread() + "Receive a disconnect call from SendPort " + sp + ", disconnect from RcvePort " + rp + " count " + count + " rp.messageCount " + rp.messageCount);
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
