package ibis.ipl.impl.messagePassing;

import java.io.IOException;

import ibis.ipl.IbisException;

class SerializeReadMessage extends ibis.ipl.impl.messagePassing.ReadMessage {

    java.io.ObjectInput obj_in;

    SerializeReadMessage(ibis.ipl.SendPort origin, ReceivePort port, int msg) {
	super(origin, port, msg);
	this.obj_in = ((ShadowSendPort)origin).obj_in;
    }

    public boolean readBoolean() throws IbisException {
	try {
	    return obj_in.readBoolean();
	} catch (IOException e) {
	    throw new IbisException("read error" + e);
	}
    }

    public byte readByte() throws IbisException {
	try {
	    byte b = (byte) obj_in.read();
	    return b;
	} catch (IOException e) {
	    throw new IbisException("read error" + e);
	}
    }

    public char readChar() throws IbisException {
	try {
	    return obj_in.readChar();
	} catch (IOException e) {
	    throw new IbisException("read error" + e);
	}
    }

    public short readShort() throws IbisException {
	try {
	    return obj_in.readShort();
	} catch (IOException e) {
	    throw new IbisException("read error" + e);
	}
    }

    public int  readInt() throws IbisException {
	try {
	    return obj_in.readInt();
	} catch (IOException e) {
	    throw new IbisException("read error" + e);
	}
    }

    public long readLong() throws IbisException {
	try {
	    return obj_in.readLong();
	} catch (IOException e) {
	    throw new IbisException("read error" + e);
	}
    }

    public float readFloat() throws IbisException {
	try {
	    return obj_in.readFloat();
	} catch (IOException e) {
	    throw new IbisException("read error" + e);
	}
    }

    public double readDouble() throws IbisException {
	try {
	    return obj_in.readDouble();
	} catch (IOException e) {
	    throw new IbisException("read error" + e);
	}
    }

    public String readString() throws IbisException {
	try {
	    return (String) obj_in.readObject();
	} catch (IOException e) {
	    throw new IbisException("read error" + e);
	} catch (ClassNotFoundException e3) {
	    throw new IbisException("class not found" + e3);
	}
    }

    public Object readObject() throws IbisException {
System.err.println("SerializeReadMessage.readObject() called " + this);
	try {
	    return obj_in.readObject();
	} catch (IOException e) {
	    throw new IbisException("read error" + e);
	} catch (ClassNotFoundException e3) {
	    throw new IbisException("class not found" + e3);
	}
    }

    public void readArrayBoolean(boolean[] destination) throws IbisException {
	try {
	    boolean[] temp = (boolean[])obj_in.readObject();
	    if (temp.length != destination.length) {
		throw new IbisException("Destination has wrong size");
	    }
	    System.arraycopy(temp, 0, destination, 0, temp.length);

	} catch (IOException e) {
	    throw new IbisException("read error" + e);
	} catch (ClassCastException e2) {
	    throw new IbisException("reading wrong type in stream", e2);
	} catch (ClassNotFoundException e3) {
	    throw new IbisException("class not found" + e3);
	}
    }

    public void readArrayByte(byte[] destination) throws IbisException {
	try {
	    byte[] temp = (byte[])obj_in.readObject();
	    if (temp.length != destination.length) {
		throw new IbisException("Destination has wrong size");
	    }
	    System.arraycopy(temp, 0, destination, 0, temp.length);

	} catch (IOException e) {
	    throw new IbisException("read error" + e);
	} catch (ClassCastException e2) {
	    throw new IbisException("reading wrong type in stream", e2);
	} catch (ClassNotFoundException e3) {
	    throw new IbisException("class not found" + e3);
	}
    }

    public void readArrayChar(char[] destination) throws IbisException {
	try {
	    char[] temp = (char[]) obj_in.readObject();
	    if (temp.length != destination.length) {
		throw new IbisException("Destination has wrong size");
	    }
	    System.arraycopy(temp, 0, destination, 0, temp.length);

	} catch (IOException e) {
	    throw new IbisException("read error" + e);
	} catch (ClassCastException e2) {
	    throw new IbisException("reading wrong type in stream", e2);
	} catch (ClassNotFoundException e3) {
	    throw new IbisException("class not found" + e3);
	}
    }

