package ibis.ipl.impl.messagePassing;

import ibis.ipl.IbisIOException;

class WriteMessage implements ibis.ipl.WriteMessage {

    SendPort sPort;
    ByteOutputStream out;


    WriteMessage() {
    }

    WriteMessage(SendPort sPort) {
	if (Ibis.DEBUG) {
	    System.err.println("**************************************************Creating new SendPort port = " + sPort);
	}
	this.sPort = sPort;
	out = sPort.out;
    }


    public int getCount() {
	return out.getCount();
    }

    public void resetCount() {
	out.resetCount();
    }


    private void send(boolean doSend, boolean isReset) throws IbisIOException {
	if (Ibis.DEBUG) {
	    System.err.println("%%%%%%%%%%%%%%% Send an Ibis /no-serial/ WriteMessage");
	}

	Ibis.myIbis.lock();
	try {
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
    }


    public void send() throws IbisIOException {
	send(true, false);
    }


    public void finish() throws IbisIOException {
	out.finish();
    }


    public void reset(boolean doSend) throws IbisIOException {
	send(doSend, true);
    }


    public void writeBoolean(boolean value) throws IbisIOException {
	throw new IbisIOException("Write Boolean not supported");
    }

    public void writeByte(byte value) throws IbisIOException {
	out.write(value);
    }

    public void writeChar(char value) throws IbisIOException {
	throw new IbisIOException("Write Char not supported");
    }

    public void writeShort(short value) throws IbisIOException {
	throw new IbisIOException("Write Short not supported");
    }

    public void writeInt(int value) throws IbisIOException {
	throw new IbisIOException("Write Int not supported");
    }

    public void writeLong(long value) throws IbisIOException {
	throw new IbisIOException("Write Long not supported");
    }

    public void writeFloat(float value) throws IbisIOException {
	throw new IbisIOException("Write Float not supported");
    }

    public void writeDouble(double value) throws IbisIOException {
	throw new IbisIOException("Write Double not supported");
    }

    public void writeString(String value) throws IbisIOException {
	throw new IbisIOException("Write String not supported");
    }

    public void writeObject(Object value) throws IbisIOException {
	throw new IbisIOException("Write Object not supported");
    }

    public void writeArray(boolean[] value) throws IbisIOException {
	writeObject(value);
    }

    public void writeArray(byte[] value) throws IbisIOException {
	out.write(value);
    }

    public void writeArray(char[] value) throws IbisIOException {
	writeObject(value);
    }

    public void writeArray(short[] value) throws IbisIOException {
	writeObject(value);
    }

    public void writeArray(int[] value) throws IbisIOException {
	writeObject(value);
    }

    public void writeArray(long[] value) throws IbisIOException {
	writeObject(value);
    }

    public void writeArray(float[] value) throws IbisIOException {
	writeObject(value);
    }

    public void writeArray(double[] value) throws IbisIOException {
	writeObject(value);
    }

    public void writeArray(Object[] value) throws IbisIOException {
	writeObject(value);
    }


    public void writeArray(boolean[] value, int offset,
					    int size) throws IbisIOException {
	boolean[] temp = new boolean[size];
	System.arraycopy(value, offset, temp, 0, size);
	writeObject(temp);
    }

    public void writeArray(byte[] value, int offset,
				  int size) throws IbisIOException {
	byte[] temp = new byte[size];
	System.arraycopy(value, offset, temp, 0, size);
	writeObject(temp);
    }

    public void writeArray(char[] value, int offset,
					 int size) throws IbisIOException {
	char[] temp = new char[size];
	System.arraycopy(value, offset, temp, 0, size);
	writeObject(temp);
    }

    public void writeArray(short[] value, int offset,
					  int size) throws IbisIOException {
	short[] temp = new short[size];
	System.arraycopy(value, offset, temp, 0, size);
	writeObject(temp);
    }

    public void writeArray(int[] value, int offset,
					int size) throws IbisIOException {
	int[] temp = new int[size];
	System.arraycopy(value, offset, temp, 0, size);
	writeObject(temp);
    }

    public void writeArray(long[] value, int offset,
					 int size) throws IbisIOException {
	long[] temp = new long[size];
	System.arraycopy(value, offset, temp, 0, size);
	writeObject(temp);
    }

    public void writeArray(float[] value, int offset,
					  int size) throws IbisIOException {
	float[] temp = new float[size];
	System.arraycopy(value, offset, temp, 0, size);
	writeObject(temp);
    }

    public void writeArray(double[] value, int offset,
					   int size) throws IbisIOException {
	double[] temp = new double[size];
	System.arraycopy(value, offset, temp, 0, size);
	writeObject(temp);
    }

    public void writeArray(Object[] value, int offset,
					   int size) throws IbisIOException {
	Object[] temp = new Object[size];
	System.arraycopy(value, offset, temp, 0, size);
	writeObject(temp);
    }
}
