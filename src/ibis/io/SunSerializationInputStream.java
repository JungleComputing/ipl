package ibis.io;

import java.io.ObjectInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OptionalDataException;

public final class SunSerializationInputStream extends SerializationInputStream {
    public SunSerializationInputStream(InputStream s) throws IOException {
	super(s);
    }

    public String serializationImplName() {
	return "sun";
    }

    public void statistics() {
    }

    public int bytesRead() {
	return 0;
    }

    public void resetBytesRead() {
    }

    public void readArraySliceBoolean(boolean[] ref, int off, int len) throws IOException {
	boolean[] temp;
	try {
	    temp = (boolean[]) readObject();
	    if(temp.length != len) {
		throw new IOException("Received sub array has wrong len");
	    }
	    System.arraycopy(temp, 0, ref, off, len);
	} catch (ClassCastException e) {
	    throw new IOException("reading wrong type in stream" + e);
	} catch (ClassNotFoundException f) {
	    throw new IOException("class not found" + f);
	} catch (OptionalDataException g) {
	    throw new IOException("optional data exception" + g);
	}
    }

    public void readArraySliceByte(byte[] ref, int off, int len) throws IOException {
	byte[] temp;
	try {
	    temp = (byte[]) readObject();
	    if(temp.length != len) {
		throw new IOException("Received sub array has wrong len");
	    }
	    System.arraycopy(temp, 0, ref, off, len);
	} catch (ClassCastException e) {
	    throw new IOException("reading wrong type in stream" + e);
	} catch (ClassNotFoundException f) {
	    throw new IOException("class not found" + f);
	} catch (OptionalDataException g) {
	    throw new IOException("optional data exception" + g);
	}
    }

    public void readArraySliceChar(char[] ref, int off, int len) throws IOException {
	char[] temp;
	try {
	    temp = (char[]) readObject();
	    if(temp.length != len) {
		throw new IOException("Received sub array has wrong len");
	    }
	    System.arraycopy(temp, 0, ref, off, len);
	} catch (ClassCastException e) {
	    throw new IOException("reading wrong type in stream" + e);
	} catch (ClassNotFoundException f) {
	    throw new IOException("class not found" + f);
	} catch (OptionalDataException g) {
	    throw new IOException("optional data exception" + g);
	}
    }

    public void readArraySliceShort(short[] ref, int off, int len) throws IOException {
	short[] temp;
	try {
	    temp = (short[]) readObject();
	    if(temp.length != len) {
		throw new IOException("Received sub array has wrong len");
	    }
	    System.arraycopy(temp, 0, ref, off, len);
	} catch (ClassCastException e) {
	    throw new IOException("reading wrong type in stream" + e);
	} catch (ClassNotFoundException f) {
	    throw new IOException("class not found" + f);
	} catch (OptionalDataException g) {
	    throw new IOException("optional data exception" + g);
	}
    }

    public void readArraySliceInt(int[] ref, int off, int len) throws IOException {
	int[] temp;
	try {
	    temp = (int[]) readObject();
	    if(temp.length != len) {
		throw new IOException("Received sub array has wrong len");
	    }
	    System.arraycopy(temp, 0, ref, off, len);
	} catch (ClassCastException e) {
	    throw new IOException("reading wrong type in stream" + e);
	} catch (ClassNotFoundException f) {
	    throw new IOException("class not found" + f);
	} catch (OptionalDataException g) {
	    throw new IOException("optional data exception" + g);
	}
    }

    public void readArraySliceLong(long[] ref, int off, int len) throws IOException {
	long[] temp;
	try {
	    temp = (long[]) readObject();
	    if(temp.length != len) {
		throw new IOException("Received sub array has wrong len");
	    }
	    System.arraycopy(temp, 0, ref, off, len);
	} catch (ClassCastException e) {
	    throw new IOException("reading wrong type in stream" + e);
	} catch (ClassNotFoundException f) {
	    throw new IOException("class not found" + f);
	} catch (OptionalDataException g) {
	    throw new IOException("optional data exception" + g);
	}
    }

    public void readArraySliceFloat(float[] ref, int off, int len) throws IOException {
	float[] temp;
	try {
	    temp = (float[]) readObject();
	    if(temp.length != len) {
		throw new IOException("Received sub array has wrong len");
	    }
	    System.arraycopy(temp, 0, ref, off, len);
	} catch (ClassCastException e) {
	    throw new IOException("reading wrong type in stream" + e);
	} catch (ClassNotFoundException f) {
	    throw new IOException("class not found" + f);
	} catch (OptionalDataException g) {
	    throw new IOException("optional data exception" + g);
	}
    }

    public void readArraySliceDouble(double[] ref, int off, int len) throws IOException {
	double[] temp;
	try {
	    temp = (double[]) readObject();
	    if(temp.length != len) {
		throw new IOException("Received sub array has wrong len");
	    }
	    System.arraycopy(temp, 0, ref, off, len);
	} catch (ClassCastException e) {
	    throw new IOException("reading wrong type in stream" + e);
	} catch (ClassNotFoundException f) {
	    throw new IOException("class not found" + f);
	} catch (OptionalDataException g) {
	    throw new IOException("optional data exception" + g);
	}
    }

    public void readArraySliceObject(Object[] ref, int off, int len) throws IOException {
	Object[] temp;
	try {
	    temp = (Object[]) readObject();
	    if(temp.length != len) {
		throw new IOException("Received sub array has wrong len");
	    }
	    System.arraycopy(temp, 0, ref, off, len);
	} catch (ClassCastException e) {
	    throw new IOException("reading wrong type in stream" + e);
	} catch (ClassNotFoundException f) {
	    throw new IOException("class not found" + f);
	} catch (OptionalDataException g) {
	    throw new IOException("optional data exception" + g);
	}
    }

    protected Object doReadObject() throws IOException {
	/*  We should not get here, because doReadObject is only
	    called from readObjectOverride(), which is only called when
	    we are not doing Sun serialization.
	*/
	throw new IOException("doReadObject called from sun serialization");
    }
}