    public void readArrayShort(short[] destination) throws IbisException {

	try {
	    short[] temp = (short[]) obj_in.readObject();
	    if (temp.length != destination.length) {
		throw new IbisException("Destination has wrong size");
	    }
	    System.arraycopy(temp, 0, destination, 0, temp.length);

	} catch (IOException e) {
	    throw new IbisException("read error" + e);
	} catch (ClassCastException e2) {
	    throw new IbisException("reading wrong type in stream", e2);
	} catch (ClassNotFoundException e3) {
	    throw new IbisException("class not found" + e3);
	}
    }

    public void readArrayInt(int[] destination) throws IbisException {
	try {
	    int[] temp = (int[]) obj_in.readObject();
	    if (temp.length != destination.length) {
		throw new IbisException("Destination has wrong size");
	    }
	    System.arraycopy(temp, 0, destination, 0, temp.length);

	} catch (IOException e) {
	    throw new IbisException("read error" + e);
	} catch (ClassCastException e2) {
	    throw new IbisException("reading wrong type in stream", e2);
	} catch (ClassNotFoundException e3) {
	    throw new IbisException("class not found" + e3);
	}
    }

    public void readArrayLong(long[] destination) throws IbisException {
	try {
	    long[] temp = (long[]) obj_in.readObject();
	    if (temp.length != destination.length) {
		throw new IbisException("Destination has wrong size");
	    }
	    System.arraycopy(temp, 0, destination, 0, temp.length);

	} catch (IOException e) {
	    throw new IbisException("read error" + e);
	} catch (ClassCastException e2) {
	    throw new IbisException("reading wrong type in stream", e2);
	} catch (ClassNotFoundException e3) {
	    throw new IbisException("class not found" + e3);
	}
    }

    public void readArrayFloat(float[] destination) throws IbisException {
	try {
	    float[] temp = (float[]) obj_in.readObject();
	    if (temp.length != destination.length) {
		throw new IbisException("Destination has wrong size");
	    }
	    System.arraycopy(temp, 0, destination, 0, temp.length);

	} catch (IOException e) {
	    throw new IbisException("read error" + e);
	} catch (ClassCastException e2) {
	    throw new IbisException("reading wrong type in stream", e2);
	} catch (ClassNotFoundException e3) {
	    throw new IbisException("class not found" + e3);
	}
    }

    public void readArrayDouble(double[] destination) throws IbisException {
	try {
	    double[] temp = (double[]) obj_in.readObject();
	    if (temp.length != destination.length) {
		throw new IbisException("Destination has wrong size");
	    }
	    System.arraycopy(temp, 0, destination, 0, temp.length);

	} catch (IOException e) {
	    throw new IbisException("read error" + e);
	} catch (ClassCastException e2) {
	    throw new IbisException("reading wrong type in stream", e2);
	} catch (ClassNotFoundException e3) {
	    throw new IbisException("class not found" + e3);
	}
    }

    public void readSubArrayBoolean(boolean[] destination, int offset,
				    int size) throws IbisException {
	try {
	    boolean[] temp = (boolean[])obj_in.readObject();
	    if (temp.length != size) {
		throw new IbisException("Received sub array has wrong size");
	    }
	    System.arraycopy(temp, 0, destination, offset, size);

	} catch (IOException e) {
	    throw new IbisException("read error" + e);
	} catch (ClassCastException e2) {
	    throw new IbisException("reading wrong type in stream", e2);
	} catch (ClassNotFoundException e3) {
	    throw new IbisException("class not found" + e3);
	}
    }

