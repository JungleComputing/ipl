package ibis.ipl.impl.messagePassing;

import ibis.io.IbisSerializationInputStream;

import ibis.ipl.IbisIOException;

final class IbisShadowSendPort extends ShadowSendPort {

    IbisSerializationInputStream obj_in;

    /* Create a shadow SendPort, used by the local ReceivePort to refer to */
    IbisShadowSendPort(String type,
		        String name,
		        String ibisId,
		        int send_cpu,
		        int send_port,
		        int rcve_port)
	    throws IbisIOException {
	super(type, name, ibisId, send_cpu, send_port, rcve_port);
// System.err.println("In IbisShadowSendPort.<init>");
	obj_in = new IbisSerializationInputStream(new ArrayInputStream(in));
    }


    ibis.ipl.impl.messagePassing.ReadMessage getMessage(int msgSeqno) {
	ibis.ipl.impl.messagePassing.ReadMessage msg = cachedMessage;

	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.err.println("Get a Ibis ReadMessage ");
	    System.err.println(" >>>>>> >>>>>>> >>>>>>> Don't forget to set the stream in the ReadMessage");
	}

	if (msg != null) {
	    cachedMessage = null;

	} else {
	    msg = new IbisReadMessage(this, receivePort);
	    if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
		System.err.println("Create a -manta- ReadMessage " + msg); 
	    }
	}

	msg.msgSeqno = msgSeqno;
	return msg;
    }

}
