package ibis.io;

import ibis.ipl.IbisIOException;
import java.io.ObjectInputStream;
import java.io.InputStream;
import java.io.IOException;

public final class SunSerializationInputStream extends SerializationInputStream {
	ObjectInputStream in;

	public SunSerializationInputStream(InputStream s) throws IbisIOException {
		try {
			in = new ObjectInputStream(s);
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public String serializationImplName() {
		return "sun";
	}

	public void statistics() {
	}

	public void print() {
	}

	public int bytesRead() {
		return 0;
	}

	public void resetBytesRead() {
	}

	public int read() throws IbisIOException {
		try {
			return in.read();
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public int read(byte[] b) throws IbisIOException {
		try {
			return in.read(b);
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public int read(byte[] b, int off, int len) throws IbisIOException {
		try {
			return in.read(b, off, len);
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}


	public long skip(long n) throws IbisIOException {
		try {
			return in.skip(n);
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public int skipBytes(int n) throws IbisIOException {
		try {
			return in.skipBytes(n);
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public int available() throws IbisIOException {
		try {
			return in.available();
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}


	public void readFully(byte[] b) throws IbisIOException {
		try {
			in.readFully(b);
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public void readFully(byte[] b, int off, int len) throws IbisIOException {
		try {
			in.readFully(b, off, len);
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}


	public boolean readBoolean() throws IbisIOException {
		try {
			return in.readBoolean();
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public byte readByte() throws IbisIOException {
		try {
			return in.readByte();
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public int readUnsignedByte() throws IbisIOException {
		try {
			return in.readUnsignedByte();
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public short readShort() throws IbisIOException {
		try {
			return in.readShort();
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public int readUnsignedShort() throws IbisIOException {
		try {
			return in.readUnsignedShort();
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public char readChar() throws IbisIOException {
		try {
			return in.readChar();
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public int readInt() throws IbisIOException {
		try {
			return in.readInt();
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public long readLong() throws IbisIOException {
		try {
			return in.readLong();
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public float readFloat() throws IbisIOException {
		try {
			return in.readFloat();
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public double readDouble() throws IbisIOException {
		try {
			return in.readDouble();
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}


	public String readUTF() throws IbisIOException {
		try {
			return in.readUTF();
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public void readArraySliceBoolean(boolean[] ref, int off, int len) throws IbisIOException {
		boolean[] temp;
		try {
			temp = (boolean[]) in.readObject();
			if(temp.length != len) {
				throw new IbisIOException("Received sub array has wrong len");
			}
			System.arraycopy(temp, 0, ref, off, len);

		} catch (IOException e) {
			throw new IbisIOException("read error" + e);
		} catch (ClassCastException e2) {
			throw new IbisIOException("reading wrong type in stream", e2);
		} catch (ClassNotFoundException e3) {
			throw new IbisIOException("class not found" + e3);
		}
	}

	public void readArraySliceByte(byte[] ref, int off, int len) throws IbisIOException {
		byte[] temp;
		try {
			temp = (byte[]) in.readObject();
			if(temp.length != len) {
				throw new IbisIOException("Received sub array has wrong len");
			}
			System.arraycopy(temp, 0, ref, off, len);

		} catch (IOException e) {
			throw new IbisIOException("read error" + e);
		} catch (ClassCastException e2) {
			throw new IbisIOException("reading wrong type in stream", e2);
		} catch (ClassNotFoundException e3) {
			throw new IbisIOException("class not found" + e3);
		}
	}

	public void readArraySliceChar(char[] ref, int off, int len) throws IbisIOException {
		char[] temp;
		try {
			temp = (char[]) in.readObject();
			if(temp.length != len) {
				throw new IbisIOException("Received sub array has wrong len");
			}
			System.arraycopy(temp, 0, ref, off, len);

		} catch (IOException e) {
			throw new IbisIOException("read error" + e);
		} catch (ClassCastException e2) {
			throw new IbisIOException("reading wrong type in stream", e2);
		} catch (ClassNotFoundException e3) {
			throw new IbisIOException("class not found" + e3);
		}
	
	}

	public void readArraySliceShort(short[] ref, int off, int len) throws IbisIOException {
		short[] temp;
		try {
			temp = (short[]) in.readObject();
			if(temp.length != len) {
				throw new IbisIOException("Received sub array has wrong len");
			}
			System.arraycopy(temp, 0, ref, off, len);

		} catch (IOException e) {
			throw new IbisIOException("read error" + e);
		} catch (ClassCastException e2) {
			throw new IbisIOException("reading wrong type in stream", e2);
		} catch (ClassNotFoundException e3) {
			throw new IbisIOException("class not found" + e3);
		}
	 
	}

	public void readArraySliceInt(int[] ref, int off, int len) throws IbisIOException {
		int[] temp;
		try {
			temp = (int[]) in.readObject();
			if(temp.length != len) {
				throw new IbisIOException("Received sub array has wrong len");
			}
			System.arraycopy(temp, 0, ref, off, len);

		} catch (IOException e) {
			throw new IbisIOException("read error" + e);
		} catch (ClassCastException e2) {
			throw new IbisIOException("reading wrong type in stream", e2);
		} catch (ClassNotFoundException e3) {
			throw new IbisIOException("class not found" + e3);
		}
	 
	}

	public void readArraySliceLong(long[] ref, int off, int len) throws IbisIOException {
		long[] temp;
		try {
			temp = (long[]) in.readObject();
			if(temp.length != len) {
				throw new IbisIOException("Received sub array has wrong len");
			}
			System.arraycopy(temp, 0, ref, off, len);

		} catch (IOException e) {
			throw new IbisIOException("read error" + e);
		} catch (ClassCastException e2) {
			throw new IbisIOException("reading wrong type in stream", e2);
		} catch (ClassNotFoundException e3) {
			throw new IbisIOException("class not found" + e3);
		}
	}

	public void readArraySliceFloat(float[] ref, int off, int len) throws IbisIOException {
		float[] temp;
		try {
			temp = (float[]) in.readObject();
			if(temp.length != len) {
				throw new IbisIOException("Received sub array has wrong len");
			}
			System.arraycopy(temp, 0, ref, off, len);

		} catch (IOException e) {
			throw new IbisIOException("read error" + e);
		} catch (ClassCastException e2) {
			throw new IbisIOException("reading wrong type in stream", e2);
		} catch (ClassNotFoundException e3) {
			throw new IbisIOException("class not found" + e3);
		}
	 
	}

	public void readArraySliceDouble(double[] ref, int off, int len) throws IbisIOException {
		double[] temp;
		try {
			temp = (double[]) in.readObject();
			if(temp.length != len) {
				throw new IbisIOException("Received sub array has wrong len");
			}
			System.arraycopy(temp, 0, ref, off, len);

		} catch (IOException e) {
			throw new IbisIOException("read error" + e);
		} catch (ClassCastException e2) {
			throw new IbisIOException("reading wrong type in stream", e2);
		} catch (ClassNotFoundException e3) {
			throw new IbisIOException("class not found" + e3);
		}
	 
	}

	public void readArraySliceObject(Object[] ref, int off, int len) throws IbisIOException {
		Object[] temp;
		try {
			temp = (Object[]) in.readObject();
			if(temp.length != len) {
				throw new IbisIOException("Received sub array has wrong len");
			}
			System.arraycopy(temp, 0, ref, off, len);

		} catch (IOException e) {
			throw new IbisIOException("read error" + e);
		} catch (ClassCastException e2) {
			throw new IbisIOException("reading wrong type in stream", e2);
		} catch (ClassNotFoundException e3) {
			throw new IbisIOException("class not found" + e3);
		}
	 
	}

	public void readArrayBoolean(boolean[] destination) throws IbisIOException {
		readArraySliceBoolean(destination, 0, destination.length);
	}

	public void readArrayByte(byte[] destination) throws IbisIOException {
		readArraySliceByte(destination, 0, destination.length);
	}

	public void readArrayShort(short[] destination) throws IbisIOException {
		readArraySliceShort(destination, 0, destination.length);
	}

	public void readArrayChar(char[] destination) throws IbisIOException {
		readArraySliceChar(destination, 0, destination.length);
	}

	public void readArrayInt(int[] destination) throws IbisIOException {
		readArraySliceInt(destination, 0, destination.length);
	}

	public void readArrayLong(long[] destination) throws IbisIOException {
		readArraySliceLong(destination, 0, destination.length);
	}

	public void readArrayFloat(float[] destination) throws IbisIOException {
		readArraySliceFloat(destination, 0, destination.length);
	}

	public void readArrayDouble(double[] destination) throws IbisIOException {
		readArraySliceDouble(destination, 0, destination.length);
	}

	public void readArrayObject(Object[] destination) throws IbisIOException {
		readArraySliceObject(destination, 0, destination.length);
	}

	public Object readObject() throws IbisIOException {
		try {
			return in.readObject();
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public void close() throws IbisIOException {
		try {
			in.close();
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}
}
