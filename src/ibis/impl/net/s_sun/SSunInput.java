package ibis.impl.net.s_sun;

import ibis.impl.net.NetBufferedInputSupport;
import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetInputUpcall;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetSerializedInput;
import ibis.io.SerializationInputStream;
import ibis.io.SunSerializationInputStream;

import java.io.IOException;
import java.io.InputStream;

/**
 * The Sun serialization driver input implementation.
 */
public final class SSunInput extends NetSerializedInput {

    private NetBufferedInputSupport bufferInput;

    public SSunInput(NetPortType pt, NetDriver driver, String context,
            NetInputUpcall inputUpcall) throws IOException {
        super(pt, driver, context, inputUpcall);
    }

    public SerializationInputStream newSerializationInputStream()
            throws IOException {
        InputStream is = new DummyInputStream();
        return new SunSerializationInputStream(is);
    }

    public void setupConnection(NetConnection cnx) throws IOException {
        super.setupConnection(cnx);
        if (subInput instanceof NetBufferedInputSupport) {
            bufferInput = (NetBufferedInputSupport) subInput;
            if (!bufferInput.readBufferedSupported()) {
                bufferInput = null;
            }
        } else {
            bufferInput = null;
        }
    }

    private final class DummyInputStream extends InputStream {

        public int read() throws IOException {
            int result = subInput.readByte();
            return (result & 255);
        }

        public int read(byte[] data, int offset, int length) throws IOException {

            if (bufferInput != null) {
                // System.err.println("YES!!!");
                return bufferInput.readBuffered(data, offset, length);
            }
            // System.err.println("no..... :-(( subInput " + subInput);

            return super.read(data, offset, length);
        }

        public int read(byte[] data) throws IOException {

            if (bufferInput != null) {
                // System.err.println("YES!!!");
                return bufferInput.readBuffered(data, 0, data.length);
            }
            // System.err.println("no..... :-(( subInput " + subInput);

            return super.read(data);
        }

    }

}
