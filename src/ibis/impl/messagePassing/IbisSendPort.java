package ibis.ipl.impl.messagePassing;

import ibis.io.IbisSerializationOutputStream;

import ibis.ipl.IbisIOException;
import ibis.io.Replacer;

final public class IbisSendPort extends ibis.ipl.impl.messagePassing.SendPort {


    IbisSerializationOutputStream obj_out;


    IbisSendPort() {
    }

    public IbisSendPort(ibis.ipl.impl.messagePassing.PortType type, String name, OutputConnection conn, Replacer r) throws IbisIOException {
	super(type, name, conn, r,
	      false,	/* syncMode */
	      false	/* makeCopy */);
	if (replacer != null) {
	    obj_out = new IbisSerializationOutputStream(new ArrayOutputStream(out));
	    obj_out.setReplacer(replacer);
	} else {
	    obj_out = new IbisSerializationOutputStream(new ArrayOutputStream(out));
	}
	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
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
