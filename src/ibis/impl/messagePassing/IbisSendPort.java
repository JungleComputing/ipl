package ibis.ipl.impl.messagePassing;

import java.io.IOException;

import ibis.ipl.IbisException;
import ibis.io.IbisSerializationOutputStream;
import ibis.io.Replacer;

final public class IbisSendPort extends SendPort {


    IbisSerializationOutputStream obj_out;


    IbisSendPort() {
    }

    public IbisSendPort(PortType type,
			String name,
			OutputConnection conn,
			Replacer r)
	    throws IOException {
	super(type, name, conn, r,
	      false,	/* syncMode */
	      false	/* makeCopy */);
	if (replacer != null) {
	    obj_out = new IbisSerializationOutputStream(new ArrayOutputStream(out));
	    obj_out.setReplacer(replacer);
	} else {
	    obj_out = new IbisSerializationOutputStream(new ArrayOutputStream(out));
	}
	if (Ibis.DEBUG) {
	    System.err.println(">>>>>>>>>>>>>>>> Create a IbisSerializationOutputStream " + obj_out + " for IbisWriteMessage " + this);
	}
    }


    ibis.ipl.WriteMessage cachedMessage() throws IOException {
	if (message == null) {
	    message = new IbisWriteMessage(this);
	}

	return message;
    }

}
