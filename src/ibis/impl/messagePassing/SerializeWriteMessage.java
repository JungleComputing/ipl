package ibis.ipl.impl.messagePassing;

import java.io.BufferedOutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;

import ibis.ipl.IbisException;

class SerializeWriteMessage extends ibis.ipl.impl.messagePassing.WriteMessage {

    ibis.ipl.impl.messagePassing.ByteOutputStream out;
    ObjectOutputStream obj_out;

    SerializeWriteMessage(SendPort sPort) throws IbisException {
	super(sPort);

// System.err.println("**************************************************Creating new SerializeWriteMessage");

	this.sPort = sPort;

	out = ibis.ipl.impl.messagePassing.Ibis.myIbis.createByteOutputStream(sPort);
	try {
	    obj_out = new ObjectOutputStream(new BufferedOutputStream((java.io.OutputStream)out));
// System.err.println(Thread.currentThread() + "Created ObjectOutputStream " + obj_out);
	} catch(IOException e) {
	    throw new IbisException("Could not create message implementation", e);
	}

    }

    public void send() throws IbisException {
	try {
	    /* sPort. */ obj_out.flush();
// /* sPort. */ out.report();
	    /* sPort. */ out.flush();
	    /* sPort. */ out.send();
	    sPort.registerSend();
	} catch (IOException e) {
	    throw new IbisException("Write error", e);
	}
    }

    public void finish() throws IbisException {
	/* sPort. */ out.finish();
	try {
	    /* sPort. */ obj_out.reset();
	} catch (IOException e) {
	    throw new IbisException("ObjectOutputStream reset fails: " + e);
	}
    }


    public void reset() throws IbisException {
	/* sPort. */ out.reset();
	try {
	    /* sPort. */ obj_out.reset();
	} catch (IOException e) {
	    throw new IbisException("ObjectOutputStream reset fails: " + e);
	}
    }


    public void writeBoolean(boolean value) throws IbisException {
	try {
	    /* sPort. */ obj_out.writeBoolean(value);
	} catch (IOException e) {
	    throw new IbisException("Write error", e);
	}
    }

    public void writeByte(byte value) throws IbisException {
	try {
	    /* sPort. */ obj_out.writeByte(value);
	} catch (IOException e) {
	    throw new IbisException("Write error", e);
	}
    }

    public void writeChar(char value) throws IbisException {
	try {
	    /* sPort. */ obj_out.writeChar(value);
	} catch (IOException e) {
	    throw new IbisException("Write error", e);
	}
    }

    public void writeShort(short value) throws IbisException {
	try {
	    /* sPort. */ obj_out.writeShort(value);
	} catch (IOException e) {
	    throw new IbisException("Write error", e);
	}
    }

    public void writeInt(int value) throws IbisException {
	try {
	    /* sPort. */ obj_out.writeInt(value);
	} catch (IOException e) {
	    throw new IbisException("Write error", e);
	}
    }

    public void writeLong(long value) throws IbisException {
	try {
	    /* sPort. */ obj_out.writeLong(value);
	} catch (IOException e) {
	    throw new IbisException("Write error", e);
	}
    }

    public void writeFloat(float value) throws IbisException {
	try {
	    /* sPort. */ obj_out.writeFloat(value);
	} catch (IOException e) {
	    throw new IbisException("Write error", e);
	}
    }

    public void writeDouble(double value) throws IbisException {
	try {
	    /* sPort. */ obj_out.writeDouble(value);
	} catch (IOException e) {
	    throw new IbisException("Write error", e);
	}
    }

    public void writeString(String value) throws IbisException {
	try {
	    /* sPort. */obj_out.writeObject(value);
// sPort.obj_out.flush();
// sPort.out.report();
	} catch (IOException e) {
	    throw new IbisException("Write error", e);
	}
    }

    public void writeObject(Object value) throws IbisException {
	try {
	    /* sPort. */ obj_out.writeObject(value);
// /* sPort. */ obj_out.flush();
// /* sPort. */ out.report();
	} catch (IOException e) {
	    throw new IbisException("Write error", e);
	}
    }

    public void writeArrayBoolean(boolean[] value) throws IbisException {
	try {
	    /* sPort. */ obj_out.writeObject(value);
	} catch (IOException e) {
	    throw new IbisException("Write error", e);
	}
    }

