package ibis.impl.messagePassing;

import java.io.IOException;

final public class IbisWriteMessage extends WriteMessage {

    private ibis.io.IbisSerializationOutputStream obj_out;

    IbisWriteMessage() {
    }

    IbisWriteMessage(SendPort p) {
	super(p);
	obj_out = ((IbisSendPort)p).obj_out;
    }


    public int send() throws IOException {
// System.err.println("Send Ibis WriteMessage " + this + ": send its ByteOutput " + out + " flush its IbisSerializationOutputStream " + obj_out);
	obj_out.flush();
	// out is flushed from obj_out.

	return out.getSentFrags();
    }


    public long finish() throws IOException {
	obj_out.flush();
	obj_out.reset();
	return super.finish();
    }

    public void reset() throws IOException {
	obj_out.reset();
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
