/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* $Id$ */

package ibis.ipl.impl.stacking.lrmc.io;

import java.io.IOException;
import java.nio.ByteBuffer;

import ibis.io.Conversion;
import ibis.io.DataOutputStream;

/**
 * This is a complete implementation of <code>DataOutputStream</code>. It is
 * built on top of an <code>OutputStream</code>. There is no need to put any
 * buffering inbetween. This implementation does all the buffering needed.
 */
public final class BufferedArrayOutputStream extends DataOutputStream {

    private static final int SIZEOF_CHAR = 2;

    private static final int SIZEOF_SHORT = 2;

    private static final int SIZEOF_INT = 4;

    private static final int SIZEOF_LONG = 8;

    private static final int SIZEOF_FLOAT = 4;

    private static final int SIZEOF_DOUBLE = 8;

    private static boolean DEBUG = false;

    /** The underlying <code>OutputStream</code>. */
    private LrmcOutputStream out;

    /** The buffer in which output data is collected. */
    private byte[] buffer;

    /** Size of the buffer in which output data is collected. */
    private int index = 0;

    /** Number of bytes written so far to the underlying layer. */
    private long bytes = 0;

    /** Object used for conversion of primitive types to bytes. */
    private Conversion conversion;

    /** Size of the buffer in which output data is collected. */
    private final int BUF_SIZE;

    /**
     * Constructor.
     *
     * @param out   the underlying <code>OutputStream</code>
     * @param bufsz the buffer size.
     */
    public BufferedArrayOutputStream(LrmcOutputStream out, int bufsz) {
        this.out = out;
        this.buffer = out.getBuffer();
        this.BUF_SIZE = bufsz;
        conversion = Conversion.loadConversion(false);
    }

    @Override
    public int bufferSize() {
        return BUF_SIZE;
    }

    @Override
    public long bytesWritten() {
        return bytes + index;
    }

    @Override
    public void resetBytesWritten() {
        bytes = index;
    }

    /**
     * Checks if there is space for <code>incr</code> more bytes and if not, the
     * buffer is written to the underlying <code>OutputStream</code>.
     *
     * @param incr   the space requested
     * @param forced always flushes when set.
     * @exception IOException in case of trouble.
     */
    private void flush(int incr, boolean forced) throws IOException {

        if (DEBUG) {
            System.err.println("flush(" + incr + ") : " + " " + (index + incr >= BUF_SIZE) + " " + (index) + " " + forced + ")");
        }

        if (forced || index + incr > BUF_SIZE) {
            bytes += index;

            // The write will return a new buffer for us which is (at least)
            // the same size as the old one.
            buffer = out.write(0, index, forced);

            // Assume we lost the buffer here
            index = 0;
        }
    }

    @Override
    public void write(int b) throws IOException {
        writeByte((byte) b);
    }

    @Override
    public void writeBoolean(boolean value) throws IOException {
        byte b = conversion.boolean2byte(value);
        flush(1, false);
        buffer[index++] = b;
    }

    @Override
    public void writeByte(byte value) throws IOException {
        flush(1, false);
        buffer[index++] = value;
    }

    @Override
    public void writeChar(char value) throws IOException {
        flush(SIZEOF_CHAR, false);
        conversion.char2byte(value, buffer, index);
        index += SIZEOF_CHAR;
    }

    @Override
    public void writeShort(short value) throws IOException {
        flush(SIZEOF_SHORT, false);
        conversion.short2byte(value, buffer, index);
        index += SIZEOF_SHORT;
    }

    @Override
    public void writeInt(int value) throws IOException {
        flush(SIZEOF_INT, false);
        conversion.int2byte(value, buffer, index);
        index += SIZEOF_INT;
    }

    @Override
    public void writeLong(long value) throws IOException {
        flush(SIZEOF_LONG, false);
        conversion.long2byte(value, buffer, index);
        index += SIZEOF_LONG;
    }

    @Override
    public void writeFloat(float value) throws IOException {
        flush(SIZEOF_FLOAT, false);
        conversion.float2byte(value, buffer, index);
        index += SIZEOF_FLOAT;
    }

    @Override
    public void writeDouble(double value) throws IOException {
        flush(SIZEOF_DOUBLE, false);
        conversion.double2byte(value, buffer, index);
        index += SIZEOF_DOUBLE;
    }

