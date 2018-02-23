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

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingleBufferArrayInputStream extends DataInputStream {

    private static final boolean DEBUG = IOProperties.DEBUG;

    private static final Logger logger = LoggerFactory
            .getLogger(BufferedArrayInputStream.class);

    /** The buffer size. */
    private final int BUF_SIZE;

    /** The buffer. */
    private final byte[] buffer;

    private int index;

    private int offset = 0;

    /** Object used to convert primitive types to bytes. */
    private Conversion conversion;

    public SingleBufferArrayInputStream(byte[] buffer) {
        this.buffer = buffer;
        this.BUF_SIZE = buffer.length;
        conversion = Conversion.loadConversion(false);
    }

    public long bytesRead() {
        return index - offset;
    }

    public void resetBytesRead() {
        offset = index;
    }

    public final int read() throws IOException {
        try {
            int b = readByte();
            return (b & 0377);
        } catch (EOFException e) {
            return -1;
        }
    }

    public final int size() throws IOException {
        return BUF_SIZE;
    }
    
    public final int available() throws IOException {
        return BUF_SIZE - index;
    }

    private final void checkAvailable(int bytes) throws IOException {
        if (BUF_SIZE - index < bytes) {
            throw new java.io.EOFException("EOF encountered");
        }
    }

    public void readArray(boolean[] a, int off, int len) throws IOException {

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("readArray(boolean[" + off + " ... " + (off + len)
                    + "])");
        }

        final int to_convert = len * Constants.SIZEOF_BOOLEAN;

        checkAvailable(to_convert);

        // enough data in the buffer
        conversion.byte2boolean(buffer, index, a, off, len);
        index += to_convert;
    }

    public void readArray(byte[] a, int off, int len) throws IOException {

        if (DEBUG && logger.isDebugEnabled()) {
            logger
                    .debug("readArray(byte[" + off + " ... " + (off + len)
                            + "])");
        }

        checkAvailable(len);

        // enough data in the buffer
        System.arraycopy(buffer, index, a, off, len);
        index += len;
    }

    public void readArray(short[] a, int off, int len) throws IOException {

        if (DEBUG && logger.isDebugEnabled()) {
            logger
                    .debug("readArray(char[" + off + " ... " + (off + len)
                            + "])");
        }

        final int to_convert = len * Constants.SIZEOF_SHORT;

        checkAvailable(to_convert);

        // enough data in the buffer
        conversion.byte2short(buffer, index, a, off, len);
        index += to_convert;
    }

    public void readArray(char[] a, int off, int len) throws IOException {

        if (DEBUG && logger.isDebugEnabled()) {
            logger
                    .debug("readArray(char[" + off + " ... " + (off + len)
                            + "])");
        }

        final int to_convert = len * Constants.SIZEOF_CHAR;

        checkAvailable(to_convert);

        // enough data in the buffer
        conversion.byte2char(buffer, index, a, off, len);
        index += to_convert;
    }

    public void readArray(int[] a, int off, int len) throws IOException {

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("readArray(int[" + off + " ... " + (off + len) + "])");
        }

        final int to_convert = len * Constants.SIZEOF_INT;

        checkAvailable(to_convert);

        // enough data in the buffer
        conversion.byte2int(buffer, index, a, off, len);
        index += to_convert;
    }

    public void readArray(long[] a, int off, int len) throws IOException {

        if (DEBUG && logger.isDebugEnabled()) {
            logger
                    .debug("readArray(long[" + off + " ... " + (off + len)
                            + "])");
        }

        final int to_convert = len * Constants.SIZEOF_LONG;

        checkAvailable(to_convert);

        // enough data in the buffer
        conversion.byte2long(buffer, index, a, off, len);
        index += to_convert;
    }

    public void readArray(float[] a, int off, int len) throws IOException {

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("readArray(float[" + off + " ... " + (off + len)
                    + "])");
        }

        final int to_convert = len * Constants.SIZEOF_FLOAT;

        checkAvailable(to_convert);

        // enough data in the buffer
        conversion.byte2float(buffer, index, a, off, len);
        index += to_convert;
    }

    public void readArray(double[] a, int off, int len) throws IOException {

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("readArray(double[" + off + " ... " + (off + len)
                    + "])");
        }

        final int to_convert = len * Constants.SIZEOF_DOUBLE;

        checkAvailable(to_convert);

        // enough data in the buffer
        conversion.byte2double(buffer, index, a, off, len);
        index += to_convert;
    }

    public byte readByte() throws IOException {
        checkAvailable(1);
        return buffer[index++];
    }

    public boolean readBoolean() throws IOException {
        checkAvailable(1);
        return conversion.byte2boolean(buffer[index++]);
    }

    public char readChar() throws IOException {
        char v;
        checkAvailable(Constants.SIZEOF_CHAR);
        v = conversion.byte2char(buffer, index);
        index += Constants.SIZEOF_CHAR;
        return v;
    }

    public short readShort() throws IOException {
        short v;
        checkAvailable(Constants.SIZEOF_SHORT);
        v = conversion.byte2short(buffer, index);
        index += Constants.SIZEOF_SHORT;
        return v;
    }

    public int readInt() throws IOException {
        int v;
        checkAvailable(Constants.SIZEOF_INT);
        v = conversion.byte2int(buffer, index);
        index += Constants.SIZEOF_INT;
        return v;
    }

    public long readLong() throws IOException {
        long v;
        checkAvailable(Constants.SIZEOF_LONG);
        v = conversion.byte2long(buffer, index);
        index += Constants.SIZEOF_LONG;
        return v;
    }

    public float readFloat() throws IOException {
        float v;
        checkAvailable(Constants.SIZEOF_FLOAT);
        v = conversion.byte2float(buffer, index);
        index += Constants.SIZEOF_FLOAT;
        return v;
    }

    public double readDouble() throws IOException {
        double v;
        checkAvailable(Constants.SIZEOF_DOUBLE);
        v = conversion.byte2double(buffer, index);
        index += Constants.SIZEOF_DOUBLE;
        return v;
    }

    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public int read(byte[] a, int off, int len) throws IOException {
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("read(byte[" + off + " ... " + (off + len) + "])");
        }

        checkAvailable(len);

        System.arraycopy(buffer, index, a, off, len);
        index += len;
        return len;
    }

    public void close() throws IOException {
        // empty
    }

    public int bufferSize() {
        return BUF_SIZE;
    }

    public void readByteBuffer(ByteBuffer value) throws IOException,
	    ReadOnlyBufferException {
	
	int len = value.limit() - value.position();
	
        checkAvailable(len);
        
        // enough data in the buffer
        value.put(buffer, index, len);
        index += len;	
    }
}
