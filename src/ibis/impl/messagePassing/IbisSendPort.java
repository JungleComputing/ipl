package ibis.impl.messagePassing;

import ibis.io.IbisSerializationOutputStream;
import ibis.ipl.Replacer;

import java.io.IOException;

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
	// obj_out = new IbisSerializationOutputStream(new ArrayOutputStream(out));
	obj_out = new IbisSerializationOutputStream(out);
	if (replacer != null) {
	    obj_out.setReplacer(replacer);
	}
	out.setAllocator(obj_out.getAllocator());

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
