package ibis.ipl.impl.messagePassing;

import ibis.ipl.IbisIOException;
import java.io.IOException;

final public class IbisWriteMessage extends WriteMessage {

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
	try {
	    obj_out.flush();
	} catch (IOException e) {
	    throw new IbisIOException("got exception", e);
	}

	// Do this from obj_out: out.flush();

	Ibis.myIbis.lock();
	sPort.registerSend();	// No exceptions
	Ibis.myIbis.unlock();
    }

    private void reset(boolean doSend, boolean finish) throws IbisIOException {
// System.err.println("Reset Ibis WriteMessage " + this + " and its ByteOutput " + out + (finish ? " and also" : " but not") + " its IbisSerializationOutputStream " + obj_out);
	// Ibis.myIbis.lock();
	try {
	    if (doSend) {
		obj_out.flush();
	    }

	    obj_out.reset();
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}

	Ibis.myIbis.lock();
	try {
	    if (doSend) {
		sPort.registerSend();
	    }
	    out.reset(false);	// throws IbisIOException

	    if (finish) {
		sPort.reset();
	    }
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	} finally {
	    Ibis.myIbis.unlock();
	}
    }

    public void finish() throws IbisIOException {
	reset(false, true);
    }

    public void reset(boolean doSend) throws IbisIOException {
	reset(doSend, false);
    }

    public void writeBoolean(boolean value) throws IbisIOException {
	try {
	    obj_out.writeBoolean(value);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeByte(byte value) throws IbisIOException {
	try {
	    obj_out.writeByte(value);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeChar(char value) throws IbisIOException {
	try {
	    obj_out.writeChar(value);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeShort(short value) throws IbisIOException {
	try {
	    obj_out.writeShort(value);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeInt(int value) throws IbisIOException {
	try {
	    obj_out.writeInt(value);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeLong(long value) throws IbisIOException {
	try {
	    obj_out.writeLong(value);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeFloat(float value) throws IbisIOException {
	try {
	    obj_out.writeFloat(value);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeDouble(double value) throws IbisIOException {
	try {
	    obj_out.writeDouble(value);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeObject(Object value) throws IbisIOException {
	try {
	    obj_out.writeObject(value);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeString(String value) throws IbisIOException { 
	writeObject(value);
    }

    public void writeArraySliceBoolean(boolean[] value, int offset,
					    int size) throws IbisIOException {
	try {
	    obj_out.writeArraySliceBoolean(value, offset, size);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeArraySliceByte(byte[] value, int offset,
				         int size) throws IbisIOException {
	try {
	    obj_out.writeArraySliceByte(value, offset, size);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeArraySliceChar(char[] value, int offset,
					 int size) throws IbisIOException {
	try {
	    obj_out.writeArraySliceChar(value, offset, size);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeArraySliceShort(short[] value, int offset,
					  int size) throws IbisIOException {
	try {
	    obj_out.writeArraySliceShort(value, offset, size);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeArraySliceInt(int[] value, int offset,
					int size) throws IbisIOException {
	try {
	    obj_out.writeArraySliceInt(value, offset, size);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeArraySliceLong(long[] value, int offset,
					 int size) throws IbisIOException {
	try {
	    obj_out.writeArraySliceLong(value, offset, size);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeArraySliceFloat(float[] value, int offset,
					  int size) throws IbisIOException {
	try {
	    obj_out.writeArraySliceFloat(value, offset, size);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeArraySliceDouble(double[] value, int offset,
					   int size) throws IbisIOException {
	try {
	    obj_out.writeArraySliceDouble(value, offset, size);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeArraySliceObject(Object[] value, int offset,
					   int size) throws IbisIOException {
	try {
	    obj_out.writeArraySliceObject(value, offset, size);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }


    public void writeArrayBoolean(boolean[] value) throws IbisIOException {
	try {
	    obj_out.writeArrayBoolean(value);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeArrayByte(byte[] value) throws IbisIOException {
	try {
	    obj_out.writeArrayByte(value);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeArrayChar(char[] value) throws IbisIOException {
	try {
	    obj_out.writeArrayChar(value);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeArrayShort(short[] value) throws IbisIOException {
	try {
	    obj_out.writeArrayShort(value);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeArrayInt(int[] value) throws IbisIOException {
	try {
	    obj_out.writeArrayInt(value);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeArrayLong(long[] value) throws IbisIOException {
	try {
	    obj_out.writeArrayLong(value);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeArrayFloat(float[] value) throws IbisIOException {
	try {
	    obj_out.writeArrayFloat(value);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeArrayDouble(double[] value) throws IbisIOException {
	try {
	    obj_out.writeArrayDouble(value);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeArrayObject(Object[] value) throws IbisIOException {
	try {
	    obj_out.writeArrayObject(value);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }
}
