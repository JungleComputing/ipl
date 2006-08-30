/* $Id$ */

package ibis.impl.messagePassing;

import ibis.io.DataSerializationOutputStream;
import ibis.io.IbisSerializationOutputStream;
import ibis.io.Replacer;

import java.io.IOException;

/**
 * messagePassing implementation of SendPort that uses Ibis serialization
 */
final public class IbisSendPort extends SendPort {

    DataSerializationOutputStream obj_out;

    public IbisSendPort(PortType type, String name) throws IOException {
        super(type, name);
        if (type.serializationType == PortType.SERIALIZATION_DATA) {
            obj_out = new DataSerializationOutputStream(out);
        }
        else {
            obj_out = new IbisSerializationOutputStream(out);
        }
        out.setAllocator(obj_out.getAllocator());

        if (Ibis.DEBUG) {
            System.err.println(
                    ">>>>>>>>>>>>>>>> Create a IbisSerializationOutputStream "
                    + obj_out + " for IbisWriteMessage " + this);
        }
    }

    public void setReplacer(Replacer r) throws IOException {
        obj_out.setReplacer(r);
    }

    ibis.ipl.WriteMessage cachedMessage() throws IOException {
        if (message == null) {
            message = new IbisWriteMessage(this);
        }

        return message;
    }

}
