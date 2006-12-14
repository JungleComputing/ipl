/* $Id$ */

package ibis.impl.messagePassing;

import java.io.IOException;

/**
 * messagePassing implementation of ReadMessage that uses Ibis serialization
 */
final public class IbisReadMessage extends ReadMessage {

    private ibis.io.DataSerializationInputStream obj_in;

    IbisReadMessage(ibis.ipl.SendPort origin, ReceivePort port) {
        super(origin, port);
        obj_in = ((IbisShadowSendPort) origin).obj_in;
    }

    public long finish() throws IOException {
        obj_in.clear();
        long cnt = super.finish();
        return cnt;
    }

    public boolean readBoolean() throws IOException {
        return obj_in.readBoolean();
    }

    public byte readByte() throws IOException {
        return obj_in.readByte();
    }

    public char readChar() throws IOException {
        return obj_in.readChar();
    }

    public short readShort() throws IOException {
        return obj_in.readShort();
    }

    public int readInt() throws IOException {
        return obj_in.readInt();
    }

    public long readLong() throws IOException {
        return obj_in.readLong();
    }

    public float readFloat() throws IOException {
        return obj_in.readFloat();
    }

    public double readDouble() throws IOException {
        return obj_in.readDouble();
    }

    public Object readObject() throws IOException, ClassNotFoundException {
        return obj_in.readObject();
    }

    public String readString() throws IOException {
        return obj_in.readString();
    }

    public void readArray(boolean[] destination) throws IOException {
        obj_in.readArray(destination);
    }

    public void readArray(byte[] destination) throws IOException {
        obj_in.readArray(destination);
    }

    public void readArray(char[] destination) throws IOException {
        obj_in.readArray(destination);
    }

    public void readArray(short[] destination) throws IOException {
        obj_in.readArray(destination);
    }

    public void readArray(int[] destination) throws IOException {
        obj_in.readArray(destination);
    }

    public void readArray(long[] destination) throws IOException {
        obj_in.readArray(destination);
    }

    public void readArray(float[] destination) throws IOException {
        obj_in.readArray(destination);
    }

    public void readArray(double[] destination) throws IOException {
        obj_in.readArray(destination);
    }

    public void readArray(Object[] destination) throws IOException,
            ClassNotFoundException {
        obj_in.readArray(destination);
    }

    public void readArray(boolean[] destination, int offset, int size)
            throws IOException {
        obj_in.readArray(destination, offset, size);
    }

    public void readArray(byte[] destination, int offset, int size)
            throws IOException {
        obj_in.readArray(destination, offset, size);
    }

    public void readArray(char[] destination, int offset, int size)
            throws IOException {
        obj_in.readArray(destination, offset, size);
    }

    public void readArray(short[] destination, int offset, int size)
            throws IOException {
        obj_in.readArray(destination, offset, size);
    }

    public void readArray(int[] destination, int offset, int size)
            throws IOException {
        obj_in.readArray(destination, offset, size);
    }

    public void readArray(long[] destination, int offset, int size)
            throws IOException {
        obj_in.readArray(destination, offset, size);
    }

    public void readArray(float[] destination, int offset, int size)
            throws IOException {
        obj_in.readArray(destination, offset, size);
    }

    public void readArray(double[] destination, int offset, int size)
            throws IOException {
        obj_in.readArray(destination, offset, size);
    }

    public void readArray(Object[] destination, int offset, int size)
            throws IOException, ClassNotFoundException {
        obj_in.readArray(destination, offset, size);
    }

    public void receive() throws IOException {
        throw new IOException("receive not supported");
    }
}
