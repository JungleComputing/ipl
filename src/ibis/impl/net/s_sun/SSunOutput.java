package ibis.impl.net.s_sun;

import ibis.impl.net.NetDriver;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetSerializedOutput;
import ibis.impl.net.NetBufferedOutput;
import ibis.io.SerializationOutputStream;
import ibis.io.SunSerializationOutputStream;

import java.io.IOException;
import java.io.OutputStream;

/**
 * The ID output implementation.
 */
public final class SSunOutput extends NetSerializedOutput {

        public SSunOutput(NetPortType pt, NetDriver driver, String context) throws IOException {
		super(pt, driver, context);
	}

        public SerializationOutputStream newSerializationOutputStream() throws IOException {
                OutputStream os = new DummyOutputStream();
		return new SunSerializationOutputStream(os);
        }

	public boolean writeBufferedSupported() {
	    return subOutput.writeBufferedSupported();
	}

	public void writeBuffered(byte[] data, int offset, int length)
		throws IOException {
	    subOutput.writeBuffered(data, offset, length);
	}
        
        private final class DummyOutputStream extends OutputStream {

                public void write(int b) throws IOException {
			subOutput.writeByte((byte)b);
                }

	    public void write(byte[] data, int offset, int length)
		    throws IOException {

		if (writeBufferedSupported()) {
		    writeBuffered(data, offset, length);
		    return;
		}

		super.write(data, offset, length);
	    }

	    public void flush() throws IOException {
		if (writeBufferedSupported()) {
		    ((NetBufferedOutput)subOutput).flush();
		}
	    }

        }

}
