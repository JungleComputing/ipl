package ibis.ipl.impl.messagePassing;

import java.io.BufferedOutputStream;
import java.io.ObjectOutputStream;

import ibis.ipl.IbisIOException;

public class SerializeSendPort extends ibis.ipl.impl.messagePassing.SendPort {

    ObjectOutputStream obj_out;


    SerializeSendPort() {
    }

    public SerializeSendPort(ibis.ipl.impl.messagePassing.PortType type, String name, OutputConnection conn) throws IbisIOException {
	super(type, name, conn,
	      true,	/* syncMode */
	      true	/* makeCopy */);
	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.err.println("/////////// Created a new SerializeSendPort " + this);
	}
    }


    public void connect(ibis.ipl.ReceivePortIdentifier receiver,
			int timeout)
	    throws IbisIOException {

	// synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
	ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	try {

	    // Add the new receiver to our tables.
	    int my_split = addConnection((ReceivePortIdentifier)receiver);

	    // Reset all our previous connections so the
	    // ObjectStream(BufferedStream()) may go through a stop/restart.
	    if (obj_out != null) {
		try {
		    obj_out.close();
		} catch (java.io.IOException e) {
		    throw new IbisIOException(e);
		}
	    }

	    for (int i = 0; i < my_split; i++) {
		ReceivePortIdentifier r = splitter[i];
		outConn.ibmp_disconnect(r.cpu, r.port, ident.port, messageCount);
	    }
	    messageCount = 0;

	    for (int i = 0; i < splitter.length; i++) {
		ReceivePortIdentifier r = splitter[i];
		if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
		    System.err.println(Thread.currentThread() + "Now do native connect call to " + r + "; me = " + ident);
		    System.err.println("ibis.ipl.impl.messagePassing.Ibis.myIbis " + ibis.ipl.impl.messagePassing.Ibis.myIbis);
		    System.err.println("ibis.ipl.impl.messagePassing.Ibis.myIbis.identifier() " + ibis.ipl.impl.messagePassing.Ibis.myIbis.identifier());
		    System.err.println("ibis.ipl.impl.messagePassing.Ibis.myIbis.identifier().name() " + ibis.ipl.impl.messagePassing.Ibis.myIbis.identifier().name());
		}
		outConn.ibmp_connect(r.cpu, r.port, ident.port, ident.type,
				     ibis.ipl.impl.messagePassing.Ibis.myIbis.identifier().name(),
				     i == my_split ? syncer[i] : null,
				     type.serializationType);
		if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
		    System.err.println(Thread.currentThread() + "Done native connect call to " + r + "; me = " + ident);
		}
	    }

	    if (! syncer[my_split].s_wait(timeout)) {
		throw new ibis.ipl.IbisConnectionTimedOutException("No connection to " + receiver);
	    }
	    if (! syncer[my_split].accepted) {
		throw new ibis.ipl.IbisConnectionRefusedException("No connection to " + receiver);
	    }
	// }
	} finally {
	    ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
	}

	try {
	    obj_out = new ObjectOutputStream(new BufferedOutputStream((java.io.OutputStream)out));
	    if (message != null) {
		((SerializeWriteMessage)message).obj_out = obj_out;
	    }
	    obj_out.flush();
	    ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	    try {
		out.send(true);
		out.reset(true);
	    } finally {
		ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
	    }
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
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
