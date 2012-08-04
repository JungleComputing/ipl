package ibis.ipl.impl.stacking.cache.io;

import ibis.io.Conversion;
import ibis.io.DataInputStream;
import ibis.ipl.ReadMessage;
import ibis.ipl.impl.stacking.cache.CacheReceivePort;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;

public abstract class BufferedDataInputStream extends DataInputStream{

    /*
     * The port which gives me new Messages.
     */
    public CacheReceivePort port;
    /*
     * The current partial message.
     */
    protected ReadMessage currentMsg;
    /*
     * The number of bytes which can still be read from the currentMsg's sent buffer.
     */
    protected int remainingBytes;
    /*
     * The buffer.
     */
    protected byte[] buffer;
    /*
     * The buffer maximum size.
     * I do not send more than `capacity` bytes. (except for the protocol)
     */
    protected int capacity;
    /*
     * Index of the buffer.
     */
    protected int index;
    /*
     * Number of available bytes from the index onward.
     */
    protected int buffered_bytes;
    /*
     * Number of bytes read so far from the underlying layer.
     */
    protected long bytes = 0;
    /*
     * Object used to convert primitive types to bytes.
     */
    protected Conversion conversion;
    public boolean closed;
    
    protected BufferedDataInputStream(CacheReceivePort port) {
        this.port = port;
        this.capacity = BufferedDataOutputStream.BUFFER_CAPACITY;
        this.buffer = new byte[this.capacity];
        this.conversion = Conversion.loadConversion(false);
        this.closed = false;
    }
    
    @Override
    public long bytesRead() {
        return bytes - buffered_bytes;
    }

    @Override
    public void resetBytesRead() {
        bytes = buffered_bytes;
    }

    @Override
    public final int read() throws IOException {
        try {
            int b = readByte();
            return (b & 255);
        } catch(EOFException e) {
            return -1;
        }
    }
    
    /*
     * Manually offer a message with a buffer inside to feed it
     * to the DataInputStream.
     * For upcalls.
     */
    abstract public void offerToBuffer(boolean isLastPart, ReadMessage msg);

    /*
     * Block until it is guaranteed that there are at least n bytes 
     * available for reading.
     */
    abstract protected void requestFromBuffer(int n) throws IOException;

    @Override
    public final int available() throws IOException {
        return buffered_bytes;
    }

    @Override
    public void readArray(boolean[] a, int off, int len) throws IOException {
        int sizeof = Conversion.BOOLEAN_SIZE;
        int useable, converted;
        int to_convert = len * sizeof;

        while (buffered_bytes < to_convert) {
            // not enough data in the buffer

            if (buffered_bytes == 0) {
                index = 0;
                requestFromBuffer(Math.min(capacity, to_convert));
            } else {
                // first, copy the data we do have to 'a' .
                useable = buffered_bytes / sizeof;
                conversion.byte2boolean(buffer, index, a, off, useable);

                len -= useable;
                off += useable;

                converted = useable * sizeof;
                index += converted;
                buffered_bytes -= converted;
                to_convert -= converted;

                // second, copy the leftovers to the start of the buffer.
                System.arraycopy(buffer, index, buffer, 0, buffered_bytes);
                index = 0;

                // third, fill the buffer as far as possible.
                requestFromBuffer(Math.min(capacity, to_convert));
            }
        }

        // enough data in the buffer
        conversion.byte2boolean(buffer, index, a, off, len);
        buffered_bytes -= to_convert;
        index += to_convert;
    }

    @Override
    public void readArray(byte[] a, int off, int len) throws IOException {
        int sizeof = Conversion.BYTE_SIZE;
        int useable, converted;
        int to_convert = len * sizeof;

        while (buffered_bytes < to_convert) {
            // not enough data in the buffer

            if (buffered_bytes == 0) {
                index = 0;
                requestFromBuffer(Math.min(capacity, to_convert));
            } else {
                // first, copy the data we do have to 'a' .
                useable = buffered_bytes / sizeof;
                
                System.arraycopy(buffer, index, a, off, useable);

                len -= useable;
                off += useable;

                converted = useable * sizeof;
                index += converted;
                buffered_bytes -= converted;
                to_convert -= converted;

                // second, copy the leftovers to the start of the buffer.
                System.arraycopy(buffer, index, buffer, 0, buffered_bytes);
                index = 0;

                // third, fill the buffer as far as possible.
                requestFromBuffer(Math.min(capacity, to_convert));
            }
        }

        // enough data in the buffer
        System.arraycopy(buffer, index, a, off, len);
        buffered_bytes -= to_convert;
        index += to_convert;
    }

