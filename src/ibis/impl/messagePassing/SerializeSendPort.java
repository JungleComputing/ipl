package ibis.ipl.impl.messagePassing;

import java.io.BufferedOutputStream;
import java.io.ObjectOutputStream;

import ibis.ipl.IbisIOException;
import ibis.io.Replacer;

final public class SerializeSendPort extends SendPort {

    ibis.io.SunSerializationOutputStream obj_out;

    SerializeSendPort() {
    }

    public SerializeSendPort(PortType type, String name, OutputConnection conn, Replacer r) throws IbisIOException {
	super(type, name, conn, r,
	      true,	/* syncMode */
	      true	/* makeCopy */);
	if (Ibis.DEBUG) {
	    System.err.println("/////////// Created a new SerializeSendPort " + this);
	}
    }


    public void connect(ibis.ipl.ReceivePortIdentifier receiver,
			int timeout)
	    throws IbisIOException {

	// Reset all our previous connections so the
	// ObjectStream(BufferedStream()) may go through a stop/restart.
	if (obj_out != null) {
	    try {
		obj_out.reset();
	    } catch (java.io.IOException e) {
		throw new IbisIOException(e);
	    }
	}

	Ibis.myIbis.lock();
	try {

	    // Add the new receiver to our tables.
	    int my_split = addConnection((ReceivePortIdentifier)receiver);

	    byte[] sf = ident.getSerialForm();
	    for (int i = 0; i < my_split; i++) {
		ReceivePortIdentifier r = splitter[i];
		outConn.ibmp_disconnect(r.cpu,
					r.getSerialForm(),
					sf,
					messageCount);
	    }
	    messageCount = 0;

	    for (int i = 0; i < splitter.length; i++) {
		ReceivePortIdentifier r = splitter[i];
		if (Ibis.DEBUG) {
		    System.err.println(Thread.currentThread() + "Now do native connect call to " + r + "; me = " + ident);
		    System.err.println("Ibis.myIbis " + Ibis.myIbis);
		    System.err.println("Ibis.myIbis.identifier() " + Ibis.myIbis.identifier());
		    System.err.println("Ibis.myIbis.identifier().name() " + Ibis.myIbis.identifier().name());
		}
		outConn.ibmp_connect(r.cpu,
				     r.getSerialForm(),
				     ident.getSerialForm(),
				     i == my_split ? syncer[i] : null);
		if (Ibis.DEBUG) {
		    System.err.println(Thread.currentThread() + "Done native connect call to " + r + "; me = " + ident);
		}
	    }

	    if (! syncer[my_split].s_wait(timeout)) {
		throw new ibis.ipl.IbisConnectionTimedOutException("No connection to " + receiver);
	    }
	    if (! syncer[my_split].accepted) {
		throw new ibis.ipl.IbisConnectionRefusedException("No connection to " + receiver);
	    }
	} finally {
	    Ibis.myIbis.unlock();
	}

	try {
	    obj_out = new ibis.io.SunSerializationOutputStream(new BufferedOutputStream((java.io.OutputStream)out));
	    if (replacer != null) {
		obj_out.setReplacer(replacer);
	    }
	    if (message != null) {
		((SerializeWriteMessage)message).obj_out = obj_out;
	    }
	    obj_out.flush();
	    Ibis.myIbis.lock();
	    try {
		out.send(true);
		out.reset(true);
	    } finally {
		Ibis.myIbis.unlock();
	    }
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
	if (Ibis.DEBUG) {
	    System.err.println(Thread.currentThread() + ">>>>>>>>>>>> Created ObjectOutputStream " + obj_out + " on top of " + out);
	}
    }


    ibis.ipl.WriteMessage cachedMessage() throws IbisIOException {
	if (message == null) {
	    message = new SerializeWriteMessage(this);
	}

	return message;
    }

}
