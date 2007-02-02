/* $Id: TcpReadMessage.java 4910 2006-12-13 09:01:33Z ceriel $ */

package ibis.impl;

import ibis.io.SerializationInput;

import java.io.IOException;

/**
 * Implementation of the {@link ibis.ipl.ReadMessage} interface.
 * This is a complete implementation, but may be extended by an implementation.
 * In that case, the {@link ReceivePortConnectionInfo#newStream()} method
 * must also be redefined.
 */
public class ReadMessage implements ibis.ipl.ReadMessage {

    private SerializationInput in;

    private boolean isFinished = false;

    private long sequenceNr = -1;

    private long before;

    private ReceivePortConnectionInfo info;

    private ReceivePort port;

    public ReadMessage(SerializationInput in, ReceivePortConnectionInfo info,
            ReceivePort port) {
        this.in = in;
        this.info = info;
        this.port = port;
        this.before = info.bytesRead();
    }

    public ibis.ipl.ReceivePort localPort() {
        return port;
    }

    public long bytesRead() {
	long after = info.bytesRead();
	return after - before;
    }

    public ReceivePortConnectionInfo getInfo() {
        return info;
    }

    public void setInfo(ReceivePortConnectionInfo info) {
        this.info = info;
    }

    protected final void checkNotFinished() throws IOException {
        if (isFinished) {
            throw new IOException(
                    "Operating on a message that was already finished");
        }
    }

    public ibis.ipl.SendPortIdentifier origin() {
        return info.origin;
    }

    protected int available() throws IOException {
        checkNotFinished();
        return in.available();
    }

    public boolean readBoolean() throws IOException {
        checkNotFinished();
        return in.readBoolean();
    }

    public byte readByte() throws IOException {
        checkNotFinished();
        return in.readByte();
    }

    public char readChar() throws IOException {
        checkNotFinished();
        return in.readChar();
    }

    public short readShort() throws IOException {
        checkNotFinished();
        return in.readShort();
    }

    public int readInt() throws IOException {
        checkNotFinished();
        return in.readInt();
    }

    public long readLong() throws IOException {
        checkNotFinished();
        return in.readLong();
    }

    public float readFloat() throws IOException {
        checkNotFinished();
        return in.readFloat();
    }

    public double readDouble() throws IOException {
        checkNotFinished();
        return in.readDouble();
    }

    public String readString() throws IOException {
        checkNotFinished();
        return in.readString();
    }

    public Object readObject() throws IOException, ClassNotFoundException {
        checkNotFinished();
        return in.readObject();
    }

    public void readArray(boolean[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    public void readArray(byte[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    public void readArray(char[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    public void readArray(short[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    public void readArray(int[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    public void readArray(long[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    public void readArray(float[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    public void readArray(double[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    public void readArray(Object[] destination) throws IOException,
            ClassNotFoundException {
        readArray(destination, 0, destination.length);
    }

    public void readArray(boolean[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    public void readArray(byte[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    public void readArray(char[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    public void readArray(short[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    public void readArray(int[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    public void readArray(long[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    public void readArray(float[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    public void readArray(double[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    public void readArray(Object[] destination, int offset, int size)
            throws IOException, ClassNotFoundException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    public void setSequenceNumber(long s) {
        sequenceNr = s;
    }

    public long sequenceNumber() {
        return sequenceNr;
    }

    public long finish() throws IOException {
        checkNotFinished();
        in.clear();
        isFinished = true;
        long after = info.bytesRead();
        long retval = after - before;
        before = after;
        port.addCount(retval);
        port.finishMessage(this);
        return retval;
    }

    public void finish(IOException e) {
        if (isFinished) {
            return;
        }
        port.finishMessage(this, e);
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean val) {
        isFinished = val;
    }
}
