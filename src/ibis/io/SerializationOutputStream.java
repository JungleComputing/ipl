package ibis.io;

import ibis.ipl.IbisIOException;

/** This abstract class is the interface provided by Ibis Serialization **/
public abstract class SerializationOutputStream {
	private Replacer replacer;

	/** Set a replacer. The replacement mechanism can be used to replace an object with another object
	    during serialization. This is used in RMI, for instance, to replace a remote object with a stub. 
	    The replacement mechanism provided here is independent of the serialization implementation 
	    (Ibis serialization, Sun serialization).
	**/
	public void setReplacer(Replacer replacer) {
		this.replacer = replacer;
	}

	/** Returns the actual implementation used by the stream **/
	public abstract String serializationImplName();

	/** Write a byte. **/
	abstract public void write(int v) throws IbisIOException;

	abstract public void write(byte[] b) throws IbisIOException;
	abstract public void write(byte[] b, int off, int len) throws IbisIOException;

	abstract public void writeBoolean(boolean v) throws IbisIOException;
	abstract public void writeByte(int v) throws IbisIOException;
	abstract public void writeShort(int v) throws IbisIOException;
	abstract public void writeChar(int v) throws IbisIOException;
	abstract public void writeInt(int v) throws IbisIOException;
	abstract public void writeLong(long v) throws IbisIOException;
	abstract public void writeFloat(float f) throws IbisIOException;
	abstract public void writeDouble(double d) throws IbisIOException;

	/** These methods can be used to write whole arrays.
	    Duplicates are NOT detected when these calls are used.
	    It is legal to use a writeArrayXXX, with a corresponding readArraySliceXXX.
	**/
	abstract public void writeArrayBoolean(boolean[] ref) throws IbisIOException;
	abstract public void writeArrayByte(byte[] ref) throws IbisIOException;
	abstract public void writeArrayShort(short[] ref) throws IbisIOException;
	abstract public void writeArrayChar(char[] ref) throws IbisIOException;
	abstract public void writeArrayInt(int[] ref) throws IbisIOException;
	abstract public void writeArrayLong(long[] ref) throws IbisIOException;
	abstract public void writeArrayFloat(float[] ref) throws IbisIOException;
	abstract public void writeArrayDouble(double[] ref) throws IbisIOException;
	abstract public void writeArrayObject(Object[] ref) throws IbisIOException;

	/** These methods can be used to write slices of arrays.
	    Warning: duplicates are NOT detected when these calls are used!
	    It is legal to use a writeArraySliceXXX, with a corresponding readArrayXXX.
	**/
	abstract public void writeArraySliceBoolean(boolean[] ref, int off, int len) throws IbisIOException;
	abstract public void writeArraySliceByte(byte[] ref, int off, int len) throws IbisIOException;
	abstract public void writeArraySliceShort(short[] ref, int off, int len) throws IbisIOException;
	abstract public void writeArraySliceChar(char[] ref, int off, int len) throws IbisIOException;
	abstract public void writeArraySliceInt(int[] ref, int off, int len) throws IbisIOException;
	abstract public void writeArraySliceLong(long[] ref, int off, int len) throws IbisIOException;
	abstract public void writeArraySliceFloat(float[] ref, int off, int len) throws IbisIOException;
	abstract public void writeArraySliceDouble(double[] ref, int off, int len) throws IbisIOException;
	abstract public void writeArraySliceObject(Object[] ref, int off, int len) throws IbisIOException;

	abstract public void writeBytes(String s) throws IbisIOException;
	abstract public void writeChars(String s) throws IbisIOException;
	abstract public void writeUTF(String str) throws IbisIOException;

	/** Write objects and arrays. Duplicates are deteced when this call is used. **/
	/** The replacement mechanism is implemented here. **/
	public final void writeObject(Object ref) throws IbisIOException {
		if (ref != null && replacer != null) {
			ref = replacer.replace(ref);
		}

		doWriteObject(ref);
	}

	protected abstract void doWriteObject(Object ref) throws IbisIOException;

	abstract public void reset() throws IbisIOException;
	abstract public void flush() throws IbisIOException; 
	abstract public void close() throws IbisIOException;

	/** Print some statistics. **/
	abstract public void statistics();

	/** Returns the total number of bytes that is written in the stream 
	    since the last resetBytesWritten(). **/
	abstract public int bytesWritten();    

	/** Reset the statistics. **/
	abstract public void resetBytesWritten();
}