    public void readSubArrayByte(byte[] destination, int offset,
				 int size) throws IbisException {
	try {
	    byte[] temp = (byte[])obj_in.readObject();
	    if (temp.length != size) {
		throw new IbisException("Received sub array has wrong size");
	    }
	    System.arraycopy(temp, 0, destination, offset, size);

	} catch (IOException e) {
	    throw new IbisException("read error" + e);
	} catch (ClassCastException e2) {
	    throw new IbisException("reading wrong type in stream", e2);
	} catch (ClassNotFoundException e3) {
	    throw new IbisException("class not found" + e3);
	}
    }

    public void readSubArrayChar(char[] destination, int offset,
				 int size) throws IbisException {

	try {
	    char[] temp = (char[]) obj_in.readObject();
	    if (temp.length != size) {
		throw new IbisException("Received sub array has wrong size");
	    }
	    System.arraycopy(temp, 0, destination, offset, size);

	} catch (IOException e) {
	    throw new IbisException("read error" + e);
	} catch (ClassCastException e2) {
	    throw new IbisException("reading wrong type in stream", e2);
	} catch (ClassNotFoundException e3) {
	    throw new IbisException("class not found" + e3);
	}

    }

    public void readSubArrayShort(short[] destination, int offset,
				  int size) throws IbisException {

	try {
	    short[] temp = (short[]) obj_in.readObject();
	    if (temp.length != size) {
		throw new IbisException("Received sub array has wrong size");
	    }
	    System.arraycopy(temp, 0, destination, offset, size);

	} catch (IOException e) {
	    throw new IbisException("read error" + e);
	} catch (ClassCastException e2) {
	    throw new IbisException("reading wrong type in stream", e2);
	} catch (ClassNotFoundException e3) {
	    throw new IbisException("class not found" + e3);
	}

    }

    public void readSubArrayInt(int[] destination, int offset,
				int size) throws IbisException {

	try {
	    int[] temp = (int[]) obj_in.readObject();
	    if (temp.length != size) {
		throw new IbisException("Received sub array has wrong size");
	    }
	    System.arraycopy(temp, 0, destination, offset, size);

	} catch (IOException e) {
	    throw new IbisException("read error" + e);
	} catch (ClassCastException e2) {
	    throw new IbisException("reading wrong type in stream", e2);
	} catch (ClassNotFoundException e3) {
	    throw new IbisException("class not found" + e3);
	}

    }

    public void readSubArrayLong(long[] destination, int offset,
				 int size) throws IbisException {
	try {
	    long[] temp = (long[]) obj_in.readObject();
	    if (temp.length != size) {
		throw new IbisException("Received sub array has wrong size");
	    }
	    System.arraycopy(temp, 0, destination, offset, size);

	} catch (IOException e) {
	    throw new IbisException("read error" + e);
	} catch (ClassCastException e2) {
	    throw new IbisException("reading wrong type in stream", e2);
	} catch (ClassNotFoundException e3) {
	    throw new IbisException("class not found" + e3);
	}

    }

    public void readSubArrayFloat(float[] destination, int offset,
				  int size) throws IbisException {

	try {
	    float[] temp = (float[]) obj_in.readObject();
	    if (temp.length != size) {
		throw new IbisException("Received sub array has wrong size");
	    }
	    System.arraycopy(temp, 0, destination, offset, size);

	} catch (IOException e) {
	    throw new IbisException("read error" + e);
	} catch (ClassCastException e2) {
	    throw new IbisException("reading wrong type in stream", e2);
	} catch (ClassNotFoundException e3) {
	    throw new IbisException("class not found" + e3);
	}

    }

    public void readSubArrayDouble(double[] destination, int offset,
				   int size) throws IbisException {
	try {
	    double[] temp = (double[]) obj_in.readObject();
	    if (temp.length != size) {
		throw new IbisException("Received sub array has wrong size");
	    }
	    System.arraycopy(temp, 0, destination, offset, size);

	} catch (IOException e) {
	    throw new IbisException("read error" + e);
	} catch (ClassCastException e2) {
	    throw new IbisException("reading wrong type in stream", e2);
	} catch (ClassNotFoundException e3) {
	    throw new IbisException("class not found" + e3);
	}
    }

}
