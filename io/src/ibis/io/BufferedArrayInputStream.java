/* $Id$ */

package ibis.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;

/**
 * This is a complete implementation of <code>DataInputStream</code>.
 * It is built on top of an <code>InputStream</code>.
 * There is no need to put any buffering inbetween. This implementation
 * does all the buffering needed.
 */
public final class BufferedArrayInputStream extends DataInputStream {
    
    private static final boolean DEBUG = IOProperties.DEBUG;
    
    private static final Logger logger = Logger.getLogger(BufferedArrayInputStream.class);

    /** The buffer size. */
    private final int BUF_SIZE;

    /** The underlying <code>InputStream</code>. */
    private InputStream in;

    /** The buffer. */
    private byte[] buffer;
    
    private int index, buffered_bytes;

    /** Number of bytes read so far from the underlying layer. */
    private long bytes = 0;

    /** Object used to convert primitive types to bytes. */
    private Conversion conversion;

    public BufferedArrayInputStream(InputStream in, int bufSize) {
        this.in = in;
        BUF_SIZE = bufSize;
        buffer = new byte[BUF_SIZE];
        conversion = Conversion.loadConversion(false);
    }

    public long bytesRead() {
        return bytes - buffered_bytes;
    }

    public void resetBytesRead() {
        bytes = buffered_bytes;
    }

    private static final int min(int a, int b) {
        return (a > b) ? b : a;
    }

    public final int read() throws IOException {
        try {
            int b = readByte();
            return (b & 0377);
        } catch(EOFException e) {
            return -1;
        }
    }

    private final void fillBuffer(int len) throws IOException {

        // This ensures that there are at least 'len' bytes in the buffer
        // PRECONDITION: 'index + buffered_bytes' should never be larger
        // than BUF_SIZE!!

        if (buffered_bytes >= len) {
            return;
        }
        if (buffered_bytes == 0) {
            index = 0;
        } else if (index + buffered_bytes > BUF_SIZE - len) {
            // not enough space for "len" more bytes
            System.arraycopy(buffer, index, buffer, 0, buffered_bytes);
            index = 0;
        }
        while (buffered_bytes < len) {
            int n = in.read(buffer, index + buffered_bytes, BUF_SIZE
                    - (index + buffered_bytes));
            if (n < 0) {
                throw new java.io.EOFException("EOF encountered");
            }
            bytes += n;
            buffered_bytes += n;
        }
    }

    public final int available() throws IOException {
        return (buffered_bytes + in.available());
    }

    public void readArray(boolean[] a, int off, int len) throws IOException {

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("readArray(boolean[" + off + " ... "
                    + (off + len) + "])");
        }

        int useable, converted;
        int to_convert = len * Constants.SIZEOF_BOOLEAN;

        while (buffered_bytes < to_convert) {
            // not enough data in the buffer

            if (buffered_bytes == 0) {
                index = 0;
                fillBuffer(min(BUF_SIZE, to_convert));
            } else {
                // first, copy the data we do have to 'a' .
                useable = buffered_bytes / Constants.SIZEOF_BOOLEAN;
                conversion.byte2boolean(buffer, index, a, off, useable);

                len -= useable;
                off += useable;

                converted = useable * Constants.SIZEOF_BOOLEAN;
                index += converted;
                buffered_bytes -= converted;
                to_convert -= converted;

                // second, copy the leftovers to the start of the buffer.
                for (int i = 0; i < buffered_bytes; i++) {
                    buffer[i] = buffer[index + i];
                }
                index = 0;

                // third, fill the buffer as far as possible.
                fillBuffer(min(BUF_SIZE, to_convert));
            }
        }