    @Override
    public void write(byte[] b) throws IOException {
        writeArray(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        writeArray(b, off, len);
    }

    @Override
    public void writeArray(boolean[] ref, int off, int len) throws IOException {
        if (DEBUG) {
            System.err.println("writeArray(boolean[" + off + " ... " + (off + len) + "])");
        }

        do {
            flush(1, false);

            int size = Math.min(BUF_SIZE - index, len);

            conversion.boolean2byte(ref, off, size, buffer, index);

            off += size;
            index += size;
            len -= size;

        } while (len != 0);
    }

    @Override
    public void writeArray(byte[] ref, int off, int len) throws IOException {
        if (DEBUG) {
            System.err.println("writeArray(byte[" + off + " ... " + (off + len) + "])");
        }

        while (len > (BUF_SIZE - index)) {

            int space = BUF_SIZE - index;

            // System.err.println(" ______ copying " + space + " bytes");

            System.arraycopy(ref, off, buffer, index, space);

            index += space;
            len -= space;
            off += space;

            // force flush
            flush(BUF_SIZE + 1, false);
        }

        if (len > 0) {

            // System.err.println(" ______* copying " + len + " bytes");

            System.arraycopy(ref, off, buffer, index, len);
            index += len;
        }
    }

    @Override
    public void writeArray(char[] ref, int off, int len) throws IOException {
        if (DEBUG) {
            System.err.println("writeArray(char[" + off + " ... " + (off + len) + "])");
        }

        do {
            flush(SIZEOF_CHAR, false);

            int size = Math.min((BUF_SIZE - index) / SIZEOF_CHAR, len);

            conversion.char2byte(ref, off, size, buffer, index);

            off += size;
            len -= size;
            index += size * SIZEOF_CHAR;

        } while (len != 0);
    }

    @Override
    public void writeArray(short[] ref, int off, int len) throws IOException {
        if (DEBUG) {
            System.err.println("writeArray(short[" + off + " ... " + (off + len) + "])");
        }

        do {
            flush(SIZEOF_SHORT, false);

            int size = Math.min((BUF_SIZE - index) / SIZEOF_SHORT, len);

            // System.err.println("Room to write " + size + " shorts");

            conversion.short2byte(ref, off, size, buffer, index);

            off += size;
            len -= size;
            index += size * SIZEOF_SHORT;

            // System.err.println("Len = " + len + " index = " + index);

        } while (len != 0);
    }

    @Override
    public void writeArray(int[] ref, int off, int len) throws IOException {
        if (DEBUG) {
            System.err.println("writeArray(int[" + off + " ... " + (off + len) + "])");
        }

        do {
            flush(SIZEOF_INT, false);

            int size = Math.min((BUF_SIZE - index) / SIZEOF_INT, len);

            // System.err.println("Room to write " + size + " ints");

            conversion.int2byte(ref, off, size, buffer, index);

            off += size;
            len -= size;
            index += size * SIZEOF_INT;

            // System.err.println("Len = " + len + " index = " + index);

        } while (len != 0);
    }

    @Override
    public void writeArray(long[] ref, int off, int len) throws IOException {
        if (DEBUG) {
            System.err.println("writeArray(long[" + off + " ... " + (off + len) + "])");
        }

        do {
            flush(SIZEOF_LONG, false);

            int size = Math.min((BUF_SIZE - index) / SIZEOF_LONG, len);

            conversion.long2byte(ref, off, size, buffer, index);

            off += size;
            len -= size;
            index += size * SIZEOF_LONG;

        } while (len != 0);
    }

    @Override
    public void writeArray(float[] ref, int off, int len) throws IOException {
        if (DEBUG) {
            System.err.println("writeArray(float[" + off + " ... " + (off + len) + "])");
        }
        do {
            flush(SIZEOF_FLOAT, false);

            int size = Math.min((BUF_SIZE - index) / SIZEOF_FLOAT, len);

            conversion.float2byte(ref, off, size, buffer, index);

            off += size;
            len -= size;
            index += size * SIZEOF_FLOAT;

        } while (len != 0);
    }

    @Override
    public void writeArray(double[] ref, int off, int len) throws IOException {
        if (DEBUG) {
            System.err.println("writeArray(double[" + off + " ... " + (off + len) + "])");
        }

        do {
            flush(SIZEOF_DOUBLE, false);

            int size = Math.min((BUF_SIZE - index) / SIZEOF_DOUBLE, len);

            conversion.double2byte(ref, off, size, buffer, index);

            off += size;
            len -= size;
            index += size * SIZEOF_DOUBLE;

        } while (len != 0);
    }

    @Override
    public void flush() throws IOException {
        // System.err.println("_____ ignoring flush() ");
    }

    public void forcedFlush() throws IOException {

        // System.err.println(" ____ forced flush() ");
        // new Exception().printStackTrace(System.err);

        flush(BUF_SIZE + 1, true); /* Forces flush */
        out.flush();
    }

    @Override
    public void finish() {
        // empty
    }

    @Override
    public boolean finished() {
        return true;
    }

    @Override
    public void close() throws IOException {
        flush();
        out.close();
    }

    @Override
    public void writeByteBuffer(ByteBuffer value) throws IOException {

        int len = value.limit() - value.position();

        while (len > (BUF_SIZE - index)) {

            int space = BUF_SIZE - index;

            // System.err.println(" ______ copying " + space + " bytes");

            value.get(buffer, index, space);

            index += space;
            len -= space;

            // force flush
            flush(BUF_SIZE + 1, false);
        }

        if (len > 0) {

            // System.err.println(" ______* copying " + len + " bytes");

            value.get(buffer, index, len);
            index += len;
        }
    }
}
