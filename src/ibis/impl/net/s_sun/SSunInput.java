package ibis.ipl.impl.net.s_sun;

import ibis.io.ArrayInputStream;
import ibis.io.SunSerializationInputStream;
import ibis.io.SerializationInputStream;

import ibis.ipl.impl.net.*;

import ibis.ipl.IbisIOException;

import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Hashtable;


/**
 * The ID input implementation.
 */
public final class SSunInput extends NetSerializedInput {
        public SSunInput(NetPortType pt, NetDriver driver, NetIO up, String context) throws IbisIOException {
                super(pt, driver, up, context);
        }
        
        public SerializationInputStream newSerializationInputStream() throws IbisIOException {
                InputStream is = new DummyInputStream();
		try {
		    return new SunSerializationInputStream(is);
		} catch(java.io.IOException e) {
		    throw new IbisIOException("got exception", e);
		}
        }

        private final class DummyInputStream extends InputStream {
                public int read() throws IOException {
                        int result = 0;
                        
                        try {
                                result = subInput.readByte();
                        } catch (IbisIOException e) {
                                throw new IOException(e.getMessage());
                        }

                        return (result & 255);
                }
        }        
}
