package ibis.impl.messagePassing;

import java.io.IOException;

final class SerializeWriteMessage extends WriteMessage {

    private static final boolean DEBUG = Ibis.DEBUG;

    ibis.io.SunSerializationOutputStream obj_out;

    SerializeWriteMessage() {
    }

    SerializeWriteMessage(SendPort sPort) throws IOException {
	super(sPort);
	obj_out = ((SerializeSendPort)sPort).obj_out;
// System.err.println("**************************************************Creating new SerializeWriteMessage");
    }


    private void send(boolean doSend, boolean isReset) throws IOException {
	if (DEBUG) {
	    System.err.println("%%%%%%%%%%%%%%%% Send an Ibis SerializeWriteMessage");
// Thread.dumpStack();
	}

	if (doSend) {
	    obj_out.flush();
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
	    obj_out.reset();
	}
    }


    public void send() throws IOException {
	send(true, false);
    }


    public void finish() throws IOException {
	obj_out.reset();
	out.finish(); // : Now
    }


    public void reset(boolean doSend) throws IOException {
	send(doSend, true);
    }


    public void writeBoolean(boolean value) throws IOException {
	obj_out.writeBoolean(value);
    }

    public void writeByte(byte value) throws IOException {
	obj_out.writeByte(value);
    }

    public void writeChar(char value) throws IOException {
	obj_out.writeChar(value);
    }

    public void writeShort(short value) throws IOException {
	obj_out.writeShort(value);
    }

    public void writeInt(int value) throws IOException {
	obj_out.writeInt(value);
    }

    public void writeLong(long value) throws IOException {
	obj_out.writeLong(value);
    }

    public void writeFloat(float value) throws IOException {
	obj_out.writeFloat(value);
    }

    public void writeDouble(double value) throws IOException {
	obj_out.writeDouble(value);
    }

    public void writeString(String value) throws IOException {
	obj_out.writeObject(value);
// obj_out.flush();
// sPort.out.report();
    }

    public void writeObject(Object value) throws IOException {
	obj_out.writeObject(value);
// obj_out.flush();
// out.report();
    }

    public void writeArray(boolean[] value) throws IOException {
	obj_out.writeObject(value);
    }

    public void writeArray(byte[] value) throws IOException {
	obj_out.writeObject(value);
    }

    public void writeArray(char[] value) throws IOException {
	obj_out.writeObject(value);
    }

    public void writeArray(short[] value) throws IOException {
	obj_out.writeObject(value);
    }

    public void writeArray(int[] value) throws IOException {
	obj_out.writeObject(value);
    }

    public void writeArray(long[] value) throws IOException {
	obj_out.writeObject(value);
    }

    public void writeArray(float[] value) throws IOException {
	obj_out.writeObject(value);
    }

    public void writeArray(double[] value) throws IOException {
	obj_out.writeObject(value);
    }


    public void writeArray(boolean[] value, int offset, int size)
	    throws IOException {
	boolean[] temp = new boolean[size];
	System.arraycopy(value, offset, temp, 0, size);
	obj_out.writeObject(temp);
    }

    public void writeArray(byte[] value, int offset, int size)
	    throws IOException {
	byte[] temp = new byte[size];
	System.arraycopy(value, offset, temp, 0, size);
	obj_out.writeObject(temp);
    }

    public void writeArray(char[] value, int offset, int size)
	    throws IOException {
	char[] temp = new char[size];
	System.arraycopy(value, offset, temp, 0, size);
	obj_out.writeObject(temp);
    }

    public void writeArray(short[] value, int offset, int size)
	    throws IOException {
	short[] temp = new short[size];
	System.arraycopy(value, offset, temp, 0, size);
	obj_out.writeObject(temp);
    }

    public void writeArray(int[] value, int offset, int size)
	    throws IOException {
	int[] temp = new int[size];
	System.arraycopy(value, offset, temp, 0, size);
	obj_out.writeObject(temp);
    }

    public void writeArray(long[] value, int offset, int size)
	    throws IOException {
	long[] temp = new long[size];
	System.arraycopy(value, offset, temp, 0, size);
	obj_out.writeObject(temp);
    }

    public void writeArray(float[] value, int offset, int size)
	    throws IOException {
	float[] temp = new float[size];
	System.arraycopy(value, offset, temp, 0, size);
	obj_out.writeObject(temp);
    }

    public void writeArray(double[] value, int offset, int size)
	    throws IOException {
	double[] temp = new double[size];
	System.arraycopy(value, offset, temp, 0, size);
	obj_out.writeObject(temp);
    }

}
