package ibis.ipl.impl.net.s_sun;

import ibis.io.ArrayInputStream;
import ibis.io.SunSerializationInputStream;
import ibis.io.SerializationInputStream;

import ibis.ipl.impl.net.*;

import java.io.InputStream;
import java.io.IOException;

/**
 * The ID input implementation.
 */
public final class SSunInput extends NetSerializedInput {
        public SSunInput(NetPortType pt, NetDriver driver, NetIO up, String context) throws NetIbisException {
                super(pt, driver, up, context);
        }
        
        public SerializationInputStream newSerializationInputStream() throws NetIbisException {
                InputStream is = new DummyInputStream();
		try {
		    return new SunSerializationInputStream(is);
		} catch(java.io.IOException e) {
		    throw new NetIbisException("got exception", e);
		}
        }

        private final class DummyInputStream extends InputStream {
                public int read() throws IOException {
                        int result = 0;
                        
                        try {
                                result = subInput.readByte();
                        } catch (NetIbisException e) {
                                throw new IOException(e.getMessage());
                        }

                        return (result & 255);
                }
        }        
}
