package ibis.ipl.impl.messagePassing;

import java.io.ObjectOutputStream;
import java.io.IOException;

import ibis.ipl.IbisException;

class WriteMessage implements ibis.ipl.WriteMessage {

    SendPort sPort;
    ibis.ipl.impl.messagePassing.ByteOutputStream out;

    WriteMessage(SendPort sPort) {
	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.err.println("**************************************************Creating new SendPort port = " + sPort);
	}
	this.sPort = sPort;

	out = ibis.ipl.impl.messagePassing.Ibis.myIbis.createByteOutputStream(sPort);
    }

    public void send() throws IbisException {
// /* sPort. */out.report();
	// long t = Ibis.currentTime();
	/* sPort. */out.flush();
	/* sPort. */out.send();
	sPort.registerSend();
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.tSend += Ibis.currentTime() - t;
    }

    public void finish() throws IbisException {
	/* sPort. */out.finish();
    }

    public void reset() throws IbisException {
	/* sPort. */out.reset();
    }


    public void writeBoolean(boolean value) throws IbisException {
	throw new IbisException("Write Boolean not supported");
    }

    public void writeByte(byte value) throws IbisException {
	throw new IbisException("Write Byte not supported");
    }

    public void writeChar(char value) throws IbisException {
	throw new IbisException("Write Char not supported");
    }

    public void writeShort(short value) throws IbisException {
	throw new IbisException("Write Short not supported");
    }

    public void writeInt(int value) throws IbisException {
	/* sPort. */out.write(value);
    }

    public void writeLong(long value) throws IbisException {
	throw new IbisException("Write Long not supported");
    }

    public void writeFloat(float value) throws IbisException {
	throw new IbisException("Write Float not supported");
    }

    public void writeDouble(double value) throws IbisException {
	throw new IbisException("Write Double not supported");
    }

    public void writeString(String value) throws IbisException {
	throw new IbisException("Write String not supported");
    }

    public void writeObject(Object value) throws IbisException {
	throw new IbisException("Write Object not supported");
    }

    public void writeArrayBoolean(boolean[] value) throws IbisException {
	writeObject(value);
    }

    public void writeArrayByte(byte[] value) throws IbisException {
	/* sPort. */out.write(value);
    }

    public void writeArrayChar(char[] value) throws IbisException {
	writeObject(value);
    }

    public void writeArrayShort(short[] value) throws IbisException {
	writeObject(value);
    }

    public void writeArrayInt(int[] value) throws IbisException {
	writeObject(value);
    }

    public void writeArrayLong(long[] value) throws IbisException {
	writeObject(value);
    }

    public void writeArrayFloat(float[] value) throws IbisException {
	writeObject(value);
    }

    public void writeArrayDouble(double[] value) throws IbisException {
	writeObject(value);
    }


    public void writeSubArrayBoolean(boolean[] value, int offset,
					    int size) throws IbisException {
	boolean[] temp = new boolean[size];
	System.arraycopy(value, offset, temp, 0, size);
	writeObject(temp);
    }

    public void writeSubArrayByte(byte[] value, int offset,
				  int size) throws IbisException {
	byte[] temp = new byte[size];
	System.arraycopy(value, offset, temp, 0, size);
	writeObject(temp);
    }

    public void writeSubArrayChar(char[] value, int offset,
					 int size) throws IbisException {
	char[] temp = new char[size];
	System.arraycopy(value, offset, temp, 0, size);
	writeObject(temp);
    }

    public void writeSubArrayShort(short[] value, int offset,
					  int size) throws IbisException {
	short[] temp = new short[size];
	System.arraycopy(value, offset, temp, 0, size);
	writeObject(temp);
    }

    public void writeSubArrayInt(int[] value, int offset,
					int size) throws IbisException {
	int[] temp = new int[size];
	System.arraycopy(value, offset, temp, 0, size);
	writeObject(temp);
    }

    public void writeSubArrayLong(long[] value, int offset,
					 int size) throws IbisException {
	long[] temp = new long[size];
	System.arraycopy(value, offset, temp, 0, size);
	writeObject(temp);
    }

    public void writeSubArrayFloat(float[] value, int offset,
					  int size) throws IbisException {
	float[] temp = new float[size];
	System.arraycopy(value, offset, temp, 0, size);
	writeObject(temp);
    }

    public void writeSubArrayDouble(double[] value, int offset,
					   int size) throws IbisException {
	double[] temp = new double[size];
	System.arraycopy(value, offset, temp, 0, size);
	writeObject(temp);
    }

}
