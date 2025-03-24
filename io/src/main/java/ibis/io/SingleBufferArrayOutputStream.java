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
package ibis.io;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingleBufferArrayOutputStream extends DataOutputStream {

    private static final Logger logger = LoggerFactory.getLogger(BufferedArrayOutputStream.class);

    private static final boolean DEBUG = IOProperties.DEBUG;

    /** Size of the buffer in which output data is collected. */
    private final int BUF_SIZE;

    /** The buffer in which output data is collected. */
    private byte[] buffer;

    /** Size of the buffer in which output data is collected. */
    private int index = 0;

    private int offset = 0;

    /** Object used for conversion of primitive types to bytes. */
    private Conversion conversion;

    /**
     * Constructor.
     *
     * @param buffer the underlying byte buffer
     */
    public SingleBufferArrayOutputStream(byte[] buffer) {
        this.buffer = buffer;
        BUF_SIZE = buffer.length;
        conversion = Conversion.loadConversion(false);
    }

    public void reset() {
        index = 0;
    }

    @Override
    public long bytesWritten() {
        return index - offset;
    }

    @Override
    public void resetBytesWritten() {
        offset = index;
    }

    /**
     * Checks if there is space for <code>incr</code> more bytes and if not, the
     * buffer is written to the underlying <code>OutputStream</code>.
     *
     * @param incr the space requested
     * @exception IOException in case of trouble.
     */
    private void checkFreeSpace(int bytes) throws IOException {

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("checkFreeSpace(" + bytes + ") : " + " " + (index + bytes >= BUF_SIZE) + " " + (index) + ")");
        }

        if (index + bytes > BUF_SIZE) {
            throw new IOException("End of buffer reached (" + index + "+" + bytes + " > " + BUF_SIZE + ")");
        }
    }

    @Override
    public void write(int b) throws IOException {
        writeByte((byte) b);
    }

    @Override
    public void writeBoolean(boolean value) throws IOException {
        byte b = conversion.boolean2byte(value);
        checkFreeSpace(1);
        buffer[index++] = b;
    }

    @Override
    public void writeByte(byte value) throws IOException {
        checkFreeSpace(1);
        buffer[index++] = value;
    }

    @Override
    public void writeChar(char value) throws IOException {
        checkFreeSpace(Constants.SIZEOF_CHAR);
        conversion.char2byte(value, buffer, index);
        index += Constants.SIZEOF_CHAR;
    }

    @Override
    public void writeShort(short value) throws IOException {
        checkFreeSpace(Constants.SIZEOF_SHORT);
        conversion.short2byte(value, buffer, index);
        index += Constants.SIZEOF_SHORT;
    }

    @Override
    public void writeInt(int value) throws IOException {
        checkFreeSpace(Constants.SIZEOF_INT);
        conversion.int2byte(value, buffer, index);
        index += Constants.SIZEOF_INT;
    }

    @Override
    public void writeLong(long value) throws IOException {
        checkFreeSpace(Constants.SIZEOF_LONG);
        conversion.long2byte(value, buffer, index);
        index += Constants.SIZEOF_LONG;
    }

    @Override
    public void writeFloat(float value) throws IOException {
        checkFreeSpace(Constants.SIZEOF_FLOAT);
        conversion.float2byte(value, buffer, index);
        index += Constants.SIZEOF_FLOAT;
    }

    @Override
    public void writeDouble(double value) throws IOException {
        checkFreeSpace(Constants.SIZEOF_DOUBLE);
        conversion.double2byte(value, buffer, index);
        index += Constants.SIZEOF_DOUBLE;
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
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("writeArray(boolean[" + off + " ... " + (off + len) + "])");
        }

        final int toWrite = len * Constants.SIZEOF_BOOLEAN;

        checkFreeSpace(toWrite);
        conversion.boolean2byte(ref, off, len, buffer, index);
        index += toWrite;
    }

    @Override
    public void writeArray(byte[] ref, int off, int len) throws IOException {

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("writeArray(byte[" + off + " ... " + (off + len) + "])");
        }

        checkFreeSpace(len);
        System.arraycopy(ref, off, buffer, index, len);
        index += len;
    }

    @Override
    public void writeArray(char[] ref, int off, int len) throws IOException {

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("writeArray(char[" + off + " ... " + (off + len) + "])");
        }

        final int toWrite = len * Constants.SIZEOF_CHAR;
        checkFreeSpace(toWrite);
        conversion.char2byte(ref, off, len, buffer, index);
        index += toWrite;
    }

    @Override
    public void writeArray(short[] ref, int off, int len) throws IOException {

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("writeArray(short[" + off + " ... " + (off + len) + "])");
        }

        final int toWrite = len * Constants.SIZEOF_SHORT;
        checkFreeSpace(toWrite);
        conversion.short2byte(ref, off, len, buffer, index);
        index += toWrite;
    }

    @Override
    public void writeArray(int[] ref, int off, int len) throws IOException {

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("writeArray(int[" + off + " ... " + (off + len) + "])");
        }

        final int toWrite = len * Conversion.INT_SIZE;
        checkFreeSpace(toWrite);
        conversion.int2byte(ref, off, len, buffer, index);
        index += toWrite;
    }

    @Override
    public void writeArray(long[] ref, int off, int len) throws IOException {

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("writeArray(long[" + off + " ... " + (off + len) + "])");
        }

        final int toWrite = len * Conversion.INT_SIZE;
        checkFreeSpace(toWrite);
        conversion.long2byte(ref, off, len, buffer, index);
        index += toWrite;
    }

    @Override
    public void writeArray(float[] ref, int off, int len) throws IOException {

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("writeArray(float[" + off + " ... " + (off + len) + "])");
        }

        final int toWrite = len * Conversion.FLOAT_SIZE;
        checkFreeSpace(toWrite);
        conversion.float2byte(ref, off, len, buffer, index);
        index += toWrite;
    }

    @Override
    public void writeArray(double[] ref, int off, int len) throws IOException {

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("writeArray(double[" + off + " ... " + (off + len) + "])");
        }

        final int toWrite = len * Conversion.FLOAT_SIZE;
        checkFreeSpace(toWrite);
        conversion.double2byte(ref, off, len, buffer, index);
        index += toWrite;
    }

    @Override
    public void flush() throws IOException {
        // empty
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
        // empty
    }

    @Override
    public int bufferSize() {
        return BUF_SIZE;
    }

    @Override
    public void writeByteBuffer(ByteBuffer value) throws IOException {

        int len = value.limit() - value.position();

        checkFreeSpace(len);

        value.get(buffer, index, len);
        index += len;
    }
}
