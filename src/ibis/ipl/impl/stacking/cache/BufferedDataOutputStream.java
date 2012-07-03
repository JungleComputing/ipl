package ibis.ipl.impl.stacking.cache;

import ibis.io.Conversion;
import ibis.io.DataOutputStream;
import ibis.io.IOProperties;
import ibis.ipl.SendPort;
import java.io.IOException;
import java.nio.ByteBuffer;

public class BufferedDataOutputStream extends DataOutputStream {

    /*
     * Used to convert every primitive type to bytes.
     */
    final Conversion c;
    /*
     * Buffer used to store read data.
     */
    private byte[] buffer;
    /*
     * The current index of the buffer.
     */
    private int index;
    /*
     * The buffer's capacity.
     */
    private final int capacity;
    /*
     * No of bytes written, not counting the ones currently in the buffer.
     */
    int bytes;
    /*
     * The send port which generates for me newMessages. I need them so I can
     * ship my buffer to the various receive ports.
     */
    private final SendPort sp;

    public BufferedDataOutputStream(SendPort sp) {
        this.sp = sp;
        c = Conversion.loadConversion(false);
        this.index = 0;
        this.capacity = IOProperties.BUFFER_SIZE;
        this.buffer = new byte[this.capacity];

    }

    @Override
    public long bytesWritten() {
        return bytes + index;
    }

    @Override
    public int bufferSize() {
        return capacity;
    }

    private void stream(boolean lastPart) {
//        while(!sentToAllRecvPorts) {
//            getSomeConnections(sendPort);
//            WriteMessage msg = sendPort.newMessage();
//            msg.writeBoolean(lastPart);
//            msg.writeInt(index);
//            msg.writeArray(buffer);
//            msg.finish();
//        }
        index = 0;
    }

    @Override
    public void flush() {
        stream(false);
    }

    @Override
    public void close() throws IOException {
        stream(true);
    }

    /**
     * Checks if there is space for
     * <code>incr</code> more bytes and if not, the buffer is delivered.
     *
     * @param incr the space requested
     */
    private void checkAndStream(int incr) {
        if (index + incr > capacity) {
            bytes += index;

            stream(false);
        }
    }

    @Override
    public void write(int b) throws IOException {
        writeByte((byte) b);
    }

    @Override
    public void writeArray(boolean[] val, int off, int len) {
        do {
            int incr = Conversion.BOOLEAN_SIZE;
            checkAndStream(incr);

            int size = Math.min((capacity - index) / incr, len);

            c.boolean2byte(val, off, size, buffer, index);

            off += size;
            len -= size;
            index += size * incr;
        } while (len > 0);
    }

    @Override
    public void writeArray(byte[] val, int off, int len) {
        do {
            int incr = Conversion.BYTE_SIZE;
            checkAndStream(incr);

            int size = Math.min((capacity - index) / incr, len);

            System.arraycopy(val, off, buffer, index, size);

            off += size;
            len -= size;
            index += size * incr;
        } while (len > 0);
    }

    @Override
    public void writeArray(char[] val, int off, int len) {
        do {
            int incr = Conversion.CHAR_SIZE;
            checkAndStream(incr);

            int size = Math.min((capacity - index) / incr, len);

            c.char2byte(val, off, size, buffer, index);

            off += size;
            len -= size;
            index += size * incr;
        } while (len > 0);
    }

    @Override
    public void writeArray(double[] val, int off, int len) {
        do {
            int incr = Conversion.DOUBLE_SIZE;
            checkAndStream(incr);

            int size = Math.min((capacity - index) / incr, len);

            c.double2byte(val, off, size, buffer, index);

            off += size;
            len -= size;
            index += size * incr;
        } while (len > 0);
    }

    @Override
    public void writeArray(float[] val, int off, int len) {
        do {
            int incr = Conversion.FLOAT_SIZE;
            checkAndStream(incr);

            int size = Math.min((capacity - index) / incr, len);

            c.float2byte(val, off, size, buffer, index);

            off += size;
            len -= size;
            index += size * incr;
        } while (len > 0);
    }

    @Override
    public void writeArray(int[] val, int off, int len) {
        do {
            int incr = Conversion.INT_SIZE;
            checkAndStream(incr);

            int size = Math.min((capacity - index) / incr, len);

            c.int2byte(val, off, size, buffer, index);

            off += size;
            len -= size;
            index += size * incr;
        } while (len > 0);
    }

    @Override
    public void writeArray(long[] val, int off, int len) {
        do {
            int incr = Conversion.LONG_SIZE;
            checkAndStream(incr);

            int size = Math.min((capacity - index) / incr, len);

            c.long2byte(val, off, size, buffer, index);

            off += size;
            len -= size;
            index += size * incr;
        } while (len > 0);
    }

    @Override
    public void writeArray(short[] val, int off, int len) {
        do {
            int incr = Conversion.SHORT_SIZE;
            checkAndStream(incr);

            int size = Math.min((capacity - index) / incr, len);

            c.short2byte(val, off, size, buffer, index);

            off += size;
            len -= size;
            index += size * incr;
        } while (len > 0);
    }

    @Override
    public void writeBoolean(boolean val) {
        byte b = c.boolean2byte(val);
        checkAndStream(1);
        buffer[index++] = b;
    }

    @Override
    public void writeByte(byte val) {
        checkAndStream(1);
        buffer[index++] = val;
    }

    @Override
    public void writeChar(char val) {
        int incr = Conversion.CHAR_SIZE;
        checkAndStream(incr);
        c.char2byte(val, buffer, index);
        index += incr;
    }

    @Override
    public void writeDouble(double val) {
        int incr = Conversion.DOUBLE_SIZE;
        checkAndStream(incr);
        c.double2byte(val, buffer, index);
        index += incr;
    }

    @Override
    public void writeFloat(float val) {
        int incr = Conversion.FLOAT_SIZE;
        checkAndStream(incr);
        c.float2byte(val, buffer, index);
        index += incr;
    }

    @Override
    public void writeInt(int val) {
        int incr = Conversion.INT_SIZE;
        checkAndStream(incr);
        c.int2byte(val, buffer, index);
        index += incr;
    }

    @Override
    public void writeLong(long val) {
        int incr = Conversion.LONG_SIZE;
        checkAndStream(incr);
        c.long2byte(val, buffer, index);
        index += incr;
    }

    @Override
    public void writeShort(short val) {
        int incr = Conversion.SHORT_SIZE;
        checkAndStream(incr);
        c.short2byte(val, buffer, index);
        index += incr;
    }

    @Override
    public void writeByteBuffer(ByteBuffer val) {

        int len = val.limit() - val.position();
        int size = capacity - index;

        while (len >= size) {
            bytes += size;
            val.get(buffer, index, size);
            stream(false);
            len -= size;
            size = capacity;
            index = 0;
        }
        if (len > 0) {
            val.get(buffer, index, len);
            index += len;
        }
    }

    @Override
    public void resetBytesWritten() {
        bytes = 0;
    }
}
