package ibis.io;

import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.IOException;

/** This abstract class is the interface provided by Ibis Serialization.
    There are basically two ways to use this class:
    1. Actually use ObjectOutputStream. In this case, the constructor
       version with an OutputStream parameter must be used. We call this
       the Sun serialization version.
    2. Redefine all of the ObjectOutputStream. In this case, the constructor
       without parameters must be used, and all methods of ObjectOutputStream
       must be redefined. This is the path taken when Ibis serialization
       is used.
*/
public abstract class SerializationOutputStream extends ObjectOutputStream {
    private Replacer replacer;

    /** Constructor which must be called for Ibis serialization.
	The corresponding ObjectOutputStream constructor must be called,
	so that all of the ObjectOutputStream methods can be redefined.
    */
    SerializationOutputStream() throws IOException {
	super();
    }

    /** Constructor which must be called for Sun serialization.
    */
    SerializationOutputStream(OutputStream s) throws IOException {
	super(s);
    }

    /** Set a replacer. The replacement mechanism can be used to replace
	an object with another object during serialization. This is used
	in RMI, for instance, to replace a remote object with a stub. 
	The replacement mechanism provided here is independent of the
	serialization implementation (Ibis serialization, Sun
	serialization).
    */
    public void setReplacer(Replacer replacer) {
	try {
	    enableReplaceObject(true);
	} catch (Exception e) {
	}
	this.replacer = replacer;
    }

    /** Object replacement for Sun serialization.
    */
    protected Object replaceObject(Object obj) {
	if (obj != null && replacer != null) {
	    obj = replacer.replace(obj);
	}
	return obj;
    }

    /** Returns the actual implementation used by the stream 
    */
    public abstract String serializationImplName();

    /** Write a slice of an array of booleans.
	Warning: duplicates are NOT detected when these calls are used!
	It is legal to use a writeArray, with a corresponding
	readArray.
    */
    abstract public void writeArray(boolean[] ref, int off, int len)
	throws IOException;

    /** Write a slice of an array of bytes.
	Warning: duplicates are NOT detected when these calls are used!
	It is legal to use a writeArray, with a corresponding
	readArray.
    */
    abstract public void writeArray(byte[] ref, int off, int len)
	throws IOException;

    /** Write a slice of an array of shorts.
	Warning: duplicates are NOT detected when these calls are used!
	It is legal to use a writeArray, with a corresponding
	readArray.
    */
    abstract public void writeArray(short[] ref, int off, int len)
	throws IOException;

    /** Write a slice of an array of chars.
	Warning: duplicates are NOT detected when these calls are used!
	It is legal to use a writeArray, with a corresponding
	readArray.
    */
    abstract public void writeArray(char[] ref, int off, int len)
	throws IOException;

    /** Write a slice of an array of ints.
	Warning: duplicates are NOT detected when these calls are used!
	It is legal to use a writeArray, with a corresponding
	readArray.
    */
    abstract public void writeArray(int[] ref, int off, int len)
	throws IOException;

    /** Write a slice of an array of longs.
	Warning: duplicates are NOT detected when these calls are used!
	It is legal to use a writeArray, with a corresponding
	readArray.
    */
    abstract public void writeArray(long[] ref, int off, int len)
	throws IOException;

    /** Write a slice of an array of floats.
	Warning: duplicates are NOT detected when these calls are used!
	It is legal to use a writeArray, with a corresponding
	readArray.
    */
    abstract public void writeArray(float[] ref, int off, int len)
	throws IOException;

    /** Write a slice of an array of doubles.
	Warning: duplicates are NOT detected when these calls are used!
	It is legal to use a writeArray, with a corresponding
	readArray.
    */
    abstract public void writeArray(double[] ref, int off, int len)
	throws IOException;

    /** Write a slice of an array of objects.
	Warning: duplicates are NOT detected when these calls are used!
	It is legal to use a writeArray, with a corresponding
	readArray.
    */
    abstract public void writeArray(Object[] ref, int off, int len)
	throws IOException;

    /** These methods can be used to write whole arrays.
	Duplicates are NOT detected when these calls are used.
	It is legal to use a writeArrayXXX, with a corresponding
	readArray.
    **/
    public void writeArray(boolean[] ref) throws IOException {
	writeArray(ref, 0, ref.length);
    }

    public void writeArray(byte[] ref) throws IOException {
	writeArray(ref, 0, ref.length);
    }

    public void writeArray(short[] ref) throws IOException {
	writeArray(ref, 0, ref.length);
    }

    public void writeArray(char[] ref) throws IOException {
	writeArray(ref, 0, ref.length);
    }

    public void writeArray(int[] ref) throws IOException {
	writeArray(ref, 0, ref.length);
    }

    public void writeArray(long[] ref) throws IOException {
	writeArray(ref, 0, ref.length);
    }

    public void writeArray(float[] ref) throws IOException {
	writeArray(ref, 0, ref.length);
    }

    public void writeArray(double[] ref) throws IOException {
	writeArray(ref, 0, ref.length);
    }

    public void writeArray(Object[] ref) throws IOException {
	writeArray(ref, 0, ref.length);
    }


    /** Write objects and arrays.
	Duplicates are deteced when this call is used.
	The replacement mechanism is implemented here as well.
        We cannot redefine writeObject, because it is final in
        ObjectOutputStream. The trick for Ibis serialization is to have the
        ObjectOutputStream be initialized with its parameter-less constructor.
        This will cause its writeObject to call writeObjectOverride instead
        of doing its own thing.
    */
    protected final void writeObjectOverride(Object ref)
	throws IOException {
	if (ref != null && replacer != null) {
		ref = replacer.replace(ref);
	}

	doWriteObject(ref);
    }

    /** Write objects and arrays.
        To be specified by IbisSerializationOutputStream. The SunSerializationOutputStream
	version should never be called, because doWriteObject is only called from
	writeObjectOverride, which only gets called when we are doing Ibis serialization.
    */
    protected abstract void doWriteObject(Object ref)
	throws IOException;


    /** Print some statistics. 
    */
    abstract public void statistics();
}
