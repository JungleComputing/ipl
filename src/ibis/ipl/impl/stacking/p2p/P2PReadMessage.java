package ibis.ipl.impl.stacking.p2p;

import java.io.IOException;

import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPortIdentifier;

public class P2PReadMessage implements ReadMessage {
	
	final ReadMessage base;
	final P2PReceivePort port;

	public P2PReadMessage(ReadMessage message, P2PReceivePort port)
	{
		this.base = message;
		this.port = port;
	}
	
	@Override
	public long bytesRead() throws IOException {
		return base.bytesRead();
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
	public ReceivePort localPort() {
		return base.localPort();
	}

	@Override
	public SendPortIdentifier origin() {
		return base.origin();
	}

	@Override
	public void readArray(boolean[] destination) throws IOException {
		base.readArray(destination);
	}

	@Override
	public void readArray(byte[] destination) throws IOException {
		base.readArray(destination);
	}

	@Override
	public void readArray(char[] destination) throws IOException {
		base.readArray(destination);
	}

	@Override
	public void readArray(short[] destination) throws IOException {
		base.readArray(destination);
	}

	@Override
	public void readArray(int[] destination) throws IOException {
		base.readArray(destination);
	}

	@Override
	public void readArray(long[] destination) throws IOException {
		base.readArray(destination);
	}

	@Override
	public void readArray(float[] destination) throws IOException {
		base.readArray(destination);
	}

	@Override
	public void readArray(double[] destination) throws IOException {
		base.readArray(destination);
	}

	@Override
	public void readArray(Object[] destination) throws IOException,
			ClassNotFoundException {
		base.readArray(destination);

	}

	@Override
	public void readArray(boolean[] destination, int offset, int size)
			throws IOException {
		base.readArray(destination, offset, size);
	}

	@Override
	public void readArray(byte[] destination, int offset, int size)
			throws IOException {
		base.readArray(destination, offset, size);
	}

	@Override
	public void readArray(char[] destination, int offset, int size)
			throws IOException {
		base.readArray(destination, offset, size);
	}

	@Override
	public void readArray(short[] destination, int offset, int size)
			throws IOException {
		base.readArray(destination, offset, size);
	}

	@Override
	public void readArray(int[] destination, int offset, int size)
			throws IOException {
		base.readArray(destination, offset, size);
	}

	@Override
	public void readArray(long[] destination, int offset, int size)
			throws IOException {
		base.readArray(destination, offset, size);
	}

	@Override
	public void readArray(float[] destination, int offset, int size)
			throws IOException {
		base.readArray(destination, offset, size);
	}

	@Override
	public void readArray(double[] destination, int offset, int size)
			throws IOException {
		base.readArray(destination, offset, size);
	}

	@Override
	public void readArray(Object[] destination, int offset, int size)
			throws IOException, ClassNotFoundException {
		base.readArray(destination, offset, size);
	}

	@Override
	public boolean readBoolean() throws IOException {
		return base.readBoolean();
	}

	@Override
	public byte readByte() throws IOException {
		return base.readByte();
	}

	@Override
	public char readChar() throws IOException {
		return base.readChar();
	}

	@Override
	public double readDouble() throws IOException {
		return base.readDouble();
	}

	@Override
	public float readFloat() throws IOException {
		return base.readFloat();
	}

	@Override
	public int readInt() throws IOException {
		return base.readInt();
	}

	@Override
	public long readLong() throws IOException {
		return base.readLong();
	}

	@Override
	public Object readObject() throws IOException, ClassNotFoundException {
		return base.readObject();
	}

	@Override
	public short readShort() throws IOException {
		return base.readShort();
	}

	@Override
	public String readString() throws IOException {
		return base.readString();
	}

	@Override
	public int remaining() throws IOException {
		return base.remaining();
	}

	@Override
	public long sequenceNumber() {
		return base.sequenceNumber();
	}

	@Override
	public int size() throws IOException {
		return base.size();
	}

}
