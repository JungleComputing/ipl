package ibis.impl.messagePassing;

import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.ConnectionTimedOutException;
import ibis.ipl.Replacer;

import java.io.BufferedOutputStream;
import java.io.IOException;

final public class SerializeSendPort extends SendPort {

    ibis.io.SunSerializationOutputStream obj_out;

    SerializeSendPort() {
    }

    public SerializeSendPort(PortType type, String name, OutputConnection conn, Replacer r)
	    throws IOException {
	super(type, name, conn, r,
	      true,	/* syncMode */
	      true	/* makeCopy */);
	if (Ibis.DEBUG) {
	    System.err.println("/////////// Created a new SerializeSendPort " + this);
	}
    }


    public void connect(ibis.ipl.ReceivePortIdentifier receiver,
			long timeout)
	    throws IOException {

	// Reset all our previous connections so the
	// ObjectStream(BufferedStream()) may go through a stop/restart.
	if (obj_out != null) {
	    obj_out.reset();
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
		throw new ConnectionTimedOutException("No connection to " + receiver);
	    }
	    if (! syncer[my_split].accepted) {
		throw new ConnectionRefusedException("No connection to " + receiver);
	    }
	} finally {
	    Ibis.myIbis.unlock();
	}

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
	if (Ibis.DEBUG) {
	    System.err.println(Thread.currentThread() + ">>>>>>>>>>>> Created ObjectOutputStream " + obj_out + " on top of " + out);
	}
    }


    ibis.ipl.WriteMessage cachedMessage() throws IOException {
	if (message == null) {
	    message = new SerializeWriteMessage(this);
	}

	return message;
    }

}
