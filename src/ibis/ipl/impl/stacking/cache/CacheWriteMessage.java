package ibis.ipl.impl.stacking.cache;

import ibis.ipl.impl.stacking.cache.io.BufferedDataOutputStream;
import ibis.ipl.impl.stacking.cache.manager.CacheManager;
import ibis.io.DataOutputStream;
import ibis.io.SerializationFactory;
import ibis.io.SerializationOutput;
import ibis.ipl.PortType;
import ibis.ipl.WriteMessage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;

public final class CacheWriteMessage implements WriteMessage {

    SerializationOutput serOut;
    public BufferedDataOutputStream dataOut;
    /*
     * SendPort used to generate base WriteMessages.
     */
    final CacheSendPort port;
    /*
     * Number of bytes written by this CacheWriteMessage.
     */
    long bytes;

    public CacheWriteMessage(CacheSendPort sendPort) throws IOException {
        CacheManager.log.log(Level.INFO,"Created CacheWriteMessage...");
        this.port = sendPort;

        PortType type = this.port.getPortType();
        String serialization;
        if (type.hasCapability(PortType.SERIALIZATION_DATA)) {
            serialization = "data";
        } else if (type.hasCapability(PortType.SERIALIZATION_OBJECT_SUN)) {
            serialization = "sun";
        } else if (type.hasCapability(PortType.SERIALIZATION_OBJECT_IBIS)) {
            serialization = "ibis";
        } else if (type.hasCapability(PortType.SERIALIZATION_OBJECT)) {
            serialization = "object";
        } else {
            serialization = "byte";
        }
        dataOut = new BufferedDataOutputStream(this.port);
        serOut = SerializationFactory.createSerializationOutput(serialization,
                dataOut);
    }

    private void checkNotFinished() throws IOException {
        if (this.port.currentMsg == null) {
            throw new IOException(
                    "Operating on a message that was already finished");
        }
    }

    @Override
    public ibis.ipl.SendPort localPort() {
        return port;
    }

    @Override
    public int send() throws IOException {
        checkNotFinished();
        return 0;
    }

    @Override
    public void reset() throws IOException {
        serOut.reset();
    }

    @Override
    public long bytesWritten() {
        return dataOut.bytesWritten();
    }

    @Override
    public int capacity() throws IOException {
        return -1;
    }

    @Override
    public int remaining() throws IOException {
        return -1;
    }

    @Override
    public void sync(int ticket) throws IOException {
        checkNotFinished();
        serOut.flush();
    }

    @Override
    public void flush() throws IOException {
        checkNotFinished();
        serOut.flush();
    }

    @Override
    public long finish() throws IOException {
        checkNotFinished();        
        
        serOut.flush();
        dataOut.close();
        
//        serOut.close();
        /*
         * serOut.close() actually just flushes dos. I need dataOut.close() so
         * it can do: stream(true).
         */
//        serOut.close();

        port.currentMsg = null;
        return bytesWritten();
    }

    @Override
    public void finish(IOException e) {
        if (port.currentMsg == null) {
            return;
        }
        try {
            serOut.flush();
        } catch (Throwable e2) {
            // ignored
        }

        try {
            dataOut.close();
        } catch (IOException ignoreMe) {
        }
        
        try {
            serOut.close();
        } catch (Throwable e2) {
            // ignored
        }

        serOut = null;
        dataOut = null;
        port.currentMsg = null;
    }

    @Override
    public void writeBoolean(boolean value) throws IOException {
        checkNotFinished();
        serOut.writeBoolean(value);
    }

    @Override
    public void writeByte(byte value) throws IOException {
        checkNotFinished();
        serOut.writeByte(value);
    }

    @Override
    public void writeChar(char value) throws IOException {
        checkNotFinished();
        serOut.writeChar(value);
    }

    @Override
    public void writeShort(short value) throws IOException {
        checkNotFinished();
        serOut.writeShort(value);
    }

    @Override
    public void writeInt(int value) throws IOException {
        checkNotFinished();
        serOut.writeInt(value);
    }

    @Override
    public void writeLong(long value) throws IOException {
        checkNotFinished();
        serOut.writeLong(value);
    }

    @Override
    public void writeFloat(float value) throws IOException {
        checkNotFinished();
        serOut.writeFloat(value);
    }

    @Override
    public void writeDouble(double value) throws IOException {
        checkNotFinished();
        serOut.writeDouble(value);
    }

    @Override
    public void writeString(String value) throws IOException {
        checkNotFinished();
        serOut.writeString(value);
    }

    @Override
    public void writeObject(Object value) throws IOException {
        checkNotFinished();
        serOut.writeObject(value);
    }

    @Override
    public void writeArray(boolean[] value) throws IOException {
        checkNotFinished();
        serOut.writeArray(value);
    }

    @Override
    public void writeArray(byte[] value) throws IOException {
        checkNotFinished();
        serOut.writeArray(value);
    }

    @Override
    public void writeArray(char[] value) throws IOException {
        checkNotFinished();
        serOut.writeArray(value);
    }

    @Override
    public void writeArray(short[] value) throws IOException {
        checkNotFinished();
        serOut.writeArray(value);
    }

    @Override
    public void writeArray(int[] value) throws IOException {
        checkNotFinished();
        serOut.writeArray(value);
    }

    @Override
    public void writeArray(long[] value) throws IOException {
        checkNotFinished();
        serOut.writeArray(value);
    }

    @Override
    public void writeArray(float[] value) throws IOException {
        checkNotFinished();
        serOut.writeArray(value);
    }

    @Override
    public void writeArray(double[] value) throws IOException {
        checkNotFinished();
        serOut.writeArray(value);
    }

    @Override
    public void writeArray(Object[] value) throws IOException {
        checkNotFinished();
        serOut.writeArray(value);
    }

    @Override
    public void writeArray(boolean[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        serOut.writeArray(value, offset, size);
    }

    @Override
    public void writeArray(byte[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        serOut.writeArray(value, offset, size);
    }

    @Override
    public void writeArray(char[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        serOut.writeArray(value, offset, size);
    }

    @Override
    public void writeArray(short[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        serOut.writeArray(value, offset, size);
    }

    @Override
    public void writeArray(int[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        serOut.writeArray(value, offset, size);
    }

    @Override
    public void writeArray(long[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        serOut.writeArray(value, offset, size);
    }

    @Override
    public void writeArray(float[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        serOut.writeArray(value, offset, size);
    }

    @Override
    public void writeArray(double[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        serOut.writeArray(value, offset, size);
    }

    @Override
    public void writeArray(Object[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        serOut.writeArray(value, offset, size);
    }

    @Override
    public void writeByteBuffer(ByteBuffer value) throws IOException {
        checkNotFinished();
        serOut.writeByteBuffer(value);
    }
}
