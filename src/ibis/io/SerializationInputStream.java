package ibis.io;

import ibis.ipl.IbisIOException;

/** This abstract class is the interface provided by Ibis Serialization. **/
public abstract class SerializationInputStream {
	public abstract String serializationImplName();
	
	public abstract void statistics();
	public abstract void print();
	public abstract int bytesRead();
	public abstract void resetBytesRead();

	/** Read a byte. **/
	public abstract int read() throws IbisIOException;
	public abstract int read(byte[] b) throws IbisIOException;
	public abstract int read(byte[] b, int off, int len) throws IbisIOException;

	public abstract long skip(long n) throws IbisIOException;
	public abstract int skipBytes(int n) throws IbisIOException;
	public abstract int available() throws IbisIOException;

	public abstract void readFully(byte[] b) throws IbisIOException;
	public abstract void readFully(byte[] b, int off, int len) throws IbisIOException;

	public abstract boolean readBoolean() throws IbisIOException;
	public abstract byte readByte() throws IbisIOException;
	public abstract int readUnsignedByte() throws IbisIOException;
	public abstract short readShort() throws IbisIOException;
	public abstract int readUnsignedShort() throws IbisIOException;
	public abstract char readChar() throws IbisIOException;
	public abstract int readInt() throws IbisIOException;
	public abstract long readLong() throws IbisIOException;
	public abstract float readFloat() throws IbisIOException;
	public abstract double readDouble() throws IbisIOException;

	public abstract String readUTF() throws IbisIOException;

	/** Methods to receive arrays in place. No duplicate checks are done.
	    These methods are a shortcut for:
	    readArraySliceXXX(destination, 0, destination.length);

	    It is therefore legal to use a readArrayXXX, with a corresponding writeArraySliceXXX.
	    The destination array should be of the correct length!
	**/
	public abstract void readArrayBoolean(boolean[] destination) throws IbisIOException;
	public abstract void readArrayByte(byte[] destination) throws IbisIOException;
	public abstract void readArrayShort(short[] destination) throws IbisIOException;
	public abstract void readArrayChar(char[] destination) throws IbisIOException;
	public abstract void readArrayInt(int[] destination) throws IbisIOException;
	public abstract void readArrayLong(long[] destination) throws IbisIOException;
	public abstract void readArrayFloat(float[] destination) throws IbisIOException;
	public abstract void readArrayDouble(double[] destination) throws IbisIOException;
	public abstract void readArrayObject(Object[] destination) throws IbisIOException;

	/** Read a slice of an array in place. No cycle checks are done. 
	    It is legal to use a readArraySliceXXX, with a corresponding writeArrayXXX.
	**/
	public abstract void readArraySliceBoolean(boolean[] ref, int off, int len) throws IbisIOException;
	public abstract void readArraySliceByte(byte[] ref, int off, int len) throws IbisIOException;
	public abstract void readArraySliceChar(char[] ref, int off, int len) throws IbisIOException;
	public abstract void readArraySliceShort(short[] ref, int off, int len) throws IbisIOException;
	public abstract void readArraySliceInt(int[] ref, int off, int len) throws IbisIOException;
	public abstract void readArraySliceLong(long[] ref, int off, int len) throws IbisIOException;
	public abstract void readArraySliceFloat(float[] ref, int off, int len) throws IbisIOException;
	public abstract void readArraySliceDouble(double[] ref, int off, int len) throws IbisIOException;
	public abstract void readArraySliceObject(Object[] ref, int off, int len) throws IbisIOException;

	public abstract Object readObject() throws IbisIOException;
	public abstract void close() throws IbisIOException;
}
