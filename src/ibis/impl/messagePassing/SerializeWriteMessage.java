package ibis.ipl.impl.messagePassing;

import java.io.BufferedOutputStream;
import java.io.ObjectOutputStream;

import ibis.ipl.IbisIOException;

final class SerializeWriteMessage extends ibis.ipl.impl.messagePassing.WriteMessage {

    ObjectOutputStream obj_out;

    SerializeWriteMessage() {
    }

    SerializeWriteMessage(SendPort sPort) throws IbisIOException {
	super(sPort);
	obj_out = ((SerializeSendPort)sPort).obj_out;
// System.err.println("**************************************************Creating new SerializeWriteMessage");
    }


    private void send(boolean doSend, boolean isReset) throws IbisIOException {
	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.err.println("%%%%%%%%%%%%%%%% Send an Ibis SerializeWriteMessage");
	}

	try {
	    obj_out.flush();
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}

	ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
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
	    ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
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
	out.finish();

	try {
	    obj_out.reset();
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
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

    public void writeArrayBoolean(boolean[] value) throws IbisIOException {
	try {
	    obj_out.writeObject(value);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeArrayByte(byte[] value) throws IbisIOException {
	try {
	    obj_out.writeObject(value);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeArrayChar(char[] value) throws IbisIOException {
	try {
	    obj_out.writeObject(value);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeArrayShort(short[] value) throws IbisIOException {
	try {
	    obj_out.writeObject(value);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeArrayInt(int[] value) throws IbisIOException {
	try {
	    obj_out.writeObject(value);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeArrayLong(long[] value) throws IbisIOException {
	try {
	    obj_out.writeObject(value);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeArrayFloat(float[] value) throws IbisIOException {
	try {
	    obj_out.writeObject(value);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeArrayDouble(double[] value) throws IbisIOException {
	try {
	    obj_out.writeObject(value);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }


    public void writeSubArrayBoolean(boolean[] value, int offset,
					    int size) throws IbisIOException {
	boolean[] temp = new boolean[size];
	System.arraycopy(value, offset, temp, 0, size);
	try {
	    obj_out.writeObject(temp);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeSubArrayByte(byte[] value, int offset,
				  int size) throws IbisIOException {
	byte[] temp = new byte[size];
	System.arraycopy(value, offset, temp, 0, size);
	try {
	    obj_out.writeObject(temp);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeSubArrayChar(char[] value, int offset,
					 int size) throws IbisIOException {
	char[] temp = new char[size];
	System.arraycopy(value, offset, temp, 0, size);
	try {
	    obj_out.writeObject(temp);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeSubArrayShort(short[] value, int offset,
					  int size) throws IbisIOException {
	short[] temp = new short[size];
	System.arraycopy(value, offset, temp, 0, size);
	try {
	    obj_out.writeObject(temp);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeSubArrayInt(int[] value, int offset,
					int size) throws IbisIOException {
	int[] temp = new int[size];
	System.arraycopy(value, offset, temp, 0, size);
	try {
	    obj_out.writeObject(temp);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeSubArrayLong(long[] value, int offset,
					 int size) throws IbisIOException {
	long[] temp = new long[size];
	System.arraycopy(value, offset, temp, 0, size);
	try {
	    obj_out.writeObject(temp);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeSubArrayFloat(float[] value, int offset,
					  int size) throws IbisIOException {
	float[] temp = new float[size];
	System.arraycopy(value, offset, temp, 0, size);
	try {
	    obj_out.writeObject(temp);
	} catch (java.io.IOException e) {
	    throw new IbisIOException(e);
	}
    }

    public void writeSubArrayDouble(double[] value, int offset,
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
