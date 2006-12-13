/* $Id$ */

package ibis.impl.tcp;

import ibis.io.SerializationInput;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPortIdentifier;

import java.io.IOException;

final class TcpReadMessage implements ReadMessage, Config {
    private SerializationInput in;

    private long sequenceNr = -1;

    private TcpReceivePort port;

    private TcpSendPortIdentifier origin;

    private ConnectionHandler handler;

    boolean isFinished = false;

    long before;

    TcpReadMessage(TcpReceivePort port, SerializationInput in,
            TcpSendPortIdentifier origin, ConnectionHandler handler) {
        this.port = port;
        this.in = in;
        this.origin = origin;
        this.handler = handler;
        before = handler.bufferedInput.bytesRead();
    }

    TcpReadMessage(TcpReadMessage o) {
        this.port = o.port;
        this.in = o.in;
        this.origin = o.origin;
        this.handler = o.handler;
        this.isFinished = false;
        this.sequenceNr = o.sequenceNr;
        before = handler.bufferedInput.bytesRead();
    }

    ConnectionHandler getHandler() {
        return handler;
    }

    public ReceivePort localPort() {
        return port;
    }

    protected int available() throws IOException {
        if (isFinished) {
            throw new IOException(
                    "Reading data from a message that was already finished");
        }
        return in.available();
    }

    public long bytesRead() throws IOException {
	long after = handler.bufferedInput.bytesRead();
	return after - before;
    }

    public long finish() throws IOException {
        long retval = 0;

        if (isFinished) {
            throw new IOException(
                    "Finishing a message that was already finished");
        }

        if (STATS) {
            long after = handler.bufferedInput.bytesRead();
            retval = after - before;
            before = after;
            port.count += retval;
        }

        in.clear();     // Before finishMessage, or else there is a race here.
                        // (Ceriel)

        port.finishMessage();

        return retval;
    }

    public void finish(IOException e) {
        if (isFinished) {
            return;
        }
        port.finishMessage(e);
    }

    public SendPortIdentifier origin() {
        return origin;
    }

    void setSequenceNumber(long s) {
        sequenceNr = s;
    }

    public long sequenceNumber() {
        return sequenceNr;
    }

    public boolean readBoolean() throws IOException {
        if (isFinished) {
            throw new IOException(
                    "Reading data from a message that was already finished");
        }
        return in.readBoolean();
    }

    public byte readByte() throws IOException {
        if (isFinished) {
            throw new IOException(
                    "Reading data from a message that was already finished");
        }
        return in.readByte();
    }

    public char readChar() throws IOException {
        if (isFinished) {
            throw new IOException(
                    "Reading data from a message that was already finished");
        }
        return in.readChar();
    }

    public short readShort() throws IOException {
        if (isFinished) {
            throw new IOException(
                    "Reading data from a message that was already finished");
        }
        return in.readShort();
    }

    public int readInt() throws IOException {
        if (isFinished) {
            throw new IOException(
                    "Reading data from a message that was already finished");
        }
        return in.readInt();
    }

    public long readLong() throws IOException {
        if (isFinished) {
            throw new IOException(
                    "Reading data from a message that was already finished");
        }
        return in.readLong();
    }

    public float readFloat() throws IOException {
        if (isFinished) {
            throw new IOException(
                    "Reading data from a message that was already finished");
        }
        return in.readFloat();
    }

    public double readDouble() throws IOException {
        if (isFinished) {
            throw new IOException(
                    "Reading data from a message that was already finished");
        }
        return in.readDouble();
    }

    public String readString() throws IOException {
        if (isFinished) {
            throw new IOException(
                    "Reading data from a message that was already finished");
        }
        return in.readString();
    }

    public Object readObject() throws IOException, ClassNotFoundException {
        if (isFinished) {
            throw new IOException(
                    "Reading data from a message that was already finished");
        }
        return in.readObject();
    }

