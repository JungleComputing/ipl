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


    private void reset(boolean doSend, boolean finish)
	    throws IbisIOException {
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
	    out.reset(finish);	// throws IbisIOException
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

    public void writeArray(boolean[] value, int offset,
					    int size) throws IbisIOException {
	try {
	    obj_out.writeArray(value, offset, size);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeArray(byte[] value, int offset,
				         int size) throws IbisIOException {
	try {
	    obj_out.writeArray(value, offset, size);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeArray(char[] value, int offset,
					 int size) throws IbisIOException {
	try {
	    obj_out.writeArray(value, offset, size);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeArray(short[] value, int offset,
					  int size) throws IbisIOException {
	try {
	    obj_out.writeArray(value, offset, size);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeArray(int[] value, int offset,
					int size) throws IbisIOException {
	try {
	    obj_out.writeArray(value, offset, size);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeArray(long[] value, int offset,
					 int size) throws IbisIOException {
	try {
	    obj_out.writeArray(value, offset, size);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeArray(float[] value, int offset,
					  int size) throws IbisIOException {
	try {
	    obj_out.writeArray(value, offset, size);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeArray(double[] value, int offset,
					   int size) throws IbisIOException {
	try {
	    obj_out.writeArray(value, offset, size);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeArray(Object[] value, int offset,
					   int size) throws IbisIOException {
	try {
	    obj_out.writeArray(value, offset, size);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }


    public void writeArray(boolean[] value) throws IbisIOException {
	try {
	    obj_out.writeArray(value);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeArray(byte[] value) throws IbisIOException {
	try {
	    obj_out.writeArray(value);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeArray(char[] value) throws IbisIOException {
	try {
	    obj_out.writeArray(value);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeArray(short[] value) throws IbisIOException {
	try {
	    obj_out.writeArray(value);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeArray(int[] value) throws IbisIOException {
	try {
	    obj_out.writeArray(value);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeArray(long[] value) throws IbisIOException {
	try {
	    obj_out.writeArray(value);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeArray(float[] value) throws IbisIOException {
	try {
	    obj_out.writeArray(value);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeArray(double[] value) throws IbisIOException {
	try {
	    obj_out.writeArray(value);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }

    public void writeArray(Object[] value) throws IbisIOException {
	try {
	    obj_out.writeArray(value);
	} catch(IOException e) {
	    throw new IbisIOException("got exception", e);
	}
    }
}
