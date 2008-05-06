package ibis.ipl.impl.mx;

import java.io.IOException;

import ibis.io.DataInputStream;

public class MxDataInputStream extends DataInputStream {
	MxReadChannel channel;
	
	public MxDataInputStream(MxReadChannel channel) {
		// TODO Auto-generated constructor stub
		this.channel = channel;
	}

	@Override
	public int bufferSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long bytesRead() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean readBoolean() throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void resetBytesRead() {
		// TODO Auto-generated method stub

	}

	@Override
	public int read() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	public void readArray(boolean[] destination, int offset, int length)
			throws IOException {
		// TODO Auto-generated method stub

	}

	public void readArray(byte[] destination, int offset, int length)
			throws IOException {
		// TODO Auto-generated method stub

	}

	public void readArray(char[] destination, int offset, int length)
			throws IOException {
		// TODO Auto-generated method stub

	}

	public void readArray(short[] destination, int offset, int length)
			throws IOException {
		// TODO Auto-generated method stub

	}

	public void readArray(int[] destination, int offset, int length)
			throws IOException {
		// TODO Auto-generated method stub

	}

	public void readArray(long[] destination, int offset, int length)
			throws IOException {
		// TODO Auto-generated method stub

	}

	public void readArray(float[] destination, int offset, int length)
			throws IOException {
		// TODO Auto-generated method stub

	}

	public void readArray(double[] destination, int offset, int length)
			throws IOException {
		// TODO Auto-generated method stub

	}

	public byte readByte() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	public char readChar() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	public double readDouble() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	public float readFloat() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	public int readInt() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	public long readLong() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	public short readShort() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

}
