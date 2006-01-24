/* $Id$ */

package ibis.impl.nio;

import ibis.io.SerializationInput;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPortIdentifier;

import java.io.IOException;

final class NioReadMessage implements ReadMessage, Config {
    SerializationInput in;

    NioDissipator dissipator;

    NioReceivePort port;

    boolean isFinished = false;

    private long sequencenr;

    NioReadMessage(NioReceivePort port, NioDissipator dissipator,
            long sequencenr) throws IOException {
        this.port = port;
        this.dissipator = dissipator;
        this.sequencenr = sequencenr;

        if (dissipator != null) {
            in = dissipator.sis;
        }

    }

    public ReceivePort localPort() {
        return port;
    }

    public long finish() throws IOException {
        long messageCount;

        if (isFinished) {
            throw new IOException("finish called twice on a message!");
        }

        in.clear();

        messageCount = dissipator.bytesRead();
        dissipator.resetBytesRead();

        port.finish(this, messageCount);

        isFinished = true;

        return messageCount;
    }

    public void finish(IOException e) {
        port.finish(this, e);
        isFinished = true;
    }

    public long bytesRead() throws IOException {
	return dissipator.bytesRead();
    }

    public SendPortIdentifier origin() {
        return dissipator.peer;
    }

    public long sequenceNumber() {
        return sequencenr;
    }

    public boolean readBoolean() throws IOException {
        return in.readBoolean();
    }

    public byte readByte() throws IOException {
        return in.readByte();
    }

    public char readChar() throws IOException {
        return in.readChar();
    }

    public short readShort() throws IOException {
        return in.readShort();
    }

    public int readInt() throws IOException {
        return in.readInt();
    }

    public long readLong() throws IOException {
        return in.readLong();
    }

    public float readFloat() throws IOException {
        return in.readFloat();
    }

    public double readDouble() throws IOException {
        return in.readDouble();
    }

    public String readString() throws IOException {
        return in.readString();
    }

    public Object readObject() throws IOException, ClassNotFoundException {
        return in.readObject();
    }

    public void readArray(boolean[] destination) throws IOException {
        in.readArray(destination, 0, destination.length);
    }

    public void readArray(byte[] destination) throws IOException {
        in.readArray(destination, 0, destination.length);
    }

    public void readArray(char[] destination) throws IOException {
        in.readArray(destination, 0, destination.length);
    }

    public void readArray(short[] destination) throws IOException {
        in.readArray(destination, 0, destination.length);
    }

    public void readArray(int[] destination) throws IOException {
        in.readArray(destination, 0, destination.length);
    }

    public void readArray(long[] destination) throws IOException {
        in.readArray(destination, 0, destination.length);
    }

    public void readArray(float[] destination) throws IOException {
        in.readArray(destination, 0, destination.length);
    }

    public void readArray(double[] destination) throws IOException {
        in.readArray(destination, 0, destination.length);
    }

    public void readArray(Object[] destination) throws IOException,
            ClassNotFoundException {
        in.readArray(destination, 0, destination.length);
    }

    public void readArray(boolean[] destination, int offset, int size)
            throws IOException {
        in.readArray(destination, offset, size);
    }

    public void readArray(byte[] destination, int offset, int size)
            throws IOException {
        in.readArray(destination, offset, size);
    }

    public void readArray(char[] destination, int offset, int size)
            throws IOException {
        in.readArray(destination, offset, size);
    }

    public void readArray(short[] destination, int offset, int size)
            throws IOException {
        in.readArray(destination, offset, size);
    }

    public void readArray(int[] destination, int offset, int size)
            throws IOException {
        in.readArray(destination, offset, size);
    }

    public void readArray(long[] destination, int offset, int size)
            throws IOException {
        in.readArray(destination, offset, size);
    }

    public void readArray(float[] destination, int offset, int size)
            throws IOException {
        in.readArray(destination, offset, size);
    }

    public void readArray(double[] destination, int offset, int size)
            throws IOException {
        in.readArray(destination, offset, size);
    }

    public void readArray(Object[] destination, int offset, int size)
            throws IOException, ClassNotFoundException {
        in.readArray(destination, offset, size);
    }
}