    @Override
    public void readArray(short[] a, int off, int len) throws IOException {
        int sizeof = Conversion.SHORT_SIZE;
        int useable, converted;
        int to_convert = len * sizeof;

        while (buffered_bytes < to_convert) {
            // not enough data in the buffer

            if (buffered_bytes == 0) {
                index = 0;
                requestFromBuffer(Math.min(capacity, to_convert));
            } else {
                // first, copy the data we do have to 'a' .
                useable = buffered_bytes / sizeof;
                conversion.byte2short(buffer, index, a, off, useable);

                len -= useable;
                off += useable;

                converted = useable * sizeof;
                index += converted;
                buffered_bytes -= converted;
                to_convert -= converted;

                // second, copy the leftovers to the start of the buffer.
                System.arraycopy(buffer, index, buffer, 0, buffered_bytes);
                index = 0;

                // third, fill the buffer as far as possible.
                requestFromBuffer(Math.min(capacity, to_convert));
            }
        }

        // enough data in the buffer
        conversion.byte2short(buffer, index, a, off, len);
        buffered_bytes -= to_convert;
        index += to_convert;
    }

    @Override
    public void readArray(char[] a, int off, int len) throws IOException {
        int sizeof = Conversion.CHAR_SIZE;
        int useable, converted;
        int to_convert = len * sizeof;

        while (buffered_bytes < to_convert) {
            // not enough data in the buffer

            if (buffered_bytes == 0) {
                index = 0;
                requestFromBuffer(Math.min(capacity, to_convert));
            } else {
                // first, copy the data we do have to 'a' .
                useable = buffered_bytes / sizeof;
                conversion.byte2char(buffer, index, a, off, useable);

                len -= useable;
                off += useable;

                converted = useable * sizeof;
                index += converted;
                buffered_bytes -= converted;
                to_convert -= converted;

                // second, copy the leftovers to the start of the buffer.
                System.arraycopy(buffer, index, buffer, 0, buffered_bytes);
                index = 0;

                // third, fill the buffer as far as possible.
                requestFromBuffer(Math.min(capacity, to_convert));
            }
        }

        // enough data in the buffer
        conversion.byte2char(buffer, index, a, off, len);
        buffered_bytes -= to_convert;
        index += to_convert;
    }

    @Override
    public void readArray(int[] a, int off, int len) throws IOException {
        int sizeof = Conversion.INT_SIZE;
        int useable, converted;
        int to_convert = len * sizeof;

        while (buffered_bytes < to_convert) {
            // not enough data in the buffer

            if (buffered_bytes == 0) {
                index = 0;
                requestFromBuffer(Math.min(capacity, to_convert));
            } else {
                // first, copy the data we do have to 'a' .
                useable = buffered_bytes / sizeof;
                conversion.byte2int(buffer, index, a, off, useable);

                len -= useable;
                off += useable;

                converted = useable * sizeof;
                index += converted;
                buffered_bytes -= converted;
                to_convert -= converted;

                // second, copy the leftovers to the start of the buffer.
                System.arraycopy(buffer, index, buffer, 0, buffered_bytes);
                index = 0;

                // third, fill the buffer as far as possible.
                requestFromBuffer(Math.min(capacity, to_convert));
            }
        }

        // enough data in the buffer
        conversion.byte2int(buffer, index, a, off, len);
        buffered_bytes -= to_convert;
        index += to_convert;
    }

    @Override
    public void readArray(long[] a, int off, int len) throws IOException {
        int sizeof = Conversion.LONG_SIZE;
        int useable, converted;
        int to_convert = len * sizeof;

        while (buffered_bytes < to_convert) {
            // not enough data in the buffer

            if (buffered_bytes == 0) {
                index = 0;
                requestFromBuffer(Math.min(capacity, to_convert));
            } else {
                // first, copy the data we do have to 'a' .
                useable = buffered_bytes / sizeof;
                conversion.byte2long(buffer, index, a, off, useable);

                len -= useable;
                off += useable;

                converted = useable * sizeof;
                index += converted;
                buffered_bytes -= converted;
                to_convert -= converted;

                // second, copy the leftovers to the start of the buffer.
                System.arraycopy(buffer, index, buffer, 0, buffered_bytes);
                index = 0;

                // third, fill the buffer as far as possible.
                requestFromBuffer(Math.min(capacity, to_convert));
            }
        }

        // enough data in the buffer
        conversion.byte2long(buffer, index, a, off, len);
        buffered_bytes -= to_convert;
        index += to_convert;
    }

