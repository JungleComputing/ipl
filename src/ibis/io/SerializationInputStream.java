package ibis.io;

import java.io.ObjectInputStream;
import java.io.InputStream;
import java.io.IOException;

/** This abstract class is the interface provided by Ibis Serialization.
    There are basically two ways to use this class:
    1. Actually use ObjectInputStream. In this case, the constructor
       version with an InputStream parameter must be used. We call this
       the Sun serialization version.
    2. Redefine all of the ObjectInputStream. In this case, the constructor
       without parameters must be used, and all methods of ObjectInputStream
       must be redefined. This is the path taken when Ibis serialization
       is used.
**/
public abstract class SerializationInputStream extends ObjectInputStream {

    /**	Constructor which must be called for Ibis serialization,
	because the corresponding ObjectIputStream constructor must be called,
	so that all of the ObjectInputStream methods can be redefined.
    **/
    SerializationInputStream() throws IOException {
	super();
    }

    /** Constructor which must be called for Sun serialization.
    **/
    SerializationInputStream(InputStream s) throws IOException {
	super(s);
    }

    /** Returns the actual implementation used by the stream **/
    public abstract String serializationImplName();
    
    /** Print some statistics. **/
    public abstract void statistics();

    /** Returns the total number of bytes that is written in the stream
	since the last resetBytesRead().
    **/
    public abstract int bytesRead();

    /** Reset the statistics. **/
    public abstract void resetBytesRead();

    /** Methods to receive arrays in place. No duplicate checks are done.
	These methods are a shortcut for:
	readArraySliceXXX(dest, 0, dest.length);
	It is therefore legal to use a readArrayXXX, with a corresponding
	writeArraySliceXXX. The destination array should be of the correct
	length!
    **/
    public void readArrayBoolean(boolean[] dest) throws IOException {
	readArraySliceBoolean(dest, 0, dest.length);
    }

    public void readArrayByte(byte[] dest) throws IOException {
	readArraySliceByte(dest, 0, dest.length);
    }

    public void readArrayShort(short[] dest) throws IOException {
	readArraySliceShort(dest, 0, dest.length);
    }

    public void readArrayChar(char[] dest) throws IOException {
	readArraySliceChar(dest, 0, dest.length);
    }

    public void readArrayInt(int[] dest) throws IOException {
	readArraySliceInt(dest, 0, dest.length);
    }

    public void readArrayLong(long[] dest) throws IOException {
	readArraySliceLong(dest, 0, dest.length);
    }

    public void readArrayFloat(float[] dest) throws IOException {
	readArraySliceFloat(dest, 0, dest.length);
    }

    public void readArrayDouble(double[] dest) throws IOException {
	readArraySliceDouble(dest, 0, dest.length);
    }

    public void readArrayObject(Object[] dest) throws IOException, ClassNotFoundException {
	readArraySliceObject(dest, 0, dest.length);
    }


    /** Read a slice of an array in place. No cycle checks are done. 
	It is legal to use a readArraySliceXXX, with a corresponding
	writeArrayXXX.
    **/
    public abstract void readArraySliceBoolean(boolean[] ref, int off, int len)
	throws IOException;
    public abstract void readArraySliceByte(byte[] ref, int off, int len)
	throws IOException;
    public abstract void readArraySliceChar(char[] ref, int off, int len)
	throws IOException;
    public abstract void readArraySliceShort(short[] ref, int off, int len)
	throws IOException;
    public abstract void readArraySliceInt(int[] ref, int off, int len)
	throws IOException;
    public abstract void readArraySliceLong(long[] ref, int off, int len)
	throws IOException;
    public abstract void readArraySliceFloat(float[] ref, int off, int len)
	throws IOException;
    public abstract void readArraySliceDouble(double[] ref, int off, int len)
	throws IOException;
    public abstract void readArraySliceObject(Object[] ref, int off, int len)
	throws IOException, ClassNotFoundException;

    /** We cannot redefine readOBject, because it is final in ObjectInputStream.
	The trick for Ibis serialization is to have the ObjectInputStream
	be initialized with its parameter-less constructor.
	This will cause its readObject method to call readObjectOverride
	instead of doing its own thing.
    **/
    protected final Object readObjectOverride() throws IOException, ClassNotFoundException {
	return doReadObject();
    }

    protected abstract Object doReadObject() throws IOException, ClassNotFoundException;
}
