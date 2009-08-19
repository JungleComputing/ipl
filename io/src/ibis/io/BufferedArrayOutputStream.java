/* $Id$ */

package ibis.io;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a complete implementation of <code>DataOutputStream</code>.
 * It is built on top of an <code>OutputStream</code>.
 * There is no need to put any buffering inbetween. This implementation
 * does all the buffering needed.
 */
public final class BufferedArrayOutputStream extends DataOutputStream {
    
    private static final Logger logger = LoggerFactory.getLogger(BufferedArrayOutputStream.class);

    private static final boolean DEBUG = IOProperties.DEBUG;

    /** Size of the buffer in which output data is collected. */
    private final int BUF_SIZE;

    /** The underlying <code>OutputStream</code>. */
    private OutputStream out;

    /** The buffer in which output data is collected. */
    private byte[] buffer;

    /** Size of the buffer in which output data is collected. */
    private int index = 0;

    /** Number of bytes written so far to the underlying layer. */
    private long bytes = 0;

    /** Object used for conversion of primitive types to bytes. */
    private Conversion conversion;

    /**
     * Constructor.
     * @param out	the underlying <code>OutputStream</code>
     * @param bufSize   the size of the output buffer in bytes
     */
    public BufferedArrayOutputStream(OutputStream out, int bufSize) {
        this.out = out;
        BUF_SIZE = bufSize;
        buffer = new byte[BUF_SIZE];
        conversion = Conversion.loadConversion(false);
    }

    /**
     * Constructor.
     * @param out       the underlying <code>OutputStream</code>
     */
    public BufferedArrayOutputStream(OutputStream out) {
        this(out, IOProperties.BUFFER_SIZE);
    }

    public long bytesWritten() {
        return bytes + index;
    }

    public void resetBytesWritten() {
        bytes = index;
    }

    /**
     * Checks if there is space for <code>incr</code> more bytes and if not,
     * the buffer is written to the underlying <code>OutputStream</code>.
     *
     * @param incr		the space requested
     * @exception IOException	in case of trouble.
     */
    private void flush(int incr) throws IOException {

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("flush(" + incr + ") : " + " "
                    + (index + incr >= BUF_SIZE) + " " + (index) + ")");
        }

