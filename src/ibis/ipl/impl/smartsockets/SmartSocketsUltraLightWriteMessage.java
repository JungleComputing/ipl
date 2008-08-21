package ibis.ipl.impl.smartsockets;

import java.io.IOException;

import ibis.io.SerializationFactory;
import ibis.io.SerializationOutput;
import ibis.io.SingleBufferArrayOutputStream;
import ibis.ipl.PortType;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

public class SmartSocketsUltraLightWriteMessage implements WriteMessage {

	private final int DEFAULT_BUFFER_SIZE = 4096;
	
	private final SmartSocketsUltraLightSendPort port;
	private final SerializationOutput out;
	private final SingleBufferArrayOutputStream bout;
	
	SmartSocketsUltraLightWriteMessage(SmartSocketsUltraLightSendPort port, byte [] buffer) throws IOException { 
		this.port = port;
		
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

		bout = new SingleBufferArrayOutputStream(buffer);
		out = SerializationFactory.createSerializationOutput(serialization, bout);		
	}
	
	public long bytesWritten() throws IOException {
		return bout.bytesWritten();
	}

	public long finish() throws IOException {
		long bytes = bout.bytesWritten();
		port.finishedMessage();
		return bytes;
	}

	public void finish(IOException exception) {
		try { 
			port.finishedMessage(exception);
		} catch (Exception e) {
			// ignore ? 
		}
	}

	protected void resetBuffers() throws IOException { 
		bout.reset();
		out.reset(true);
	}
	
	public void flush() throws IOException {
		// empty
	}

	public SendPort localPort() {
		return port;
	}

	public void reset() throws IOException {
		// empty
	}

	public int send() throws IOException {
		// empty -- excpetion ? 
		return 0;
	}

	public void sync(int ticket) throws IOException {
		// empty
	}

	public void writeArray(boolean[] value) throws IOException {
		out.writeArray(value);
	}

	public void writeArray(byte[] value) throws IOException {
		out.writeArray(value);
	}

	public void writeArray(char[] value) throws IOException {
		out.writeArray(value);
	}

	public void writeArray(short[] value) throws IOException {
		out.writeArray(value);
	}

	public void writeArray(int[] value) throws IOException {
		out.writeArray(value);
	}

	public void writeArray(long[] value) throws IOException {
		out.writeArray(value);
	}

	public void writeArray(float[] value) throws IOException {
		out.writeArray(value);
	}

	public void writeArray(double[] value) throws IOException {
		out.writeArray(value);
	}

	public void writeArray(Object[] value) throws IOException {
		out.writeArray(value);
	}

	public void writeArray(boolean[] value, int offset, int length) throws IOException {
		out.writeArray(value, offset, length);
	}

	public void writeArray(byte[] value, int offset, int length) throws IOException {
		out.writeArray(value, offset, length);
	}

	public void writeArray(char[] value, int offset, int length) throws IOException {
		out.writeArray(value, offset, length);
	}

	public void writeArray(short[] value, int offset, int length) throws IOException {
		out.writeArray(value, offset, length);
	}

	public void writeArray(int[] value, int offset, int length) throws IOException {
		out.writeArray(value, offset, length);
	}

	public void writeArray(long[] value, int offset, int length) throws IOException {
		out.writeArray(value, offset, length);
	}

	public void writeArray(float[] value, int offset, int length) throws IOException {
		out.writeArray(value, offset, length);
	}

	public void writeArray(double[] value, int offset, int length) throws IOException {
		out.writeArray(value, offset, length);
	}
	
	public void writeArray(Object[] value, int offset, int length) throws IOException {
		out.writeArray(value, offset, length);
	}

	public void writeBoolean(boolean value) throws IOException {
		out.writeBoolean(value);
	}

	public void writeByte(byte value) throws IOException {
		out.writeByte(value);
	}

	public void writeChar(char value) throws IOException {
		out.writeChar(value);		
	}

	public void writeDouble(double value) throws IOException {
		out.writeDouble(value);		
	}

	public void writeFloat(float value) throws IOException {
		out.writeFloat(value);		
	}

	public void writeInt(int value) throws IOException {
		out.writeInt(value);		
	}

	public void writeLong(long value) throws IOException {
		out.writeDouble(value);		
	}

	public void writeObject(Object value) throws IOException {
		out.writeObject(value);		
	}

	public void writeShort(short value) throws IOException {
		out.writeShort(value);				
	}

	public void writeString(String value) throws IOException {
		out.writeString(value);
	}
}
