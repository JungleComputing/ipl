package ibis.ipl.impl.messagePassing;

import java.io.BufferedOutputStream;
import java.io.ObjectOutputStream;

import ibis.ipl.IbisIOException;

final class SerializeWriteMessage extends WriteMessage {

    private static final boolean DEBUG = Ibis.DEBUG;

    ibis.io.SunSerializationOutputStream obj_out;

    SerializeWriteMessage() {
    }

    SerializeWriteMessage(SendPort sPort) throws IbisIOException {
	super(sPort);
	obj_out = ((SerializeSendPort)sPort).obj_out;
// System.err.println("**************************************************Creating new SerializeWriteMessage");
    }


    private void send(boolean doSend, boolean isReset) throws IbisIOException {
	if (DEBUG) {
	    System.err.println("%%%%%%%%%%%%%%%% Send an Ibis SerializeWriteMessage");
// Thread.dumpStack();
	}

	if (doSend) {
	    try {
		obj_out.flush();
	    } catch (java.io.IOException e) {
		throw new IbisIOException(e);
	    }
	}

	Ibis.myIbis.lock();
	try {
// out.report();
	    if (doSend) {
		out.send(true);
	    }
	    if (isReset) {
		out.reset(false);
	    }
	    sPort.registerSend();
	} finally {
	    Ibis.myIbis.unlock();
	}

	if (isReset) {
	    try {
		obj_out.reset();
	    } catch (java.io.IOException e) {
		throw new IbisIOException(e);
	    }
	}
    }


    public void send() throws IbisIOException {
	send(true, false);
    }


    public void finish() throws IbisIOException {
	try {
	    obj_out.reset();
	} catch (java.io.IOException e) {
System.err.println("SerializeWriteMessage obj_out throws exception " + e);
e.printStackTrace();
	    throw new IbisIOException(e);
	}
	out.finish(); // : Now
    }


    public void reset(boolean doSend) throws IbisIOException {
	send(doSend, true);
    }


    public void writeBoolean(boolean value) throws IbisIOException {
	try {
	    obj_out.writeBoolean(value);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeByte(byte value) throws IbisIOException {
	try {
	    obj_out.writeByte(value);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeChar(char value) throws IbisIOException {
	try {
	    obj_out.writeChar(value);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeShort(short value) throws IbisIOException {
	try {
	    obj_out.writeShort(value);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeInt(int value) throws IbisIOException {
	try {
	    obj_out.writeInt(value);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeLong(long value) throws IbisIOException {
	try {
	    obj_out.writeLong(value);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeFloat(float value) throws IbisIOException {
	try {
	    obj_out.writeFloat(value);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeDouble(double value) throws IbisIOException {
	try {
	    obj_out.writeDouble(value);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeString(String value) throws IbisIOException {
	try {
	    obj_out.writeObject(value);
// obj_out.flush();
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
// sPort.out.report();
    }

    public void writeObject(Object value) throws IbisIOException {
	try {
	    obj_out.writeObject(value);
// obj_out.flush();
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
// out.report();
    }

    public void writeArray(boolean[] value) throws IbisIOException {
	try {
	    obj_out.writeObject(value);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeArray(byte[] value) throws IbisIOException {
	try {
	    obj_out.writeObject(value);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeArray(char[] value) throws IbisIOException {
	try {
	    obj_out.writeObject(value);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeArray(short[] value) throws IbisIOException {
	try {
	    obj_out.writeObject(value);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeArray(int[] value) throws IbisIOException {
	try {
	    obj_out.writeObject(value);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeArray(long[] value) throws IbisIOException {
	try {
	    obj_out.writeObject(value);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeArray(float[] value) throws IbisIOException {
	try {
	    obj_out.writeObject(value);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeArray(double[] value) throws IbisIOException {
	try {
	    obj_out.writeObject(value);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }


    public void writeArray(boolean[] value, int offset,
					    int size) throws IbisIOException {
	boolean[] temp = new boolean[size];
	System.arraycopy(value, offset, temp, 0, size);
	try {
	    obj_out.writeObject(temp);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeArray(byte[] value, int offset,
				  int size) throws IbisIOException {
	byte[] temp = new byte[size];
	System.arraycopy(value, offset, temp, 0, size);
	try {
	    obj_out.writeObject(temp);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeArray(char[] value, int offset,
					 int size) throws IbisIOException {
	char[] temp = new char[size];
	System.arraycopy(value, offset, temp, 0, size);
	try {
	    obj_out.writeObject(temp);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeArray(short[] value, int offset,
					  int size) throws IbisIOException {
	short[] temp = new short[size];
	System.arraycopy(value, offset, temp, 0, size);
	try {
	    obj_out.writeObject(temp);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeArray(int[] value, int offset,
					int size) throws IbisIOException {
	int[] temp = new int[size];
	System.arraycopy(value, offset, temp, 0, size);
	try {
	    obj_out.writeObject(temp);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeArray(long[] value, int offset,
					 int size) throws IbisIOException {
	long[] temp = new long[size];
	System.arraycopy(value, offset, temp, 0, size);
	try {
	    obj_out.writeObject(temp);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeArray(float[] value, int offset,
					  int size) throws IbisIOException {
	float[] temp = new float[size];
	System.arraycopy(value, offset, temp, 0, size);
	try {
	    obj_out.writeObject(temp);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeArray(double[] value, int offset,
					   int size) throws IbisIOException {
	double[] temp = new double[size];
	System.arraycopy(value, offset, temp, 0, size);
	try {
	    obj_out.writeObject(temp);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

}
