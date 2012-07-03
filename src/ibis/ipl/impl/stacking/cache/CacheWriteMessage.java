package ibis.ipl.impl.stacking.cache;

import ibis.io.DataOutputStream;
import ibis.io.SerializationFactory;
import ibis.io.SerializationOutput;
import ibis.ipl.PortType;
import ibis.ipl.WriteMessage;
import java.io.IOException;
import java.nio.ByteBuffer;

public class CacheWriteMessage implements WriteMessage {

    SerializationOutput out;
    DataOutputStream dos;
    /*
     * SendPort used to generate base WriteMessages.
     */
    final CacheSendPort port;
    /*
     * Number of bytes written by this CacheWriteMessage.
     */
    long bytes;
    /*
     * Boolean for this send port.
     */
    boolean isFinished;

    public CacheWriteMessage(CacheSendPort sendPort) throws IOException {
        this.port = sendPort;
        this.isFinished = false;
        
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
        dos = new BufferedDataOutputStream(this.port);
        out = SerializationFactory.createSerializationOutput(serialization,
                dos);
    }

    private void checkNotFinished() throws IOException {
        if (isFinished) {
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
        try {
            out.reset();
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }
    @Override
    public long bytesWritten() {
        return dos.bytesWritten();
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
        try {
            out.flush();
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    @Override
    public void flush() throws IOException {
        checkNotFinished();
        try {
            out.flush();
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    @Override
    public long finish() throws IOException {
        checkNotFinished();
        try {
            out.reset();
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
        try {
            out.flush();
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
        isFinished = true;
        long retval = bytesWritten();
        port.finishMessage(this, retval);
        return retval;
    }

    @Override
    public void finish(IOException e) {
        if (isFinished) {
            return;
        }

        try {
            out.reset();
        } catch (Throwable e2) {
            // ignored
        }

        try {
            out.flush();
        } catch (Throwable e2) {
            // ignored
        }

        isFinished = true;
        port.finishMessage(this, e);
    }

    @Override
    public void writeBoolean(boolean value) throws IOException {
        checkNotFinished();
        try {
            out.writeBoolean(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    @Override
    public void writeByte(byte value) throws IOException {
        checkNotFinished();
        try {
            out.writeByte(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    @Override
    public void writeChar(char value) throws IOException {
        checkNotFinished();
        try {
            out.writeChar(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    @Override
    public void writeShort(short value) throws IOException {
        checkNotFinished();
        try {
            out.writeShort(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    @Override
    public void writeInt(int value) throws IOException {
        checkNotFinished();
        try {
            out.writeInt(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    @Override
    public void writeLong(long value) throws IOException {
        checkNotFinished();
        try {
            out.writeLong(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    @Override
    public void writeFloat(float value) throws IOException {
        checkNotFinished();
        try {
            out.writeFloat(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    @Override
    public void writeDouble(double value) throws IOException {
        checkNotFinished();
        try {
            out.writeDouble(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    @Override
    public void writeString(String value) throws IOException {
        checkNotFinished();
        try {
            out.writeString(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    @Override
    public void writeObject(Object value) throws IOException {
        checkNotFinished();
        try {
            out.writeObject(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    @Override
    public void writeArray(boolean[] value) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    @Override
    public void writeArray(byte[] value) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    @Override
    public void writeArray(char[] value) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    @Override
    public void writeArray(short[] value) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    @Override
    public void writeArray(int[] value) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    @Override
    public void writeArray(long[] value) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    @Override
    public void writeArray(float[] value) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    @Override
    public void writeArray(double[] value) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    @Override
    public void writeArray(Object[] value) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    @Override
    public void writeArray(boolean[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value, offset, size);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    @Override
    public void writeArray(byte[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value, offset, size);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    @Override
    public void writeArray(char[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value, offset, size);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    @Override
    public void writeArray(short[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value, offset, size);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    @Override
    public void writeArray(int[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value, offset, size);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    @Override
    public void writeArray(long[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value, offset, size);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    @Override
    public void writeArray(float[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value, offset, size);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    @Override
    public void writeArray(double[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value, offset, size);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    @Override
    public void writeArray(Object[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value, offset, size);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    @Override
    public void writeByteBuffer(ByteBuffer value) throws IOException {
        checkNotFinished();
        try {
            out.writeByteBuffer(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }
}
