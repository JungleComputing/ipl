package ibis.io;

import ibis.ipl.IbisIOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.IOException;

public final class SunSerializationOutputStream extends SerializationOutputStream {
	ObjectOutputStream out;

	public SunSerializationOutputStream(OutputStream s) throws IbisIOException {
		try {
			out = new ObjectOutputStream(s);
			out.flush();
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public String serializationImplName() {
		return "sun";
	}

	public void write(int v) throws IbisIOException {
		try {
			out.write(v);
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public void write(byte[] b) throws IbisIOException {
		try {
			out.write(b);
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}
	
	public void write(byte[] b, int off, int len) throws IbisIOException {
		try {
			out.write(b, off, len);
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}


	public void writeBoolean(boolean v) throws IbisIOException {
		try {
			out.writeBoolean(v);
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public void writeByte(int v) throws IbisIOException {
		try {
			out.writeByte(v);
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public void writeShort(int v) throws IbisIOException {
		try {
			out.writeShort(v);
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public void writeChar(int v) throws IbisIOException {
		try {
			out.writeChar(v);
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public void writeInt(int v) throws IbisIOException {
		try {
			out.writeInt(v);
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public void writeLong(long v) throws IbisIOException {
		try {
			out.writeLong(v);
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public void writeFloat(float f) throws IbisIOException {
		try {
			out.writeFloat(f);
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public void writeDouble(double d) throws IbisIOException {
		try {
			out.writeDouble(d);
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}


	/** These methods can be used to write whole arrays.
	    Duplicates are not detected when these calls are used. **/
	public void writeArrayBoolean(boolean[] ref) throws IbisIOException {
		try {
			out.writeObject(ref);
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public void writeArrayByte(byte[] ref) throws IbisIOException {
		try {
			out.writeObject(ref);
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public void writeArrayShort(short[] ref) throws IbisIOException {
		try {
			out.writeObject(ref);
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public void writeArrayChar(char[] ref) throws IbisIOException {
		try {
			out.writeObject(ref);
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public void writeArrayInt(int[] ref) throws IbisIOException {
		try {
			out.writeObject(ref);
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public void writeArrayLong(long[] ref) throws IbisIOException {
		try {
			out.writeObject(ref);
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public void writeArrayFloat(float[] ref) throws IbisIOException {
		try {
			out.writeObject(ref);
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public void writeArrayDouble(double[] ref) throws IbisIOException {
		try {
			out.writeObject(ref);
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public void writeArrayObject(Object[] ref) throws IbisIOException {
		try {
			out.writeObject(ref);
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}


        /** These methods can be used to write slices of arrays.
	    Warning: duplicates are NOT detected when these calls are used! **/
	public void writeArraySliceBoolean(boolean[] ref, int off, int len) throws IbisIOException {
		try {
			boolean[] temp = new boolean[len];
			System.arraycopy(ref, off, temp, 0, len);
			out.writeObject(temp);
		} catch (IOException e) {
			throw new IbisIOException("Write error", e);
		}
	}

	public void writeArraySliceByte(byte[] ref, int off, int len) throws IbisIOException {
		try {
			byte[] temp = new byte[len];
			System.arraycopy(ref, off, temp, 0, len);
			out.writeObject(temp);
		} catch (IOException e) {
			throw new IbisIOException("Write error", e);
		}
	}

	public void writeArraySliceShort(short[] ref, int off, int len) throws IbisIOException {
		try {
			short[] temp = new short[len];
			System.arraycopy(ref, off, temp, 0, len);
			out.writeObject(temp);
		} catch (IOException e) {
			throw new IbisIOException("Write error", e);
		}
	}

	public void writeArraySliceChar(char[] ref, int off, int len) throws IbisIOException {
		try {
			char[] temp = new char[len];
			System.arraycopy(ref, off, temp, 0, len);
			out.writeObject(temp);
		} catch (IOException e) {
			throw new IbisIOException("Write error", e);
		}
	}

	public void writeArraySliceInt(int[] ref, int off, int len) throws IbisIOException {
		try {
			int[] temp = new int[len];
			System.arraycopy(ref, off, temp, 0, len);
			out.writeObject(temp);
		} catch (IOException e) {
			throw new IbisIOException("Write error", e);
		}
	}

	public void writeArraySliceLong(long[] ref, int off, int len) throws IbisIOException {
		try {
			long[] temp = new long[len];
			System.arraycopy(ref, off, temp, 0, len);
			out.writeObject(temp);
		} catch (IOException e) {
			throw new IbisIOException("Write error", e);
		}
	}

	public void writeArraySliceFloat(float[] ref, int off, int len) throws IbisIOException {
		try {
			float[] temp = new float[len];
			System.arraycopy(ref, off, temp, 0, len);
			out.writeObject(temp);
		} catch (IOException e) {
			throw new IbisIOException("Write error", e);
		}
	}

	public void writeArraySliceDouble(double[] ref, int off, int len) throws IbisIOException {
		try {
			double[] temp = new double[len];
			System.arraycopy(ref, off, temp, 0, len);
			out.writeObject(temp);
		} catch (IOException e) {
			throw new IbisIOException("Write error", e);
		}
	}

	public void writeArraySliceObject(Object[] ref, int off, int len) throws IbisIOException {
		try {
			Object[] temp = new Object[len];
			System.arraycopy(ref, off, temp, 0, len);
			out.writeObject(temp);
		} catch (IOException e) {
			throw new IbisIOException("Write error", e);
		}
	}


	public void writeBytes(String s) throws IbisIOException {
		try {
			out.writeBytes(s);
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public void writeChars(String s) throws IbisIOException {
		try {
			out.writeChars(s);
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public void writeUTF(String str) throws IbisIOException {
		try {
			out.writeUTF(str);
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	protected void doWriteObject(Object ref) throws IbisIOException {
		try {
			out.writeObject(ref);
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}


	public void reset() throws IbisIOException {
		try {
			out.reset();
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public void flush() throws IbisIOException { 
		try {
			out.flush();
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public void close() throws IbisIOException {
		try {
			out.close();
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public void statistics() {
	}

	public int bytesWritten() {    
		return 0;
	}

	public void resetBytesWritten() {
	}

}
