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

    /** Methods to receive arrays in place. No duplicate checks are done.
	These methods are a shortcut for:
	readArray(dest, 0, dest.length);
	It is therefore legal to use a readArrayXXX, with a corresponding
	writeArray. The destination array should be of the correct
	length!
    **/
    public void readArray(boolean[] dest) throws IOException {
	readArray(dest, 0, dest.length);
    }

    public void readArray(byte[] dest) throws IOException {
	readArray(dest, 0, dest.length);
    }

    public void readArray(short[] dest) throws IOException {
	readArray(dest, 0, dest.length);
    }

    public void readArray(char[] dest) throws IOException {
	readArray(dest, 0, dest.length);
    }

    public void readArray(int[] dest) throws IOException {
	readArray(dest, 0, dest.length);
    }

    public void readArray(long[] dest) throws IOException {
	readArray(dest, 0, dest.length);
    }

    public void readArray(float[] dest) throws IOException {
	readArray(dest, 0, dest.length);
    }

    public void readArray(double[] dest) throws IOException {
	readArray(dest, 0, dest.length);
    }

    public void readArray(Object[] dest) throws IOException, ClassNotFoundException {
	readArray(dest, 0, dest.length);
    }


    /** Read a slice of an array in place. No cycle checks are done. 
	It is legal to use a readArray, with a corresponding
	writeArrayXXX.
    **/
    public abstract void readArray(boolean[] ref, int off, int len)
	throws IOException;
    public abstract void readArray(byte[] ref, int off, int len)
	throws IOException;
    public abstract void readArray(char[] ref, int off, int len)
	throws IOException;
    public abstract void readArray(short[] ref, int off, int len)
	throws IOException;
    public abstract void readArray(int[] ref, int off, int len)
	throws IOException;
    public abstract void readArray(long[] ref, int off, int len)
	throws IOException;
    public abstract void readArray(float[] ref, int off, int len)
	throws IOException;
    public abstract void readArray(double[] ref, int off, int len)
	throws IOException;
    public abstract void readArray(Object[] ref, int off, int len)
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