    public void writeArrayByte(byte[] value) throws IbisException {
	try {
	    /* sPort. */ obj_out.writeObject(value);
	} catch (IOException e) {
	    throw new IbisException("Write error", e);
	}
    }

    public void writeArrayChar(char[] value) throws IbisException {
	try {
	    /* sPort. */ obj_out.writeObject(value);
	} catch (IOException e) {
	    throw new IbisException("Write error", e);
	}
    }

    public void writeArrayShort(short[] value) throws IbisException {
	try {
	    /* sPort. */ obj_out.writeObject(value);
	} catch (IOException e) {
	    throw new IbisException("Write error", e);
	}
    }

    public void writeArrayInt(int[] value) throws IbisException {
	try {
	    /* sPort. */ obj_out.writeObject(value);
	} catch (IOException e) {
	    throw new IbisException("Write error", e);
	}
    }

    public void writeArrayLong(long[] value) throws IbisException {
	try {
	    /* sPort. */ obj_out.writeObject(value);
	} catch (IOException e) {
	    throw new IbisException("Write error", e);
	}
    }

    public void writeArrayFloat(float[] value) throws IbisException {
	try {
	    /* sPort. */ obj_out.writeObject(value);
	} catch (IOException e) {
	    throw new IbisException("Write error", e);
	}
    }

    public void writeArrayDouble(double[] value) throws IbisException {
	try {
	    /* sPort. */ obj_out.writeObject(value);
	} catch (IOException e) {
	    throw new IbisException("Write error", e);
	}
    }


    public void writeSubArrayBoolean(boolean[] value, int offset,
					    int size) throws IbisException {
	try {
	    boolean[] temp = new boolean[size];
	    System.arraycopy(value, offset, temp, 0, size);
	    /* sPort. */ obj_out.writeObject(temp);
	} catch (IOException e) {
	    throw new IbisException("Write error", e);
	}
    }

    public void writeSubArrayByte(byte[] value, int offset,
				  int size) throws IbisException {
	try {
	    byte[] temp = new byte[size];
	    System.arraycopy(value, offset, temp, 0, size);
	    /* sPort. */ obj_out.writeObject(temp);
	} catch (IOException e) {
	    throw new IbisException("Write error", e);
	}
    }

    public void writeSubArrayChar(char[] value, int offset,
					 int size) throws IbisException {
	try {
	    char[] temp = new char[size];
	    System.arraycopy(value, offset, temp, 0, size);
	    /* sPort. */ obj_out.writeObject(temp);
	} catch (IOException e) {
	    throw new IbisException("Write error", e);
	}
    }

    public void writeSubArrayShort(short[] value, int offset,
					  int size) throws IbisException {
	try {
	    short[] temp = new short[size];
	    System.arraycopy(value, offset, temp, 0, size);
	    /* sPort. */ obj_out.writeObject(temp);
	} catch (IOException e) {
	    throw new IbisException("Write error", e);
	}
    }

    public void writeSubArrayInt(int[] value, int offset,
					int size) throws IbisException {
	try {
	    int[] temp = new int[size];
	    System.arraycopy(value, offset, temp, 0, size);
	    /* sPort. */ obj_out.writeObject(temp);
	} catch (IOException e) {
	    throw new IbisException("Write error", e);
	}
    }

    public void writeSubArrayLong(long[] value, int offset,
					 int size) throws IbisException {
	try {
	    long[] temp = new long[size];
	    System.arraycopy(value, offset, temp, 0, size);
	    /* sPort. */ obj_out.writeObject(temp);
	} catch (IOException e) {
	    throw new IbisException("Write error", e);
	}
    }

    public void writeSubArrayFloat(float[] value, int offset,
					  int size) throws IbisException {
	try {
	    float[] temp = new float[size];
	    System.arraycopy(value, offset, temp, 0, size);
	    /* sPort. */ obj_out.writeObject(temp);
	} catch (IOException e) {
	    throw new IbisException("Write error", e);
	}
    }

    public void writeSubArrayDouble(double[] value, int offset,
					   int size) throws IbisException {
	try {
	    double[] temp = new double[size];
	    System.arraycopy(value, offset, temp, 0, size);
	    /* sPort. */ obj_out.writeObject(temp);
	} catch (IOException e) {
	    throw new IbisException("Write error", e);
	}
    }

}
