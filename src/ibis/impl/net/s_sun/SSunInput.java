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

        public SSunInput(NetPortType pt, NetDriver driver, String context) throws IOException {
                super(pt, driver, context);
        }
        
        public SerializationInputStream newSerializationInputStream() throws IOException {
                InputStream is = new DummyInputStream();
		return new SunSerializationInputStream(is);
        }

        private final class DummyInputStream extends InputStream {
                public int read() throws IOException {
                        int result = 0;
                        
			result = subInput.readByte();

                        return (result & 255);
                }
        }        
}
