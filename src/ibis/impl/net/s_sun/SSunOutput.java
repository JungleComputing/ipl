package ibis.impl.net.s_sun;

import ibis.impl.net.NetBufferedOutputSupport;
import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetSerializedOutput;
import ibis.io.SerializationOutputStream;
import ibis.io.SunSerializationOutputStream;

import java.io.IOException;
import java.io.OutputStream;

/**
 * The Sun serialization driver output implementation.
 */
public final class SSunOutput extends NetSerializedOutput {

	private NetBufferedOutputSupport bufferOutput;

        public SSunOutput(NetPortType pt, NetDriver driver, String context) throws IOException {
		super(pt, driver, context);
	}

        public SerializationOutputStream newSerializationOutputStream() throws IOException {
                OutputStream os = new DummyOutputStream();
		return new SunSerializationOutputStream(os);
        }

	public void setupConnection(NetConnection cnx) throws IOException {
	    super.setupConnection(cnx);
	    if (subOutput instanceof NetBufferedOutputSupport) {
		bufferOutput = (NetBufferedOutputSupport)subOutput;
		if (! bufferOutput.writeBufferedSupported()) {
		    bufferOutput = null;
		}
	    } else {
		bufferOutput = null;
	    }
	}


        private final class DummyOutputStream extends OutputStream {

	    public void write(int b) throws IOException {
		subOutput.writeByte((byte)b);
	    }

	    public void write(byte[] data, int offset, int length)
		    throws IOException {

		if (bufferOutput != null) {
		    bufferOutput.writeBuffered(data, offset, length);
		    return;
		}

		super.write(data, offset, length);
	    }

	    public void write(byte[] data)
		    throws IOException {

		if (bufferOutput != null) {
		    bufferOutput.writeBuffered(data, 0, data.length);
		    return;
		}

		super.write(data);
	    }

	    public void flush() throws IOException {
		if (bufferOutput != null) {
		    bufferOutput.flushBuffer();
		}
	    }

        }

}
