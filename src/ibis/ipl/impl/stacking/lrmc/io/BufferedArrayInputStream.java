/* $Id$ */

package ibis.ipl.impl.stacking.lrmc.io;

import ibis.io.Conversion;
import ibis.io.DataInputStream;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;

/**
 * This is a complete implementation of <code>DataInputStream</code>. It is
 * built on top of an <code>InputStream</code>. There is no need to put any
 * buffering in between. This implementation does all the buffering needed.
 */
public final class BufferedArrayInputStream extends DataInputStream {

    private static final int SIZEOF_BOOLEAN = 1;

    private static final int SIZEOF_CHAR = 2;

    private static final int SIZEOF_SHORT = 2;

    private static final int SIZEOF_INT = 4;

    private static final int SIZEOF_LONG = 8;

    private static final int SIZEOF_FLOAT = 4;

    private static final int SIZEOF_DOUBLE = 8;

    private static boolean DEBUG = false;

    /** The buffer size. */
    private static final int BUF_SIZE = 8 * 1024;

    /** The underlying <code>InputStream</code>. */
    private LrmcInputStream in;

    /** The buffer. */
    private byte[] buffer = new byte[BUF_SIZE];

    private int index, buffered_bytes;

    /** Number of bytes read so far from the underlying layer. */
    private long bytes = 0;

    /** Object used to convert primitive types to bytes. */
    private Conversion conversion;

    public BufferedArrayInputStream(LrmcInputStream in) {
        this.in = in;
        conversion = Conversion.loadConversion(false);
    }

    public int bufferSize() {
        return BUF_SIZE;
    }

    public void setInputStream(LrmcInputStream in) {
        this.in = in;
    }

    public LrmcInputStream getInputStream() {
        return in;
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
            byte b = readByte();
            return (b & 0377);
        } catch (EOFException e) {
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
            // System.err.println("buffer -> filled from " + index + " with "
            // + buffered_bytes + " size " + BUF_SIZE + " read " + len);

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
        return buffered_bytes;
    }