        // enough data in the buffer
        conversion.byte2boolean(buffer, index, a, off, len);
        buffered_bytes -= to_convert;
        index += to_convert;
    }

    public void readArray(byte[] a, int off, int len) throws IOException {
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("readArray(byte[" + off + " ... " + (off + len)
                    + "])");
        }

        if (buffered_bytes >= len) {
            // data is already in the buffer.
            System.arraycopy(buffer, index, a, off, len);
            index += len;
            buffered_bytes -= len;

        } else {
            if (buffered_bytes != 0) {
                // first, copy the data we do have to 'a' .
                System.arraycopy(buffer, index, a, off, buffered_bytes);
            }
            int rd = buffered_bytes;
            index = 0;
            do {
                int n = in.read(a, off + rd, len - rd);
                if (n < 0) {
                    throw new java.io.EOFException("EOF encountered");
                }
                rd += n;
                bytes += n;
            } while (rd < len);

            buffered_bytes = 0;
        }
    }

    // static int R = 0;
    // static int W = 0;

    public void readArray(short[] a, int off, int len) throws IOException {

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("readArray(char[" + off + " ... " + (off + len)
                    + "])");
        }

        int useable, converted;
        int to_convert = len * Constants.SIZEOF_SHORT;

        while (buffered_bytes < to_convert) {
            // not enough data in the buffer

            if (buffered_bytes == 0) {
                index = 0;
                fillBuffer(min(BUF_SIZE, to_convert));
            } else {
                // first, copy the data we do have to 'a' .
                useable = buffered_bytes / Constants.SIZEOF_SHORT;
                conversion.byte2short(buffer, index, a, off, useable);

                len -= useable;
                off += useable;

                converted = useable * Constants.SIZEOF_SHORT;
                index += converted;
                buffered_bytes -= converted;
                to_convert -= converted;

                // second, copy the leftovers to the start of the buffer.
                for (int i = 0; i < buffered_bytes; i++) {
                    buffer[i] = buffer[index + i];
                }
                index = 0;

                // third, fill the buffer as far as possible.
                fillBuffer(min(BUF_SIZE, to_convert));
            }
        }

        // enough data in the buffer
        conversion.byte2short(buffer, index, a, off, len);
        buffered_bytes -= to_convert;
        index += to_convert;
    }

    public void readArray(char[] a, int off, int len) throws IOException {

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("readArray(char[" + off + " ... " + (off + len)
                    + "])");
        }

        int useable, converted;
        int to_convert = len * Constants.SIZEOF_CHAR;

        while (buffered_bytes < to_convert) {
            // not enough data in the buffer

            if (buffered_bytes == 0) {
                index = 0;
                fillBuffer(min(BUF_SIZE, to_convert));
            } else {
                // first, copy the data we do have to 'a' .
                useable = buffered_bytes / Constants.SIZEOF_CHAR;
                conversion.byte2char(buffer, index, a, off, useable);

                len -= useable;
                off += useable;

                converted = useable * Constants.SIZEOF_CHAR;
                index += converted;
                buffered_bytes -= converted;
                to_convert -= converted;

                // second, copy the leftovers to the start of the buffer.
                for (int i = 0; i < buffered_bytes; i++) {
                    buffer[i] = buffer[index + i];
                }
                index = 0;

                // third, fill the buffer as far as possible.
                fillBuffer(min(BUF_SIZE, to_convert));
            }
        }

        // enough data in the buffer
        conversion.byte2char(buffer, index, a, off, len);
        buffered_bytes -= to_convert;
        index += to_convert;
    }

    public void readArray(int[] a, int off, int len) throws IOException {

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("readArray(int[" + off + " ... " + (off + len)
                    + "])");
        }

        int useable, converted;
        int to_convert = len * Constants.SIZEOF_INT;

        while (buffered_bytes < to_convert) {
            // not enough data in the buffer

            if (buffered_bytes == 0) {
                index = 0;
                fillBuffer(min(BUF_SIZE, to_convert));
            } else {
                // first, copy the data we do have to 'a' .
                useable = buffered_bytes / Constants.SIZEOF_INT;

                conversion.byte2int(buffer, index, a, off, useable);

                len -= useable;
                off += useable;

                converted = useable * Constants.SIZEOF_INT;
                index += converted;
                buffered_bytes -= converted;
                to_convert -= converted;

                // second, copy the leftovers to the start of the buffer.
                for (int i = 0; i < buffered_bytes; i++) {
                    buffer[i] = buffer[index + i];
                }
                index = 0;

                // third, fill the buffer as far as possible.
                fillBuffer(min(BUF_SIZE, to_convert));
            }
        }

        // enough data in the buffer

        conversion.byte2int(buffer, index, a, off, len);
        buffered_bytes -= to_convert;
        index += to_convert;
    }

    public void readArray(long[] a, int off, int len) throws IOException {

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("readArray(long[" + off + " ... " + (off + len)
                    + "])");
        }

        int useable, converted;
        int to_convert = len * Constants.SIZEOF_LONG;

        while (buffered_bytes < to_convert) {
            // not enough data in the buffer

            if (buffered_bytes == 0) {
                index = 0;
                fillBuffer(min(BUF_SIZE, to_convert));
            } else {
                // first, copy the data we do have to 'a' .
                useable = buffered_bytes / Constants.SIZEOF_LONG;
                conversion.byte2long(buffer, index, a, off, useable);

                len -= useable;
                off += useable;

                converted = useable * Constants.SIZEOF_LONG;
                index += converted;
                buffered_bytes -= converted;
                to_convert -= converted;

                // second, copy the leftovers to the start of the buffer.
                for (int i = 0; i < buffered_bytes; i++) {
                    buffer[i] = buffer[index + i];
                }
                index = 0;

                // third, fill the buffer as far as possible.
                fillBuffer(min(BUF_SIZE, to_convert));
            }
        }

        // enough data in the buffer
        conversion.byte2long(buffer, index, a, off, len);
        buffered_bytes -= to_convert;
        index += to_convert;
    }

    public void readArray(float[] a, int off, int len) throws IOException {

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("readArray(float[" + off + " ... " + (off + len)
                    + "])");
        }

        int useable, converted;
        int to_convert = len * Constants.SIZEOF_FLOAT;

        while (buffered_bytes < to_convert) {
            // not enough data in the buffer

            if (buffered_bytes == 0) {
                index = 0;
                fillBuffer(min(BUF_SIZE, to_convert));
            } else {
                // first, copy the data we do have to 'a' .
                useable = buffered_bytes / Constants.SIZEOF_FLOAT;
                conversion.byte2float(buffer, index, a, off, useable);

                len -= useable;
                off += useable;

                converted = useable * Constants.SIZEOF_FLOAT;
                index += converted;
                buffered_bytes -= converted;
                to_convert -= converted;

                // second, copy the leftovers to the start of the buffer.
                for (int i = 0; i < buffered_bytes; i++) {
                    buffer[i] = buffer[index + i];
                }
                index = 0;

                // third, fill the buffer as far as possible.
                fillBuffer(min(BUF_SIZE, to_convert));
            }
        }

        // enough data in the buffer
        conversion.byte2float(buffer, index, a, off, len);
        buffered_bytes -= to_convert;
        index += to_convert;
    }

    public void readArray(double[] a, int off, int len) throws IOException {

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("readArray(double[" + off + " ... "
                    + (off + len) + "])");
        }

        int useable, converted;
        int to_convert = len * Constants.SIZEOF_DOUBLE;

        while (buffered_bytes < to_convert) {
            // not enough data in the buffer

            if (buffered_bytes == 0) {
                index = 0;
                fillBuffer(min(BUF_SIZE, to_convert));
            } else {
                // first, copy the data we do have to 'a' .
                useable = buffered_bytes / Constants.SIZEOF_DOUBLE;
                conversion.byte2double(buffer, index, a, off, useable);

                len -= useable;
                off += useable;

                converted = useable * Constants.SIZEOF_DOUBLE;
                index += converted;
                buffered_bytes -= converted;
                to_convert -= converted;

                // second, copy the leftovers to the start of the buffer.
                for (int i = 0; i < buffered_bytes; i++) {
                    buffer[i] = buffer[index + i];
                }
                index = 0;

                // third, fill the buffer as far as possible.
                fillBuffer(min(BUF_SIZE, to_convert));
            }
        }

        // enough data in the buffer
        conversion.byte2double(buffer, index, a, off, len);
        buffered_bytes -= to_convert;
        index += to_convert;
    }

    public byte readByte() throws IOException {
        fillBuffer(1);
        buffered_bytes--;
        return buffer[index++];
    }

    public boolean readBoolean() throws IOException {
        fillBuffer(1);
        buffered_bytes--;
        return conversion.byte2boolean(buffer[index++]);
    }

    public char readChar() throws IOException {
        char v;
        fillBuffer(Constants.SIZEOF_CHAR);
        v = conversion.byte2char(buffer, index);
        index += Constants.SIZEOF_CHAR;
        buffered_bytes -= Constants.SIZEOF_CHAR;
        return v;
    }

    public short readShort() throws IOException {
        short v;
        fillBuffer(Constants.SIZEOF_SHORT);
        v = conversion.byte2short(buffer, index);
        index += Constants.SIZEOF_SHORT;
        buffered_bytes -= Constants.SIZEOF_SHORT;
        return v;
    }

    public int readInt() throws IOException {
        int v;
        fillBuffer(Constants.SIZEOF_INT);
        v = conversion.byte2int(buffer, index);
        index += Constants.SIZEOF_INT;
        buffered_bytes -= Constants.SIZEOF_INT;
        return v;
    }

    public long readLong() throws IOException {
        long v;
        fillBuffer(Constants.SIZEOF_LONG);
        v = conversion.byte2long(buffer, index);
        index += Constants.SIZEOF_LONG;
        buffered_bytes -= Constants.SIZEOF_LONG;
        return v;
    }

    public float readFloat() throws IOException {
        float v;
        fillBuffer(Constants.SIZEOF_FLOAT);
        v = conversion.byte2float(buffer, index);
        index += Constants.SIZEOF_FLOAT;
        buffered_bytes -= Constants.SIZEOF_FLOAT;
        return v;
    }

    public double readDouble() throws IOException {
        double v;
        fillBuffer(Constants.SIZEOF_DOUBLE);
        v = conversion.byte2double(buffer, index);
        index += Constants.SIZEOF_DOUBLE;
        buffered_bytes -= Constants.SIZEOF_DOUBLE;
        return v;
    }

    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public int read(byte[] a, int off, int len) throws IOException {
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("read(byte[" + off + " ... " + (off + len)
                    + "])");
        }

        if (buffered_bytes >= len) {
            // data is already in the buffer.

            System.arraycopy(buffer, index, a, off, len);
            index += len;
            buffered_bytes -= len;
        } else {
            if (buffered_bytes != 0) {
                // first, copy the data we do have to 'a' .
                System.arraycopy(buffer, index, a, off, buffered_bytes);
            }
            int rd = buffered_bytes;
            index = 0;
            do {
                int n = in.read(a, off + rd, len - rd);
                if (n < 0) {
                    len = rd;
                }
                else {
                    rd += n;
                    bytes += n;
                }
            } while (rd < len);

            buffered_bytes = 0;
        }

        return len;
    }

    public void close() throws IOException {
        in.close();
    }
    
    public int bufferSize() {
        return BUF_SIZE;
    }
}
