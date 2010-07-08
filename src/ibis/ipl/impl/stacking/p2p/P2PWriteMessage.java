package ibis.ipl.impl.stacking.p2p;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ibis.io.SerializationFactory;
import ibis.io.SerializationOutput;
import ibis.io.SingleBufferArrayOutputStream;
import ibis.ipl.PortType;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

public class P2PWriteMessage implements WriteMessage {
	private final P2PSendPort port;
	private final SerializationOutput out;
	private final SingleBufferArrayOutputStream bout;

	private static final Logger logger = LoggerFactory.getLogger(P2PWriteMessage.class);
	
	public P2PWriteMessage(P2PSendPort sendPort, byte[] buffer)
			throws IOException {
		this.port = sendPort;

		PortType type = port.getPortType();
		String serialization = null;

		if (type.hasCapability(PortType.SERIALIZATION_DATA)) {
			serialization = "data";
		} else if (type.hasCapability(PortType.SERIALIZATION_OBJECT_SUN)) {
			serialization = "sun";
		} else if (type.hasCapability(PortType.SERIALIZATION_OBJECT_IBIS)) {
			serialization = "ibis";
		} else if (type.hasCapability(PortType.SERIALIZATION_OBJECT)) {
			serialization = "object";
		} else {
			serialization = "byte";
		}

		logger.debug("Serialization is " + serialization);
		
		bout = new SingleBufferArrayOutputStream(buffer);
		out = SerializationFactory.createSerializationOutput(serialization,
				bout);
	}

	@Override
	public long bytesWritten() throws IOException {
		return bout.bytesWritten();
	}

	@Override
	public int capacity() throws IOException {
		return bout.bufferSize();
	}

	@Override
	public long finish() throws IOException {

		out.flush();

		long bytes = bout.bytesWritten();

		System.err.println("Written == " + bytes);

		port.finishedMessage();
		return bytes;

	}

	@Override
	public void finish(IOException exception) {
		try {
			// TODO: implement finish
			// port.finishedMessage(exception);

		} catch (Exception e) {
			// ignore ?
		}
	}

	@Override
	public void flush() throws IOException {
		// TODO: in SmartSocketsUltraLightWriteMessage is left empty, do the
		// same?
	}

	@Override
	public SendPort localPort() {
		return port;
	}

	@Override
	public int remaining() throws IOException {
		return (int) (bout.bufferSize() - bout.bytesWritten());
	}

	protected void resetBuffers() throws IOException {
		bout.reset();
		out.reset(true);
	}

	@Override
	public void reset() throws IOException {
		resetBuffers();
	}

	@Override
	public int send() throws IOException {
		// TODO: in SmartSocketsUltraLightWritePort is left empty, do the same?
		return 0;
	}

	@Override
	public void sync(int ticket) throws IOException {
		// TODO: in SmartSocketsUltraLightWritePort is left empty, do the same?
	}

	@Override
	public void writeArray(boolean[] value) throws IOException {
		out.writeArray(value);
	}

	@Override
	public void writeArray(byte[] value) throws IOException {
		out.writeArray(value);

	}

	@Override
	public void writeArray(char[] value) throws IOException {
		out.writeArray(value);

	}

	@Override
	public void writeArray(short[] value) throws IOException {
		out.writeArray(value);

	}

	@Override
	public void writeArray(int[] value) throws IOException {
		out.writeArray(value);

	}

	@Override
	public void writeArray(long[] value) throws IOException {
		out.writeArray(value);

	}

	@Override
	public void writeArray(float[] value) throws IOException {
		out.writeArray(value);
	}

	@Override
	public void writeArray(double[] value) throws IOException {
		out.writeArray(value);
	}

	@Override
	public void writeArray(Object[] value) throws IOException {
		out.writeArray(value);
	}

	@Override
	public void writeArray(boolean[] value, int offset, int length)
			throws IOException {
		out.writeArray(value, offset, length);
	}

	@Override
	public void writeArray(byte[] value, int offset, int length)
			throws IOException {
		out.writeArray(value, offset, length);
	}

	@Override
	public void writeArray(char[] value, int offset, int length)
			throws IOException {
		out.writeArray(value, offset, length);
	}

	@Override
	public void writeArray(short[] value, int offset, int length)
			throws IOException {
		out.writeArray(value, offset, length);
	}

	@Override
	public void writeArray(int[] value, int offset, int length)
			throws IOException {
		out.writeArray(value, offset, length);
	}

	@Override
	public void writeArray(long[] value, int offset, int length)
			throws IOException {
		out.writeArray(value, offset, length);
	}

	@Override
	public void writeArray(float[] value, int offset, int length)
			throws IOException {
		out.writeArray(value, offset, length);

	}

	@Override
	public void writeArray(double[] value, int offset, int length)
			throws IOException {
		out.writeArray(value, offset, length);

	}

	@Override
	public void writeArray(Object[] value, int offset, int length)
			throws IOException {
		out.writeArray(value, offset, length);
	}

	@Override
	public void writeBoolean(boolean value) throws IOException {
		out.writeBoolean(value);
	}

	@Override
	public void writeByte(byte value) throws IOException {
		out.writeByte(value);

	}

	@Override
	public void writeChar(char value) throws IOException {
		out.writeChar(value);
	}

	@Override
	public void writeDouble(double value) throws IOException {
		out.writeDouble(value);
	}

	@Override
	public void writeFloat(float value) throws IOException {
		out.writeFloat(value);
	}

	@Override
	public void writeInt(int value) throws IOException {
		out.writeInt(value);
	}

	@Override
	public void writeLong(long value) throws IOException {
		out.writeLong(value);
	}

	@Override
	public void writeObject(Object value) throws IOException {
		out.writeObject(value);
	}

	@Override
	public void writeShort(short value) throws IOException {
		out.writeShort(value);
	}

	@Override
	public void writeString(String value) throws IOException {
		out.writeString(value);
	}
}
