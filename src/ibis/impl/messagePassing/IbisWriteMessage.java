package ibis.ipl.impl.messagePassing;

import ibis.ipl.IbisIOException;

final public class IbisWriteMessage extends ibis.ipl.impl.messagePassing.WriteMessage {

    SendPort sPort;
    ibis.io.IbisSerializationOutputStream obj_out;


    IbisWriteMessage() {
    }

    IbisWriteMessage(SendPort p) {
	sPort = p;
	out = p.out;
	obj_out = ((IbisSendPort)p).obj_out;
    }


    public void send() throws IbisIOException {
// System.err.println("Send Ibis WriteMessage " + this + ": send its ByteOutput " + out + " flush its IbisSerializationOutputStream " + obj_out);
	obj_out.flush();
	// Do this from obj_out: out.flush();

	ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	sPort.registerSend();	// No exceptions
	ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
    }

    private void reset(boolean doSend, boolean finish) throws IbisIOException {
// System.err.println("Reset Ibis WriteMessage " + this + " and its ByteOutput " + out + (finish ? " and also" : " but not") + " its IbisSerializationOutputStream " + obj_out);
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	if (doSend) {
	    obj_out.flush();
	}

	obj_out.reset();

	ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	try {
	    if (doSend) {
		sPort.registerSend();
	    }
	    out.reset(false);	// throws IbisIOException

	    if (finish) {
		sPort.reset();
	    }
	} finally {
	    ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
	}
    }

    public void finish() throws IbisIOException {
	reset(false, true);
    }

    public void reset(boolean doSend) throws IbisIOException {
	reset(doSend, false);
    }

    public void writeBoolean(boolean value) throws IbisIOException {
	obj_out.writeBoolean(value);
    }

    public void writeByte(byte value) throws IbisIOException {
	obj_out.writeByte(value);
    }

    public void writeChar(char value) throws IbisIOException {
	obj_out.writeChar(value);
    }

    public void writeShort(short value) throws IbisIOException {
	obj_out.writeShort(value);
    }

    public void writeInt(int value) throws IbisIOException {
	obj_out.writeInt(value);
    }

    public void writeLong(long value) throws IbisIOException {
	obj_out.writeLong(value);
    }

    public void writeFloat(float value) throws IbisIOException {
	obj_out.writeFloat(value);
    }

    public void writeDouble(double value) throws IbisIOException {
	obj_out.writeDouble(value);
    }

    public void writeObject(Object value) throws IbisIOException {
	obj_out.writeObject(value);
    }

    public void writeString(String value) throws IbisIOException { 
	writeObject(value);
    }

    public void writeArraySliceBoolean(boolean[] value, int offset,
					    int size) throws IbisIOException {
	obj_out.writeArraySliceBoolean(value, offset, size);
    }

    public void writeArraySliceByte(byte[] value, int offset,
				         int size) throws IbisIOException {
	obj_out.writeArraySliceByte(value, offset, size);
    }

    public void writeArraySliceChar(char[] value, int offset,
					 int size) throws IbisIOException {
	obj_out.writeArraySliceChar(value, offset, size);
    }

    public void writeArraySliceShort(short[] value, int offset,
					  int size) throws IbisIOException {
	obj_out.writeArraySliceShort(value, offset, size);
    }

    public void writeArraySliceInt(int[] value, int offset,
					int size) throws IbisIOException {
	obj_out.writeArraySliceInt(value, offset, size);
    }

    public void writeArraySliceLong(long[] value, int offset,
					 int size) throws IbisIOException {
	obj_out.writeArraySliceLong(value, offset, size);
    }

    public void writeArraySliceFloat(float[] value, int offset,
					  int size) throws IbisIOException {
	obj_out.writeArraySliceFloat(value, offset, size);
    }

    public void writeArraySliceDouble(double[] value, int offset,
					   int size) throws IbisIOException {
	obj_out.writeArraySliceDouble(value, offset, size);
    }

    public void writeArraySliceObject(Object[] value, int offset,
					   int size) throws IbisIOException {
	obj_out.writeArraySliceObject(value, offset, size);
    }


    public void writeArrayBoolean(boolean[] value) throws IbisIOException {
	obj_out.writeArrayBoolean(value);
    }

    public void writeArrayByte(byte[] value) throws IbisIOException {
	obj_out.writeArrayByte(value);
    }

    public void writeArrayChar(char[] value) throws IbisIOException {
	obj_out.writeArrayChar(value);
    }

    public void writeArrayShort(short[] value) throws IbisIOException {
	obj_out.writeArrayShort(value);
    }

    public void writeArrayInt(int[] value) throws IbisIOException {
	obj_out.writeArrayInt(value);
    }

    public void writeArrayLong(long[] value) throws IbisIOException {
	obj_out.writeArrayLong(value);
    }

    public void writeArrayFloat(float[] value) throws IbisIOException {
	obj_out.writeArrayFloat(value);
    }

    public void writeArrayDouble(double[] value) throws IbisIOException {
	obj_out.writeArrayDouble(value);
    }

    public void writeArrayObject(Object[] value) throws IbisIOException {
	obj_out.writeArrayObject(value);
    }
}
