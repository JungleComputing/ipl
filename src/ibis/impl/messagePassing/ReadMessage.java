package ibis.ipl.impl.messagePassing;

import java.io.IOException;

import ibis.ipl.IbisException;

class ReadMessage implements ibis.ipl.ReadMessage {

    long sequenceNr = -1;
    ReceivePort port;
    ShadowSendPort shadowSendPort;

    int pandaMessage;

    ibis.ipl.impl.messagePassing.ReadMessage next;

    ReadMessage(ibis.ipl.SendPort s, ReceivePort port, int msg) {
	// This is already taken: synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis)
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();

// System.err.println("**************************************************Creating new ReadMessage");

	this.port = port;
	this.shadowSendPort = (ShadowSendPort)s;
	pandaMessage = msg;
    }

    public void finish() throws IbisException {
	port.finishMessage();
    }

    public ibis.ipl.SendPortIdentifier origin() {
	return shadowSendPort.identifier();
    }

    void setSequenceNumber(long s) {
	sequenceNr = s;
    }

    public long sequenceNumber() {
	return sequenceNr;
    }

    public boolean readBoolean() throws IbisException {
	throw new IbisException("Read Boolean not supported");
    }

    public byte readByte() throws IbisException {
	throw new IbisException("Read Byte not supported");
    }

    public char readChar() throws IbisException {
	throw new IbisException("Read Char not supported");
    }

    public short readShort() throws IbisException {
	throw new IbisException("Read Short not supported");
    }

    public int  readInt() throws IbisException {
	return shadowSendPort.in.read();
    }

    public long readLong() throws IbisException {
	throw new IbisException("Read Long not supported");
    }

    public float readFloat() throws IbisException {
	throw new IbisException("Read Float not supported");
    }

    public double readDouble() throws IbisException {
	throw new IbisException("Read Double not supported");
    }

    public String readString() throws IbisException {
	throw new IbisException("Read String not supported");
    }

    public Object readObject() throws IbisException {
	throw new IbisException("Read Object not supported");
    }

    public void readArrayBoolean(boolean[] destination) throws IbisException {
	throw new IbisException("Read ArrayBoolean not supported");
    }

    public void readArrayByte(byte[] destination) throws IbisException {
	shadowSendPort.in.read(destination);
    }

    public void readArrayChar(char[] destination) throws IbisException {
	throw new IbisException("Read ArrayChar not supported");
    }

    public void readArrayShort(short[] destination) throws IbisException {
	throw new IbisException("Read ArrayShort not supported");
    }

    public void readArrayInt(int[] destination) throws IbisException {
	throw new IbisException("Read ArrayInt not supported");
    }

    public void readArrayLong(long[] destination) throws IbisException {
	throw new IbisException("Read ArrayLong not supported");
    }

    public void readArrayFloat(float[] destination) throws IbisException {
	throw new IbisException("Read ArrayFloat not supported");
    }

    public void readArrayDouble(double[] destination) throws IbisException {
	throw new IbisException("Read ArrayDouble not supported");
    }

    public void readSubArrayBoolean(boolean[] destination, int offset,
				    int size) throws IbisException {
	throw new IbisException("Read SubArrayBoolean not supported");
    }

    public void readSubArrayByte(byte[] destination, int offset,
				 int size) throws IbisException {
	throw new IbisException("Read SubArrayByte not supported");
    }

    public void readSubArrayChar(char[] destination, int offset,
				 int size) throws IbisException {
	throw new IbisException("Read SubArrayChar not supported");
    }

    public void readSubArrayShort(short[] destination, int offset,
				  int size) throws IbisException {
	throw new IbisException("Read SubArrayShort not supported");
    }

    public void readSubArrayInt(int[] destination, int offset,
				int size) throws IbisException {
	throw new IbisException("Read SubArrayInt not supported");
    }

    public void readSubArrayLong(long[] destination, int offset,
				 int size) throws IbisException {
	throw new IbisException("Read SubArrayLong not supported");
    }

    public void readSubArrayFloat(float[] destination, int offset,
				  int size) throws IbisException {
	throw new IbisException("Read SubArrayFloat not supported");
    }

    public void readSubArrayDouble(double[] destination, int offset,
				   int size) throws IbisException {
	throw new IbisException("Read SubArrayDouble not supported");
    }

}
