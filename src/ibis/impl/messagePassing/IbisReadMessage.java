package ibis.ipl.impl.messagePassing;

import ibis.ipl.IbisIOException;
import java.io.IOException;

final public class IbisReadMessage extends ReadMessage {

    ibis.io.IbisSerializationInputStream obj_in;

    IbisReadMessage(ibis.ipl.SendPort origin,
		     ReceivePort port) {
	super(origin, port);
	obj_in = ((IbisShadowSendPort)origin).obj_in;
    }

    public boolean readBoolean() throws IbisIOException {
	try {
	    return obj_in.readBoolean();
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public byte readByte() throws IbisIOException {
	try {
	    return obj_in.readByte();
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public char readChar() throws IbisIOException {
	try {
	    return obj_in.readChar();
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public short readShort() throws IbisIOException {
	try {
	    return obj_in.readShort();
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public int readInt() throws IbisIOException {
	try {
	    return obj_in.readInt();
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public long readLong() throws IbisIOException {
	try {
	    return obj_in.readLong();
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public float readFloat() throws IbisIOException {
	try {
	    return obj_in.readFloat();
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public double readDouble() throws IbisIOException {
	try {
	    return obj_in.readDouble();
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public Object readObject() throws IbisIOException {
	try {
	    return obj_in.readObject();
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	} catch (ClassNotFoundException e2) {
	    throw new IbisIOException("got exception", e2);
	}
    }

    public String readString() throws IbisIOException { 
	try {
	    return (String) readObject();
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    } 

    public void readArrayBoolean(boolean[] destination) throws IbisIOException {
	try {
	    obj_in.readArrayBoolean(destination);
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void readArrayByte(byte[] destination) throws IbisIOException {
	try {
	    obj_in.readArrayByte(destination);
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void readArrayChar(char[] destination) throws IbisIOException {
	try {
	    obj_in.readArrayChar(destination);
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void readArrayShort(short[] destination) throws IbisIOException {
	try {
	    obj_in.readArrayShort(destination);
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void readArrayInt(int[] destination) throws IbisIOException {
	try {
	    obj_in.readArrayInt(destination);
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void readArrayLong(long[] destination) throws IbisIOException {
	try {
	    obj_in.readArrayLong(destination);
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void readArrayFloat(float[] destination) throws IbisIOException {
	try {
	    obj_in.readArrayFloat(destination);
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void readArrayDouble(double[] destination) throws IbisIOException {
	try {
	    obj_in.readArrayDouble(destination);
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void readArrayObject(Object[] destination) throws IbisIOException {
	try {
	    obj_in.readArrayObject(destination);
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	} catch (ClassNotFoundException e2) {
	    throw new IbisIOException("got exception", e2);
	}
    }

    public void readArraySliceBoolean(boolean[] destination, int offset,
					   int size) throws IbisIOException {
	try {
	    obj_in.readArraySliceBoolean(destination, offset, size);
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void readArraySliceByte(byte[] destination, int offset,
					int size) throws IbisIOException {
	try {
	    obj_in.readArraySliceByte(destination, offset, size);
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void readArraySliceChar(char[] destination, int offset,
					int size) throws IbisIOException {
	try {
	    obj_in.readArraySliceChar(destination, offset, size);
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void readArraySliceShort(short[] destination, int offset,
					 int size) throws IbisIOException {
	try {
	    obj_in.readArraySliceShort(destination, offset, size);
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void readArraySliceInt(int[] destination, int offset,
				       int size) throws IbisIOException {
	try {
	    obj_in.readArraySliceInt(destination, offset, size);
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void readArraySliceLong(long[] destination, int offset,
					int size) throws IbisIOException {
	try {
	    obj_in.readArraySliceLong(destination, offset, size);
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void readArraySliceFloat(float[] destination, int offset,
					 int size) throws IbisIOException {
	try {
	    obj_in.readArraySliceFloat(destination, offset, size);
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void readArraySliceDouble(double[] destination, int offset,
					  int size) throws IbisIOException {
	try {
	    obj_in.readArraySliceDouble(destination, offset, size);
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void readArraySliceObject(Object[] destination, int offset,
					  int size) throws IbisIOException {
	try {
	    obj_in.readArraySliceObject(destination, offset, size);
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	} catch (ClassNotFoundException e2) {
	    throw new IbisIOException("got exception", e2);
	}
    }

    public void receive() throws IbisIOException {
	throw new IbisIOException("receive not supported");
    }
}
