package ibis.io;

import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.IOException;

public final class SunSerializationOutputStream extends SerializationOutputStream {
    public SunSerializationOutputStream(OutputStream s) throws IOException {
	super(s);
	flush();
    }

    public String serializationImplName() {
	return "sun";
    }

    /** These methods can be used to write slices of arrays.
	Warning: duplicates are NOT detected when these calls are used! **/
    public void writeArraySliceBoolean(boolean[] ref, int off, int len) throws IOException {
	boolean[] temp = new boolean[len];
	System.arraycopy(ref, off, temp, 0, len);
	writeObject(temp);
    }

    public void writeArraySliceByte(byte[] ref, int off, int len) throws IOException {
	byte[] temp = new byte[len];
	System.arraycopy(ref, off, temp, 0, len);
	writeObject(temp);
    }

    public void writeArraySliceShort(short[] ref, int off, int len) throws IOException {
	short[] temp = new short[len];
	System.arraycopy(ref, off, temp, 0, len);
	writeObject(temp);
    }

    public void writeArraySliceChar(char[] ref, int off, int len) throws IOException {
	char[] temp = new char[len];
	System.arraycopy(ref, off, temp, 0, len);
	writeObject(temp);
    }

    public void writeArraySliceInt(int[] ref, int off, int len) throws IOException {
	int[] temp = new int[len];
	System.arraycopy(ref, off, temp, 0, len);
	writeObject(temp);
    }

    public void writeArraySliceLong(long[] ref, int off, int len) throws IOException {
	long[] temp = new long[len];
	System.arraycopy(ref, off, temp, 0, len);
	writeObject(temp);
    }

    public void writeArraySliceFloat(float[] ref, int off, int len) throws IOException {
	float[] temp = new float[len];
	System.arraycopy(ref, off, temp, 0, len);
	writeObject(temp);
    }

    public void writeArraySliceDouble(double[] ref, int off, int len) throws IOException {
	double[] temp = new double[len];
	System.arraycopy(ref, off, temp, 0, len);
	writeObject(temp);
    }

    public void writeArraySliceObject(Object[] ref, int off, int len) throws IOException {
	Object[] temp = new Object[len];
	System.arraycopy(ref, off, temp, 0, len);
	writeObject(temp);
    }


    protected void doWriteObject(Object ref) throws IOException {
	/* We should not get here, because doWriteObject is only
	   called from writeObjectOverride(), which is only called
	   when we are not doing Sun serialization.
	*/
	throw new IOException("doWriteObject called from sun serialization");
    }

    public void statistics() {
    }

    public int bytesWritten() {    
	return 0;
    }

    public void resetBytesWritten() {
    }

}