    public void readArray(boolean[] a, int off, int len) throws IOException {

        if (DEBUG) {
            System.err.println("readArray(boolean[" + off + " ... "
                    + (off + len) + "])");
        }

        int useable, converted;
        int to_convert = len * SIZEOF_BOOLEAN;

        while (buffered_bytes < to_convert) {
            // not enough data in the buffer

            if (buffered_bytes == 0) {
                index = 0;
                fillBuffer(min(BUF_SIZE, to_convert));
            } else {
                // first, copy the data we do have to 'a' .
                useable = buffered_bytes / SIZEOF_BOOLEAN;
                conversion.byte2boolean(buffer, index, a, off, useable);

                len -= useable;
                off += useable;

                converted = useable * SIZEOF_BOOLEAN;
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
        if (DEBUG) {
            System.err.println("readArray(byte[" + off + " ... " + (off + len)
                    + "])");
        }

        if (buffered_bytes >= len) {
            // System.err.println("IN BUF");

            // data is already in the buffer.
            // System.err.println("Data is in buffer -> copying " + index +
            // " ... " + (index+len) + " to " + off);

            System.arraycopy(buffer, index, a, off, len);
            index += len;
            buffered_bytes -= len;

            // System.err.println("DONE");

        } else {
            if (buffered_bytes != 0) {
                // System.err.println("PARTLY IN BUF " + buffered_bytes
                // + " " + len);
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

        // System.err.print("result -> byte[");
        // for (int i=0;i<len;i++) {
        // System.err.print(a[off+i] + ",");
        // }
        // System.err.println("]");
    }

    // static int R = 0;
    // static int W = 0;

    public void readArray(short[] a, int off, int len) throws IOException {

        int useable, converted;
        int to_convert = len * SIZEOF_SHORT;

        while (buffered_bytes < to_convert) {
            // not enough data in the buffer

            if (buffered_bytes == 0) {
                index = 0;
                fillBuffer(min(BUF_SIZE, to_convert));
            } else {
                // first, copy the data we do have to 'a' .
                useable = buffered_bytes / SIZEOF_SHORT;
                conversion.byte2short(buffer, index, a, off, useable);

                len -= useable;
                off += useable;

                converted = useable * SIZEOF_SHORT;
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

        if (DEBUG) {
            System.err.print("readArray(short[");
            for (int i = 0; i < len; i++) {
                System.err.print(a[off + i] + ",");
            }
            System.err.println("]");
            System.err.flush();
        }
    }

    public void readArray(char[] a, int off, int len) throws IOException {

        if (DEBUG) {
            System.err.println("readArray(char[" + off + " ... " + (off + len)
                    + "])");
        }

        int useable, converted;
        int to_convert = len * SIZEOF_CHAR;

        while (buffered_bytes < to_convert) {
            // not enough data in the buffer

            if (buffered_bytes == 0) {
                index = 0;
                fillBuffer(min(BUF_SIZE, to_convert));
            } else {
                // first, copy the data we do have to 'a' .
                useable = buffered_bytes / SIZEOF_CHAR;
                conversion.byte2char(buffer, index, a, off, useable);

                len -= useable;
                off += useable;

                converted = useable * SIZEOF_CHAR;
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

        if (DEBUG) {
            System.err.println("readArray(int[" + off + " ... " + (off + len)
                    + "])");
        }

        int useable, converted;
        int to_convert = len * SIZEOF_INT;

        // System.err.println("To convert " + to_convert);
        // System.err.println("Buffered " + buffered_bytes);

        while (buffered_bytes < to_convert) {
            // not enough data in the buffer

            if (buffered_bytes == 0) {
                index = 0;
                fillBuffer(min(BUF_SIZE, to_convert));
            } else {
                // first, copy the data we do have to 'a' .
                useable = buffered_bytes / SIZEOF_INT;

                // System.err.println("converting " + useable + " ints from "
                // + off);
                conversion.byte2int(buffer, index, a, off, useable);

                len -= useable;
                off += useable;

                converted = useable * SIZEOF_INT;
                index += converted;
                buffered_bytes -= converted;
                to_convert -= converted;

                // System.err.println("Leftover " + len + " ints to convert, "
                // + buffered_bytes + " bytes buffered"
                // + to_convert + " bytes to convert");

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
        // System.err.println("converting " + len + " ints from " + index
        // + " to " + off);

        conversion.byte2int(buffer, index, a, off, len);
        buffered_bytes -= to_convert;
        index += to_convert;

        // System.err.println("Done converting int [], buffer contains "
        // + buffered_bytes + " bytes (starting at " + index + ")");

    }

    public void readArray(long[] a, int off, int len) throws IOException {

        if (DEBUG) {
            System.err.println("readArray(long[" + off + " ... " + (off + len)
                    + "])");
        }

        int useable, converted;
        int to_convert = len * SIZEOF_LONG;

        while (buffered_bytes < to_convert) {
            // not enough data in the buffer

            if (buffered_bytes == 0) {
                index = 0;
                fillBuffer(min(BUF_SIZE, to_convert));
            } else {
                // first, copy the data we do have to 'a' .
                useable = buffered_bytes / SIZEOF_LONG;
                conversion.byte2long(buffer, index, a, off, useable);

                len -= useable;
                off += useable;

                converted = useable * SIZEOF_LONG;
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

        if (DEBUG) {
            System.err.println("readArray(float[" + off + " ... " + (off + len)
                    + "])");
        }

        int useable, converted;
        int to_convert = len * SIZEOF_FLOAT;

        while (buffered_bytes < to_convert) {
            // not enough data in the buffer

            if (buffered_bytes == 0) {
                index = 0;
                fillBuffer(min(BUF_SIZE, to_convert));
            } else {
                // first, copy the data we do have to 'a' .
                useable = buffered_bytes / SIZEOF_FLOAT;
                conversion.byte2float(buffer, index, a, off, useable);

                len -= useable;
                off += useable;

                converted = useable * SIZEOF_FLOAT;
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

        if (DEBUG) {
            System.err.println("readArray(double[" + off + " ... "
                    + (off + len) + "])");
        }

        int useable, converted;
        int to_convert = len * SIZEOF_DOUBLE;

        while (buffered_bytes < to_convert) {
            // not enough data in the buffer

            if (buffered_bytes == 0) {
                index = 0;
                fillBuffer(min(BUF_SIZE, to_convert));
            } else {
                // first, copy the data we do have to 'a' .
                useable = buffered_bytes / SIZEOF_DOUBLE;
                conversion.byte2double(buffer, index, a, off, useable);

                len -= useable;
                off += useable;

                converted = useable * SIZEOF_DOUBLE;
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
        fillBuffer(SIZEOF_CHAR);
        v = conversion.byte2char(buffer, index);
        index += SIZEOF_CHAR;
        buffered_bytes -= SIZEOF_CHAR;
        return v;
    }

    public short readShort() throws IOException {
        short v;
        fillBuffer(SIZEOF_SHORT);
        v = conversion.byte2short(buffer, index);
        index += SIZEOF_SHORT;
        buffered_bytes -= SIZEOF_SHORT;
        return v;
    }

    public int readInt() throws IOException {
        int v;
        fillBuffer(SIZEOF_INT);
        v = conversion.byte2int(buffer, index);
        index += SIZEOF_INT;
        buffered_bytes -= SIZEOF_INT;
        return v;
    }

    public long readLong() throws IOException {
        long v;
        fillBuffer(SIZEOF_LONG);
        v = conversion.byte2long(buffer, index);
        index += SIZEOF_LONG;
        buffered_bytes -= SIZEOF_LONG;
        return v;
    }

    public float readFloat() throws IOException {
        float v;
        fillBuffer(SIZEOF_FLOAT);
        v = conversion.byte2float(buffer, index);
        index += SIZEOF_FLOAT;
        buffered_bytes -= SIZEOF_FLOAT;
        return v;
    }

    public double readDouble() throws IOException {
        double v;
        fillBuffer(SIZEOF_DOUBLE);
        v = conversion.byte2double(buffer, index);
        index += SIZEOF_DOUBLE;
        buffered_bytes -= SIZEOF_DOUBLE;
        return v;
    }

    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public int read(byte[] a, int off, int len) throws IOException {
        if (DEBUG) {
            System.err.println("read(byte[" + off + " ... " + (off + len)
                    + "])");
        }

        if (buffered_bytes >= len) {
            // data is already in the buffer.
            // System.err.println("Data is in buffer -> copying " + index +
            // " ... " + (index+len) + " to " + off);

            System.arraycopy(buffer, index, a, off, len);
            index += len;
            buffered_bytes -= len;
        } else {
            if (buffered_bytes != 0) {
                // System.err.println("PARTLY IN BUF " + buffered_bytes
                // + " " + len);
                // first, copy the data we do have to 'a' .
                System.arraycopy(buffer, index, a, off, buffered_bytes);
            }
            int rd = buffered_bytes;
            index = 0;
            do {
                int n = in.read(a, off + rd, len - rd);
                if (n < 0) {
                    len = rd;
                } else {
                    rd += n;
                    bytes += n;
                }
            } while (rd < len);

            buffered_bytes = 0;
        }

        // System.err.print("result -> byte[");
        // for (int i=0;i<len;i++) {
        // System.err.print(a[off+i] + ",");
        // }
        // System.err.println("]");

        return len;

    }

    public void close() throws IOException {
        in.close();
    }

    @Override
    public void readByteBuffer(ByteBuffer value) throws IOException,
	    ReadOnlyBufferException {

	int len = value.limit() - value.position();
	
        if (buffered_bytes >= len) {
            value.put(buffer, index, len);
            index += len;
            buffered_bytes -= len;
        } else {
            if (buffered_bytes != 0) {
        	value.put(buffer, index, buffered_bytes);
        	len -= buffered_bytes;
        	buffered_bytes = 0;
            }
            index = 0;
            if (value.hasArray()) {
        	in.read(value.array(), value.arrayOffset(), len);
        	value.position(value.limit());
        	bytes += len;
            } else {
        	do {
        	    int toread = Math.min(len, BUF_SIZE);
        	    fillBuffer(toread);
        	    if (len < buffered_bytes) {
        		toread = len;
        	    } else {
        		toread = buffered_bytes;
        	    }
        	    value.put(buffer, index, toread);
        	    len -= toread;
        	    index += toread;
        	    buffered_bytes -= toread;
        	} while (len > 0);
            }
        }	
    }
}
