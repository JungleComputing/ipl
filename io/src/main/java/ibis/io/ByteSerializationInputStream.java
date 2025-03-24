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

package ibis.io;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * The <code>ByteSerializationInputStream</code> class can be used when only
 * byte serialization is needed. It provides an implementation for the
 * <code>SerializationInput</code> interface, built on methods in
 * <code>InputStream</code>. However, the only data that can be read are bytes
 * and byte arrays. All other methods throw an exception. It also provides a
 * base class for "data" serialization and "ibis" serialization.
 */
public class ByteSerializationInputStream implements SerializationInput {

    /** The underlying stream. */
    DataInputStream in;

    /**
     * Constructor, may be used when this class is sub-classed.
     *
     * @throws IOException on I/O error
     */
    protected ByteSerializationInputStream() throws IOException {
        in = null;
    }

    /**
     * Constructor.
     *
     * @param s the underlying <code>InputStream</code>
     * @exception IOException is thrown on an IO error.
     */
    public ByteSerializationInputStream(DataInputStream s) throws IOException {
        in = s;
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public String serializationImplName() {
        return "byte";
    }

    @Override
    public boolean reInitOnNewConnection() {
        return false;
    }

    @Override
    public void clear() {
        // Nothing for byte serialization.
    }

    @Override
    public void statistics() {
        // no statistics for byte serialization.
    }

    @Override
    public byte readByte() throws IOException {
        int b = in.read();

        if (b == -1) {
            throw new EOFException("end of file reached");
        }
        return (byte) b;
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return readByte() & 0377;
    }

    @Override
    public boolean readBoolean() throws IOException {
        throw new IOException("Illegal data type read");
    }

    @Override
    public char readChar() throws IOException {
        throw new IOException("Illegal data type read");
    }

    @Override
    public short readShort() throws IOException {
        throw new IOException("Illegal data type read");
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return readShort() & 0177777;
    }

    @Override
    public int readInt() throws IOException {
        throw new IOException("Illegal data type read");
    }

    @Override
    public long readLong() throws IOException {
        throw new IOException("Illegal data type read");
    }

    @Override
    public float readFloat() throws IOException {
        throw new IOException("Illegal data type read");
    }

    @Override
    public double readDouble() throws IOException {
        throw new IOException("Illegal data type read");
    }

    @Override
    public String readString() throws IOException {
        throw new IOException("Illegal data type read");
    }

    @Override
    public Object readObject() throws IOException, ClassNotFoundException {
        throw new IOException("Illegal data type read");
    }

    @Override
    public void readArray(byte[] ref, int off, int len) throws IOException {
        /*
         * Call read() and read() here. It is supported. RFHH
         */
        if (off == 0 && ref.length == len) {
            int rd = 0;
            do {
                rd += in.read(ref, rd, len - rd);
            } while (rd < len);
            return;
        }
        throw new IOException("Illegal data type read");
    }

    @Override
    public void readByteBuffer(ByteBuffer b) throws IOException {
        in.readByteBuffer(b);
    }

    @Override
    public void readArray(boolean[] ref, int off, int len) throws IOException {
        throw new IOException("Illegal data type read");
    }

    @Override
    public void readArray(char[] ref, int off, int len) throws IOException {
        throw new IOException("Illegal data type read");
    }

    @Override
    public void readArray(short[] ref, int off, int len) throws IOException {
        throw new IOException("Illegal data type read");
    }

    @Override
    public void readArray(int[] ref, int off, int len) throws IOException {
        throw new IOException("Illegal data type read");
    }

    @Override
    public void readArray(long[] ref, int off, int len) throws IOException {
        throw new IOException("Illegal data type read");
    }

    @Override
    public void readArray(float[] ref, int off, int len) throws IOException {
        throw new IOException("Illegal data type read");
    }

    @Override
    public void readArray(double[] ref, int off, int len) throws IOException {
        throw new IOException("Illegal data type read");
    }

    @Override
    public void readArray(Object[] ref, int off, int len) throws IOException, ClassNotFoundException {
        throw new IOException("Illegal data type read");
    }

    @Override
    public void readArray(boolean[] ref) throws IOException {
        readArray(ref, 0, ref.length);
    }

    @Override
    public void readArray(byte[] ref) throws IOException {
        readArray(ref, 0, ref.length);
    }

    @Override
    public void readArray(short[] ref) throws IOException {
        readArray(ref, 0, ref.length);
    }

    @Override
    public void readArray(char[] ref) throws IOException {
        readArray(ref, 0, ref.length);
    }

    @Override
    public void readArray(int[] ref) throws IOException {
        readArray(ref, 0, ref.length);
    }

    @Override
    public void readArray(long[] ref) throws IOException {
        readArray(ref, 0, ref.length);
    }

    @Override
    public void readArray(float[] ref) throws IOException {
        readArray(ref, 0, ref.length);
    }

    @Override
    public void readArray(double[] ref) throws IOException {
        readArray(ref, 0, ref.length);
    }

    @Override
    public void readArray(Object[] ref) throws IOException, ClassNotFoundException {
        readArray(ref, 0, ref.length);
    }

    @Override
    public void close() throws IOException {
        // nothing
    }

    @Override
    public void realClose() throws IOException {
        close();
        in.close();
    }

}
