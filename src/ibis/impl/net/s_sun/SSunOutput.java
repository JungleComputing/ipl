package ibis.ipl.impl.net.s_sun;

import ibis.io.ArrayOutputStream;
import ibis.io.SunSerializationOutputStream;
import ibis.io.SerializationOutputStream;

import ibis.ipl.impl.net.*;

import java.io.IOException;
import java.io.OutputStream;

/**
 * The ID output implementation.
 */
public final class SSunOutput extends NetSerializedOutput {
        public SSunOutput(NetPortType pt, NetDriver driver, NetIO up, String context) throws NetIbisException {
		super(pt, driver, up, context);
	}
        public SerializationOutputStream newSerializationOutputStream() throws NetIbisException {
                OutputStream os = new DummyOutputStream();
		try {
		    return new SunSerializationOutputStream(os);
		} catch(IOException e) {
		    throw new NetIbisException("got exception", e);
		}
        }
        
        private final class DummyOutputStream extends OutputStream {
                public void write(int b) throws IOException {
                        try {
                                subOutput.writeByte((byte)b);
                        } catch (NetIbisException e) {
                                throw new IOException(e.getMessage());
                        }
                }
        }

}
