package ibis.ipl.impl.messagePassing;

import ibis.io.IbisSerializationOutputStream;

import ibis.ipl.IbisIOException;
import ibis.io.Replacer;

final public class IbisSendPort extends SendPort {


    IbisSerializationOutputStream obj_out;


    IbisSendPort() {
    }

    public IbisSendPort(PortType type, String name, OutputConnection conn, Replacer r) throws IbisIOException {
	super(type, name, conn, r,
	      false,	/* syncMode */
	      false	/* makeCopy */);
	try {
	    if (replacer != null) {
		obj_out = new IbisSerializationOutputStream(new ArrayOutputStream(out));
		obj_out.setReplacer(replacer);
	    } else {
		obj_out = new IbisSerializationOutputStream(new ArrayOutputStream(out));
	    }
	} catch (java.io.IOException e) {
	    throw new IbisIOException("could not create IbisSerializationOutputStream", e);
	}
	if (Ibis.DEBUG) {
	    System.err.println(">>>>>>>>>>>>>>>> Create a IbisSerializationOutputStream " + obj_out + " for IbisWriteMessage " + this);
	}
    }


    ibis.ipl.WriteMessage cachedMessage() throws IbisIOException {
	if (message == null) {
	    message = new IbisWriteMessage(this);
	}

	return message;
    }

}