        if (index + incr > BUF_SIZE) {
            bytes += index;

            out.write(buffer, 0, index);
            index = 0;
        }
    }

    public void write(int b) throws IOException {
        writeByte((byte) b);
    }

    public void writeBoolean(boolean value) throws IOException {
        byte b = conversion.boolean2byte(value);
        flush(1);
        buffer[index++] = b;
    }

    public void writeByte(byte value) throws IOException {
        flush(1);
        buffer[index++] = value;
    }

    public void writeChar(char value) throws IOException {
        flush(Constants.SIZEOF_CHAR);
        conversion.char2byte(value, buffer, index);
        index += Constants.SIZEOF_CHAR;
    }

    public void writeShort(short value) throws IOException {
        flush(Constants.SIZEOF_SHORT);
        conversion.short2byte(value, buffer, index);
        index += Constants.SIZEOF_SHORT;
    }

    public void writeInt(int value) throws IOException {
        flush(Constants.SIZEOF_INT);
        conversion.int2byte(value, buffer, index);
        index += Constants.SIZEOF_INT;
    }

    public void writeLong(long value) throws IOException {
        flush(Constants.SIZEOF_LONG);
        conversion.long2byte(value, buffer, index);
        index += Constants.SIZEOF_LONG;
    }

    public void writeFloat(float value) throws IOException {
        flush(Constants.SIZEOF_FLOAT);
        conversion.float2byte(value, buffer, index);
        index += Constants.SIZEOF_FLOAT;
    }

    public void writeDouble(double value) throws IOException {
        flush(Constants.SIZEOF_DOUBLE);
        conversion.double2byte(value, buffer, index);
        index += Constants.SIZEOF_DOUBLE;
    }

    public void write(byte[] b) throws IOException {
        writeArray(b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        writeArray(b, off, len);
    }

    public void writeArray(boolean[] ref, int off, int len)
            throws IOException {
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("writeArray(boolean[" + off + " ... "
                    + (off + len) + "])");
        }

        do {
            flush(1);

            int size = Math.min(BUF_SIZE - index, len);

            conversion.boolean2byte(ref, off, size, buffer, index);

            off += size;
            index += size;
            len -= size;

        } while (len != 0);
    }

    public void writeArray(byte[] ref, int off, int len)
            throws IOException {
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("writeArray(byte[" + off + " ... " + (off + len)
                    + "])");
        }

        if (len > (BUF_SIZE - index)) {

            if (index > 0) {
                bytes += index;
                out.write(buffer, 0, index);
                index = 0;
            }
            if (len >= BUF_SIZE) {
                bytes += len;
                out.write(ref, off, len);
            } else {
                System.arraycopy(ref, off, buffer, 0, len);
                index = len;
            }
        } else {
            System.arraycopy(ref, off, buffer, index, len);
            index += len;
        }
    }

    public void writeArray(char[] ref, int off, int len)
            throws IOException {
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("writeArray(char[" + off + " ... " + (off + len)
                    + "])");
        }

        do {
            flush(Constants.SIZEOF_CHAR);

            int size = Math.min((BUF_SIZE - index) / Constants.SIZEOF_CHAR, len);

            conversion.char2byte(ref, off, size, buffer, index);

            off += size;
            len -= size;
            index += size * Constants.SIZEOF_CHAR;

        } while (len != 0);
    }

    public void writeArray(short[] ref, int off, int len)
            throws IOException {
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("writeArray(short[" + off + " ... "
                    + (off + len) + "])");
        }

        do {
            flush(Constants.SIZEOF_SHORT);

            int size = Math.min((BUF_SIZE - index) / Constants.SIZEOF_SHORT, len);

            conversion.short2byte(ref, off, size, buffer, index);

            off += size;
            len -= size;
            index += size * Constants.SIZEOF_SHORT;

        } while (len != 0);
    }

    public void writeArray(int[] ref, int off, int len)
            throws IOException {
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("writeArray(int[" + off + " ... " + (off + len)
                    + "])");
        }

        do {
            flush(Constants.SIZEOF_INT);

            int size = Math.min((BUF_SIZE - index) / Constants.SIZEOF_INT, len);

            conversion.int2byte(ref, off, size, buffer, index);

            off += size;
            len -= size;
            index += size * Constants.SIZEOF_INT;

        } while (len != 0);
    }

    public void writeArray(long[] ref, int off, int len)
            throws IOException {
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("writeArray(long[" + off + " ... " + (off + len)
                    + "])");
        }

        do {
            flush(Constants.SIZEOF_LONG);

            int size = Math.min((BUF_SIZE - index) / Constants.SIZEOF_LONG, len);

            conversion.long2byte(ref, off, size, buffer, index);

            off += size;
            len -= size;
            index += size * Constants.SIZEOF_LONG;

        } while (len != 0);
    }

    public void writeArray(float[] ref, int off, int len)
            throws IOException {
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("writeArray(float[" + off + " ... "
                    + (off + len) + "])");
        }
        do {
            flush(Constants.SIZEOF_FLOAT);

            int size = Math.min((BUF_SIZE - index) / Constants.SIZEOF_FLOAT, len);

            conversion.float2byte(ref, off, size, buffer, index);

            off += size;
            len -= size;
            index += size * Constants.SIZEOF_FLOAT;

        } while (len != 0);
    }

    public void writeArray(double[] ref, int off, int len)
            throws IOException {
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("writeArray(double[" + off + " ... "
                    + (off + len) + "])");
        }

        do {
            flush(Constants.SIZEOF_DOUBLE);

            int size = Math.min((BUF_SIZE - index) / Constants.SIZEOF_DOUBLE, len);

            conversion.double2byte(ref, off, size, buffer, index);

            off += size;
            len -= size;
            index += size * Constants.SIZEOF_DOUBLE;

        } while (len != 0);
    }

    public void flush() throws IOException {
        flush(BUF_SIZE + 1); /* Forces flush */
        out.flush();
    }

    public void finish() {
        // empty
    }

    public boolean finished() {
        return true;
    }

    public void close() throws IOException {
        flush();
        out.close();
    }
    
    public int bufferSize() {
        return BUF_SIZE;
    }
}
