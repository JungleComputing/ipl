package ibis.impl.messagePassing;

import java.io.IOException;

import ibis.ipl.IbisException;

import ibis.io.IbisSerializationInputStream;

final class IbisShadowSendPort extends ShadowSendPort {

    IbisSerializationInputStream obj_in;

    /* Create a shadow SendPort, used by the local ReceivePort to refer to */
    IbisShadowSendPort(ReceivePortIdentifier rId, SendPortIdentifier sId)
	    throws IOException {
	super(rId, sId);
// System.err.println("In IbisShadowSendPort.<init>");
	obj_in = new IbisSerializationInputStream(new ArrayInputStream(in));
    }


    ReadMessage getMessage(int msgSeqno) {
	ReadMessage msg = cachedMessage;

	if (Ibis.DEBUG) {
	    System.err.println("Get a Ibis ReadMessage ");
	    System.err.println(" >>>>>> >>>>>>> >>>>>>> Don't forget to set the stream in the ReadMessage");
	}

	if (msg != null) {
	    cachedMessage = null;

	} else {
	    msg = new IbisReadMessage(this, receivePort);
	    if (Ibis.DEBUG) {
		System.err.println("Create an -ibis-serialization- ReadMessage " + msg); 
	    }
	}

	msg.msgSeqno = msgSeqno;
	return msg;
    }

}
