package ibis.impl.nio;

import ibis.io.SerializationOutputStream;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

import java.io.IOException;

final class NioWriteMessage implements WriteMessage, Config {
    private NioSendPort port;

    private SerializationOutputStream out;

    NioWriteMessage(NioSendPort port, SerializationOutputStream out)
            throws IOException {
        this.port = port;
        this.out = out;
    }

    /**
     * Sends all pending output to the network
     */
    public int send() throws IOException {
        out.flush();
        return 0; // fake ticket
    }

    /**
     * Syncs data up to point of ticket. Actually just sends out everything
     */
    public void sync(int ticket) throws IOException {
        out.flush();
    }

    public void reset() throws IOException {
        out.reset();
    }

    public long finish() throws IOException {
        out.flush();
        return port.finish();
    }

    public void finish(IOException e) {
        port.finish(e);
    }

    public SendPort localPort() {
        return port;
    }

    public void writeBoolean(boolean value) throws IOException {
        out.writeBoolean(value);
    }

    public void writeByte(byte value) throws IOException {
        out.writeByte(value);
    }

    public void writeChar(char value) throws IOException {
        out.writeChar(value);
    }

    public void writeShort(short value) throws IOException {
        out.writeShort(value);
    }

    public void writeInt(int value) throws IOException {
        out.writeInt(value);
    }

    public void writeLong(long value) throws IOException {
        out.writeLong(value);
    }

    public void writeFloat(float value) throws IOException {
        out.writeFloat(value);
    }

    public void writeDouble(double value) throws IOException {
        out.writeDouble(value);
    }

    public void writeString(String value) throws IOException {
        out.writeUTF(value);
    }

    public void writeObject(Object value) throws IOException {
        out.writeObject(value);
    }

    public void writeArray(boolean[] value) throws IOException {
        out.writeArray(value);
    }

    public void writeArray(byte[] value) throws IOException {
        out.writeArray(value);
    }

    public void writeArray(char[] value) throws IOException {
        out.writeArray(value);
    }

    public void writeArray(short[] value) throws IOException {
        out.writeArray(value);
    }

    public void writeArray(int[] value) throws IOException {
        out.writeArray(value);
    }

    public void writeArray(long[] value) throws IOException {
        out.writeArray(value);
    }

    public void writeArray(float[] value) throws IOException {
        out.writeArray(value);
    }

    public void writeArray(double[] value) throws IOException {
        out.writeArray(value);
    }

    public void writeArray(Object[] value) throws IOException {
        out.writeArray(value);
    }

    public void writeArray(boolean[] value, int offset, int size)
            throws IOException {
        out.writeArray(value, offset, size);
    }

    public void writeArray(byte[] value, int offset, int size)
            throws IOException {
        out.writeArray(value, offset, size);
    }

    public void writeArray(char[] value, int offset, int size)
            throws IOException {
        out.writeArray(value, offset, size);
    }

    public void writeArray(short[] value, int offset, int size)
            throws IOException {
        out.writeArray(value, offset, size);
    }

    public void writeArray(int[] value, int offset, int size)
            throws IOException {
        out.writeArray(value, offset, size);
    }

    public void writeArray(long[] value, int offset, int size)
            throws IOException {
        out.writeArray(value, offset, size);
    }

    public void writeArray(float[] value, int offset, int size)
            throws IOException {
        out.writeArray(value, offset, size);
    }

    public void writeArray(double[] value, int offset, int size)
            throws IOException {
        out.writeArray(value, offset, size);
    }

    public void writeArray(Object[] value, int offset, int size)
            throws IOException {
        out.writeArray(value, offset, size);
    }
}