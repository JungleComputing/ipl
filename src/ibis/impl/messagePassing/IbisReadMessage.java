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

    public void readArray(boolean[] destination) throws IbisIOException {
	try {
	    obj_in.readArray(destination);
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void readArray(byte[] destination) throws IbisIOException {
	try {
	    obj_in.readArray(destination);
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void readArray(char[] destination) throws IbisIOException {
	try {
	    obj_in.readArray(destination);
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void readArray(short[] destination) throws IbisIOException {
	try {
	    obj_in.readArray(destination);
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void readArray(int[] destination) throws IbisIOException {
	try {
	    obj_in.readArray(destination);
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void readArray(long[] destination) throws IbisIOException {
	try {
	    obj_in.readArray(destination);
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void readArray(float[] destination) throws IbisIOException {
	try {
	    obj_in.readArray(destination);
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void readArray(double[] destination) throws IbisIOException {
	try {
	    obj_in.readArray(destination);
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void readArray(Object[] destination) throws IbisIOException {
	try {
	    obj_in.readArray(destination);
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	} catch (ClassNotFoundException e2) {
	    throw new IbisIOException("got exception", e2);
	}
    }

    public void readArray(boolean[] destination, int offset,
					   int size) throws IbisIOException {
	try {
	    obj_in.readArray(destination, offset, size);
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void readArray(byte[] destination, int offset,
					int size) throws IbisIOException {
	try {
	    obj_in.readArray(destination, offset, size);
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void readArray(char[] destination, int offset,
					int size) throws IbisIOException {
	try {
	    obj_in.readArray(destination, offset, size);
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void readArray(short[] destination, int offset,
					 int size) throws IbisIOException {
	try {
	    obj_in.readArray(destination, offset, size);
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void readArray(int[] destination, int offset,
				       int size) throws IbisIOException {
	try {
	    obj_in.readArray(destination, offset, size);
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void readArray(long[] destination, int offset,
					int size) throws IbisIOException {
	try {
	    obj_in.readArray(destination, offset, size);
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void readArray(float[] destination, int offset,
					 int size) throws IbisIOException {
	try {
	    obj_in.readArray(destination, offset, size);
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void readArray(double[] destination, int offset,
					  int size) throws IbisIOException {
	try {
	    obj_in.readArray(destination, offset, size);
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void readArray(Object[] destination, int offset,
					  int size) throws IbisIOException {
	try {
	    obj_in.readArray(destination, offset, size);
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
