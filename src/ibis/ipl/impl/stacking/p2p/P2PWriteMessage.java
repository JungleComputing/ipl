package ibis.ipl.impl.stacking.p2p;

import java.io.IOException;

import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

public class P2PWriteMessage implements WriteMessage{
	final WriteMessage base;
	final SendPort port;
	
	public P2PWriteMessage(WriteMessage message, P2PSendPort sendPort) {
		this.base = message;
		this.port = sendPort;
	}
	
	@Override
	public long bytesWritten() throws IOException {
		return base.bytesWritten();
	}

	@Override
	public int capacity() throws IOException {
		return base.capacity();
	}

	@Override
	public long finish() throws IOException {
		return base.finish();
	}

	@Override
	public void finish(IOException exception) {
		base.finish(exception);
	}

	@Override
	public void flush() throws IOException {
		base.flush();		
	}

	@Override
	public SendPort localPort() {
		return port;
	}

	@Override
	public int remaining() throws IOException {
		return base.remaining();
	}

	@Override
	public void reset() throws IOException {
		base.reset();
	}

	@Override
	public int send() throws IOException {
		
		return base.send();
	}

	@Override
	public void sync(int ticket) throws IOException {
		base.sync(ticket);
	}

	@Override
	public void writeArray(boolean[] value) throws IOException {
		base.writeArray(value);
	}

	@Override
	public void writeArray(byte[] value) throws IOException {
		base.writeArray(value);
		
	}

	@Override
	public void writeArray(char[] value) throws IOException {
		base.writeArray(value);
		
	}

	@Override
	public void writeArray(short[] value) throws IOException {
		base.writeArray(value);
		
	}

	@Override
	public void writeArray(int[] value) throws IOException {
		base.writeArray(value);
		
	}

	@Override
	public void writeArray(long[] value) throws IOException {
		base.writeArray(value);
		
	}

	@Override
	public void writeArray(float[] value) throws IOException {
		base.writeArray(value);	
	}

	@Override
	public void writeArray(double[] value) throws IOException {
		base.writeArray(value);
	}

	@Override
	public void writeArray(Object[] value) throws IOException {
		base.writeArray(value);
	}

	@Override
	public void writeArray(boolean[] value, int offset, int length)
			throws IOException {
		base.writeArray(value, offset, length);	
	}

	@Override
	public void writeArray(byte[] value, int offset, int length)
			throws IOException {
		base.writeArray(value, offset, length);
	}

	@Override
	public void writeArray(char[] value, int offset, int length)
			throws IOException {
		base.writeArray(value, offset, length);
	}

	@Override
	public void writeArray(short[] value, int offset, int length)
			throws IOException {
		base.writeArray(value, offset, length);
	}

	@Override
	public void writeArray(int[] value, int offset, int length)
			throws IOException {
		base.writeArray(value, offset, length);
	}

	@Override
	public void writeArray(long[] value, int offset, int length)
			throws IOException {
		base.writeArray(value, offset, length);
	}

	@Override
	public void writeArray(float[] value, int offset, int length)
			throws IOException {
		base.writeArray(value, offset, length);
		
	}

	@Override
	public void writeArray(double[] value, int offset, int length)
			throws IOException {
		base.writeArray(value, offset, length);
		
	}

	@Override
	public void writeArray(Object[] value, int offset, int length)
			throws IOException {
		base.writeArray(value, offset, length);
	}

	@Override
	public void writeBoolean(boolean value) throws IOException {
		base.writeBoolean(value);
	}

	@Override
	public void writeByte(byte value) throws IOException {
		base.writeByte(value);
		
	}

	@Override
	public void writeChar(char value) throws IOException {
		base.writeChar(value);
	}

	@Override
	public void writeDouble(double value) throws IOException {
		base.writeDouble(value);
	}

	@Override
	public void writeFloat(float value) throws IOException {
		base.writeFloat(value);
	}

	@Override
	public void writeInt(int value) throws IOException {
		base.writeInt(value);
	}

	@Override
	public void writeLong(long value) throws IOException {
		base.writeLong(value);
	}

	@Override
	public void writeObject(Object value) throws IOException {
		base.writeObject(value);
	}

	@Override
	public void writeShort(short value) throws IOException {
		base.writeShort(value);	
	}

	@Override
	public void writeString(String value) throws IOException {
		base.writeString(value);
	}

}