    @Override
    public void readArray(float[] a, int off, int len) throws IOException {
        int sizeof = Conversion.FLOAT_SIZE;
        int useable, converted;
        int to_convert = len * sizeof;

        while (buffered_bytes < to_convert) {
            // not enough data in the buffer

            if (buffered_bytes == 0) {
                index = 0;
                requestFromBuffer(Math.min(capacity, to_convert));
            } else {
                // first, copy the data we do have to 'a' .
                useable = buffered_bytes / sizeof;
                conversion.byte2float(buffer, index, a, off, useable);

                len -= useable;
                off += useable;

                converted = useable * sizeof;
                index += converted;
                buffered_bytes -= converted;
                to_convert -= converted;

                // second, copy the leftovers to the start of the buffer.
                System.arraycopy(buffer, index, buffer, 0, buffered_bytes);
                index = 0;

                // third, fill the buffer as far as possible.
                requestFromBuffer(Math.min(capacity, to_convert));
            }
        }

        // enough data in the buffer
        conversion.byte2float(buffer, index, a, off, len);
        buffered_bytes -= to_convert;
        index += to_convert;
    }

    @Override
    public void readArray(double[] a, int off, int len) throws IOException {
        int sizeof = Conversion.DOUBLE_SIZE;
        int useable, converted;
        int to_convert = len * sizeof;

        while (buffered_bytes < to_convert) {
            // not enough data in the buffer

            if (buffered_bytes == 0) {
                index = 0;
                requestFromBuffer(Math.min(capacity, to_convert));
            } else {
                // first, copy the data we do have to 'a' .
                useable = buffered_bytes / sizeof;
                conversion.byte2double(buffer, index, a, off, useable);

                len -= useable;
                off += useable;

                converted = useable * sizeof;
                index += converted;
                buffered_bytes -= converted;
                to_convert -= converted;

                // second, copy the leftovers to the start of the buffer.
                System.arraycopy(buffer, index, buffer, 0, buffered_bytes);
                index = 0;

                // third, fill the buffer as far as possible.
                requestFromBuffer(Math.min(capacity, to_convert));
            }
        }

        // enough data in the buffer
        conversion.byte2double(buffer, index, a, off, len);
        buffered_bytes -= to_convert;
        index += to_convert;
    }

    @Override
    public byte readByte() throws IOException {
        requestFromBuffer(1);
        buffered_bytes--;
        return buffer[index++];
    }

    @Override
    public boolean readBoolean() throws IOException {
        requestFromBuffer(1);
        buffered_bytes--;
        return conversion.byte2boolean(buffer[index++]);
    }

    @Override
    public char readChar() throws IOException {
        int sizeof = Conversion.CHAR_SIZE;
        char v;
        requestFromBuffer(sizeof);
        v = conversion.byte2char(buffer, index);
        index += sizeof;
        buffered_bytes -= sizeof;
        return v;
    }

    @Override
    public short readShort() throws IOException {
        int sizeof = Conversion.SHORT_SIZE;
        short v;
        requestFromBuffer(sizeof);
        v = conversion.byte2short(buffer, index);
        index += sizeof;
        buffered_bytes -= sizeof;
        return v;
    }

    @Override
    public int readInt() throws IOException {
        int sizeof = Conversion.INT_SIZE;
        int v;
        requestFromBuffer(sizeof);
        v = conversion.byte2int(buffer, index);
        index += sizeof;
        buffered_bytes -= sizeof;
        return v;
    }

    @Override
    public long readLong() throws IOException {
        int sizeof = Conversion.LONG_SIZE;
        long v;
        requestFromBuffer(sizeof);
        v = conversion.byte2long(buffer, index);
        index += sizeof;
        buffered_bytes -= sizeof;
        return v;
    }

    @Override
    public float readFloat() throws IOException {
        int sizeof = Conversion.FLOAT_SIZE;
        float v;
        requestFromBuffer(sizeof);
        v = conversion.byte2float(buffer, index);
        index += sizeof;
        buffered_bytes -= sizeof;
        return v;
    }

    @Override
    public double readDouble() throws IOException {
        int sizeof = Conversion.DOUBLE_SIZE;
        double v;
        requestFromBuffer(sizeof);
        v = conversion.byte2double(buffer, index);
        index += sizeof;
        buffered_bytes -= sizeof;
        return v;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] a, int off, int len) throws IOException {
        readArray(a, off, len);
        return len;
    }

    @Override
    abstract public void close() throws IOException;

    @Override
    public int bufferSize() {
        return capacity;
    }

    @Override
    public void readByteBuffer(ByteBuffer value) throws IOException,
            ReadOnlyBufferException {

        int len = value.limit() - value.position();

        if (buffered_bytes >= len) {
            // data is already in the buffer.
            value.put(buffer, index, len);
            index += len;
            buffered_bytes -= len;
        } else {
            if (buffered_bytes != 0) {
                // first, copy the data we do have to 'a' .
                value.put(buffer, index, buffered_bytes);
                len -= buffered_bytes;
                buffered_bytes = 0;
            }
            index = 0;
            while(len > 0) {
                int toRead = Math.min(len, capacity);
                requestFromBuffer(toRead);
                toRead = Math.min(len, buffered_bytes);
                value.put(buffer, index, toRead);
                len -= toRead;
                index += toRead;
                buffered_bytes -= toRead;
            }
        }
    }
}
