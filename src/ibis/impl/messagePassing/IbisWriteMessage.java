package ibis.impl.messagePassing;

import java.io.IOException;

final public class IbisWriteMessage extends WriteMessage {

    private SendPort sPort;
    private ibis.io.IbisSerializationOutputStream obj_out;

    IbisWriteMessage() {
    }

    IbisWriteMessage(SendPort p) {
	sPort = p;
	out = p.out;
	obj_out = ((IbisSendPort)p).obj_out;
    }


    public int send() throws IOException {
// System.err.println("Send Ibis WriteMessage " + this + ": send its ByteOutput " + out + " flush its IbisSerializationOutputStream " + obj_out);
	obj_out.flush();

	// Do this from obj_out: out.flush();

	Ibis.myIbis.lock();
	sPort.registerSend();
	Ibis.myIbis.unlock();
	return 0;
    }


    private void reset(boolean doSend, boolean finish) throws IOException {
// System.err.println("Reset Ibis WriteMessage " + this + " and its ByteOutput " + out + (finish ? " and also" : " but not") + " its IbisSerializationOutputStream " + obj_out);
	// Ibis.myIbis.lock();
	if (doSend) {
	    obj_out.flush();
	}

	obj_out.reset();

	Ibis.myIbis.lock();
	try {
	    if (doSend) {
		sPort.registerSend();
	    }
	    out.reset(finish);
	} finally {
	    Ibis.myIbis.unlock();
	}
    }


    public long finish() throws IOException {
	reset(false, true);
	long after = (long) out.getCount();
	long retval = after - before;
	sPort.count += retval;
	before = after;
	return retval;
    }

    public void reset() throws IOException {
	reset(false, false);
	long after = (long) out.getCount();
	sPort.count += after - before;
	before = after;
    }

    public void sync(int ticket) throws IOException {
	Ibis.myIbis.lock();
	try {
	    out.reset(true);
	} finally {
	    Ibis.myIbis.unlock();
	}
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

    public void writeObject(Object value) throws IOException {
	obj_out.writeObject(value);
    }

    public void writeString(String value) throws IOException { 
	writeObject(value);
    }

    public void writeArray(boolean[] value, int offset, int size)
	    throws IOException {
	obj_out.writeArray(value, offset, size);
    }

    public void writeArray(byte[] value, int offset, int size)
	    throws IOException {
	obj_out.writeArray(value, offset, size);
    }

    public void writeArray(char[] value, int offset, int size)
	    throws IOException {
	obj_out.writeArray(value, offset, size);
    }

    public void writeArray(short[] value, int offset, int size)
	    throws IOException {
	obj_out.writeArray(value, offset, size);
    }

    public void writeArray(int[] value, int offset, int size)
	    throws IOException {
	obj_out.writeArray(value, offset, size);
    }

    public void writeArray(long[] value, int offset, int size)
	    throws IOException {
	obj_out.writeArray(value, offset, size);
    }

    public void writeArray(float[] value, int offset, int size)
	    throws IOException {
	obj_out.writeArray(value, offset, size);
    }

    public void writeArray(double[] value, int offset, int size)
	    throws IOException {
	obj_out.writeArray(value, offset, size);
    }

    public void writeArray(Object[] value, int offset, int size)
	    throws IOException {
	obj_out.writeArray(value, offset, size);
    }


    public void writeArray(boolean[] value) throws IOException {
	obj_out.writeArray(value);
    }

    public void writeArray(byte[] value) throws IOException {
	obj_out.writeArray(value);
    }

    public void writeArray(char[] value) throws IOException {
	obj_out.writeArray(value);
    }

    public void writeArray(short[] value) throws IOException {
	obj_out.writeArray(value);
    }

    public void writeArray(int[] value) throws IOException {
	obj_out.writeArray(value);
    }

    public void writeArray(long[] value) throws IOException {
	obj_out.writeArray(value);
    }

    public void writeArray(float[] value) throws IOException {
	obj_out.writeArray(value);
    }

    public void writeArray(double[] value) throws IOException {
	obj_out.writeArray(value);
    }

    public void writeArray(Object[] value) throws IOException {
	obj_out.writeArray(value);
    }
}