    public void readArray(boolean[] destination) throws IOException {
        if (isFinished) {
            throw new IOException(
                    "Reading data from a message that was already finished");
        }
        in.readArray(destination, 0, destination.length);
    }

    public void readArray(byte[] destination) throws IOException {
        if (isFinished) {
            throw new IOException(
                    "Reading data from a message that was already finished");
        }
        in.readArray(destination, 0, destination.length);
    }

    public void readArray(char[] destination) throws IOException {
        if (isFinished) {
            throw new IOException(
                    "Reading data from a message that was already finished");
        }
        in.readArray(destination, 0, destination.length);
    }

    public void readArray(short[] destination) throws IOException {
        if (isFinished) {
            throw new IOException(
                    "Reading data from a message that was already finished");
        }
        in.readArray(destination, 0, destination.length);
    }

    public void readArray(int[] destination) throws IOException {
        if (isFinished) {
            throw new IOException(
                    "Reading data from a message that was already finished");
        }
        in.readArray(destination, 0, destination.length);
    }

    public void readArray(long[] destination) throws IOException {
        if (isFinished) {
            throw new IOException(
                    "Reading data from a message that was already finished");
        }
        in.readArray(destination, 0, destination.length);
    }

    public void readArray(float[] destination) throws IOException {
        if (isFinished) {
            throw new IOException(
                    "Reading data from a message that was already finished");
        }
        in.readArray(destination, 0, destination.length);
    }

    public void readArray(double[] destination) throws IOException {
        if (isFinished) {
            throw new IOException(
                    "Reading data from a message that was already finished");
        }
        in.readArray(destination, 0, destination.length);
    }

    public void readArray(Object[] destination) throws IOException,
            ClassNotFoundException {
        if (isFinished) {
            throw new IOException(
                    "Reading data from a message that was already finished");
        }
        in.readArray(destination, 0, destination.length);
    }

    public void readArray(boolean[] destination, int offset, int size)
            throws IOException {
        if (isFinished) {
            throw new IOException(
                    "Reading data from a message that was already finished");
        }
        in.readArray(destination, offset, size);
    }

    public void readArray(byte[] destination, int offset, int size)
            throws IOException {
        if (isFinished) {
            throw new IOException(
                    "Reading data from a message that was already finished");
        }
        in.readArray(destination, offset, size);
    }

    public void readArray(char[] destination, int offset, int size)
            throws IOException {
        if (isFinished) {
            throw new IOException(
                    "Reading data from a message that was already finished");
        }
        in.readArray(destination, offset, size);
    }

    public void readArray(short[] destination, int offset, int size)
            throws IOException {
        if (isFinished) {
            throw new IOException(
                    "Reading data from a message that was already finished");
        }
        in.readArray(destination, offset, size);
    }

    public void readArray(int[] destination, int offset, int size)
            throws IOException {
        if (isFinished) {
            throw new IOException(
                    "Reading data from a message that was already finished");
        }
        in.readArray(destination, offset, size);
    }

    public void readArray(long[] destination, int offset, int size)
            throws IOException {
        if (isFinished) {
            throw new IOException(
                    "Reading data from a message that was already finished");
        }
        in.readArray(destination, offset, size);
    }

    public void readArray(float[] destination, int offset, int size)
            throws IOException {
        if (isFinished) {
            throw new IOException(
                    "Reading data from a message that was already finished");
        }
        in.readArray(destination, offset, size);
    }

    public void readArray(double[] destination, int offset, int size)
            throws IOException {
        if (isFinished) {
            throw new IOException(
                    "Reading data from a message that was already finished");
        }
        in.readArray(destination, offset, size);
    }

    public void readArray(Object[] destination, int offset, int size)
            throws IOException, ClassNotFoundException {
        if (isFinished) {
            throw new IOException(
                    "Reading data from a message that was already finished");
        }
        in.readArray(destination, offset, size);
    }
}
