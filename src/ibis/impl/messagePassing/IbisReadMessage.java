package ibis.ipl.impl.messagePassing;

import ibis.ipl.IbisIOException;

final public class IbisReadMessage extends ReadMessage {

    ibis.io.IbisSerializationInputStream obj_in;

    IbisReadMessage(ibis.ipl.SendPort origin,
		     ReceivePort port) {
	super(origin, port);
	obj_in = ((IbisShadowSendPort)origin).obj_in;
    }

    public boolean readBoolean() throws IbisIOException {
	return obj_in.readBoolean();
    }

    public byte readByte() throws IbisIOException {
	return obj_in.readByte();
    }

    public char readChar() throws IbisIOException {
	return obj_in.readChar();
    }

    public short readShort() throws IbisIOException {
	return obj_in.readShort();
    }

    public int readInt() throws IbisIOException {
	return obj_in.readInt();
    }

    public long readLong() throws IbisIOException {
	return obj_in.readLong();
    }

    public float readFloat() throws IbisIOException {
	return obj_in.readFloat();
    }

    public double readDouble() throws IbisIOException {
	return obj_in.readDouble();
    }

    public Object readObject() throws IbisIOException {
	return obj_in.readObject();
    }

    public String readString() throws IbisIOException { 
	return (String) readObject();
    } 

    public void readArrayBoolean(boolean[] destination) throws IbisIOException {
	obj_in.readArrayBoolean(destination);
    }

    public void readArrayByte(byte[] destination) throws IbisIOException {
	obj_in.readArrayByte(destination);
    }

    public void readArrayChar(char[] destination) throws IbisIOException {
	obj_in.readArrayChar(destination);
    }

    public void readArrayShort(short[] destination) throws IbisIOException {
	obj_in.readArrayShort(destination);
    }

    public void readArrayInt(int[] destination) throws IbisIOException {
	obj_in.readArrayInt(destination);
    }

    public void readArrayLong(long[] destination) throws IbisIOException {
	obj_in.readArrayLong(destination);
    }

    public void readArrayFloat(float[] destination) throws IbisIOException {
	obj_in.readArrayFloat(destination);
    }

    public void readArrayDouble(double[] destination) throws IbisIOException {
	obj_in.readArrayDouble(destination);
    }

    public void readArrayObject(Object[] destination) throws IbisIOException {
	obj_in.readArrayObject(destination);
    }

    public void readArraySliceBoolean(boolean[] destination, int offset,
					   int size) throws IbisIOException {
	obj_in.readArraySliceBoolean(destination, offset, size);
    }

    public void readArraySliceByte(byte[] destination, int offset,
					int size) throws IbisIOException {
	obj_in.readArraySliceByte(destination, offset, size);
    }

    public void readArraySliceChar(char[] destination, int offset,
					int size) throws IbisIOException {
	obj_in.readArraySliceChar(destination, offset, size);
    }

    public void readArraySliceShort(short[] destination, int offset,
					 int size) throws IbisIOException {
	obj_in.readArraySliceShort(destination, offset, size);
    }

    public void readArraySliceInt(int[] destination, int offset,
				       int size) throws IbisIOException {
	obj_in.readArraySliceInt(destination, offset, size);
    }

    public void readArraySliceLong(long[] destination, int offset,
					int size) throws IbisIOException {
	obj_in.readArraySliceLong(destination, offset, size);
    }

    public void readArraySliceFloat(float[] destination, int offset,
					 int size) throws IbisIOException {
	obj_in.readArraySliceFloat(destination, offset, size);
    }

    public void readArraySliceDouble(double[] destination, int offset,
					  int size) throws IbisIOException {
	obj_in.readArraySliceDouble(destination, offset, size);
    }

    public void readArraySliceObject(Object[] destination, int offset,
					  int size) throws IbisIOException {
	obj_in.readArraySliceObject(destination, offset, size);
    }

    public void receive() throws IbisIOException {
	throw new IbisIOException("receive not supported");
    }
}
