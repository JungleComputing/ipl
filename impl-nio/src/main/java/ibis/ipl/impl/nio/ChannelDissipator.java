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

package ibis.ipl.impl.nio;

import java.io.EOFException;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ReadOnlyBufferException;
import java.nio.channels.ReadableByteChannel;

import ibis.io.DataInputStream;

/**
 * Reads data from a channel.
 */
public final class ChannelDissipator extends DataInputStream {
    public static final int SIZEOF_BYTE = 1;

    public static final int SIZEOF_CHAR = 2;

    public static final int SIZEOF_SHORT = 2;

    public static final int SIZEOF_INT = 4;

    public static final int SIZEOF_LONG = 8;

    public static final int SIZEOF_FLOAT = 4;

    public static final int SIZEOF_DOUBLE = 8;

    static final int BUFFER_SIZE = 1024;

    private ReadableByteChannel channel;

    private ByteBuffer buffer;

    private long count = 0;

    public ChannelDissipator(ReadableByteChannel channel) {
        buffer = ByteBuffer.allocate(BUFFER_SIZE).order(ByteOrder.BIG_ENDIAN);
        // make the buffer apear empty
        buffer.limit(0);
        this.channel = channel;
    }

    @Override
    public int bufferSize() {
        return BUFFER_SIZE;
    }

    @Override
    public int available() throws IOException {
        return buffer.remaining();
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    @Override
    public long bytesRead() {
        return count;
    }

    @Override
    public void resetBytesRead() {
        count = 0;
    }

    private void readAtLeast(int minimum) throws IOException {
        int count;
        buffer.compact();

        while (buffer.position() < minimum) {
            count = channel.read(buffer);
            if (count == -1) {
                throw new IOException("eos read on reading from channel");
            }
            this.count += count;
        }
        buffer.flip();
    }

    @Override
    public boolean readBoolean() throws IOException {
        return (readByte() == ((byte) 1));
    }

    @Override
    public byte readByte() throws IOException {
        try {
            return buffer.get();
        } catch (BufferUnderflowException e) {
            readAtLeast(SIZEOF_BYTE);
            return buffer.get();
        }
    }

    @Override
    public int read() throws IOException {
        try {
            return readByte() & 0377;
        } catch (EOFException e) {
            return -1;
        }
    }

    @Override
    public char readChar() throws IOException {
        try {
            return buffer.getChar();
        } catch (BufferUnderflowException e) {
            readAtLeast(SIZEOF_CHAR);
            return buffer.getChar();
        }
    }

    @Override
    public short readShort() throws IOException {
        try {
            return buffer.getShort();
        } catch (BufferUnderflowException e) {
            readAtLeast(SIZEOF_SHORT);
            return buffer.getShort();
        }
    }

    @Override
    public int readInt() throws IOException {
        try {
            return buffer.getInt();
        } catch (BufferUnderflowException e) {
            readAtLeast(SIZEOF_INT);
            return buffer.getInt();
        }
    }

    @Override
    public long readLong() throws IOException {
        try {
            return buffer.getLong();
        } catch (BufferUnderflowException e) {
            readAtLeast(SIZEOF_LONG);
            return buffer.getLong();
        }
    }

    @Override
    public float readFloat() throws IOException {
        try {
            return buffer.getFloat();
        } catch (BufferUnderflowException e) {
            readAtLeast(SIZEOF_FLOAT);
            return buffer.getFloat();
        }
    }

    @Override
    public double readDouble() throws IOException {
        try {
            return buffer.getDouble();
        } catch (BufferUnderflowException e) {
            readAtLeast(SIZEOF_DOUBLE);
            return buffer.getDouble();
        }
    }

    @Override
    public void readArray(boolean[] destination, int offset, int length) throws IOException {
        for (int i = offset; i < (offset + length); i++) {
            destination[i] = readBoolean();
        }
    }

    @Override
    public void readArray(byte[] destination, int offset, int length) throws IOException {
        for (int i = offset; i < (offset + length); i++) {
            destination[i] = readByte();
        }
    }

    @Override
    public void readArray(char[] destination, int offset, int length) throws IOException {
        for (int i = offset; i < (offset + length); i++) {
            destination[i] = readChar();
        }
    }

    @Override
    public void readArray(short[] destination, int offset, int length) throws IOException {
        for (int i = offset; i < (offset + length); i++) {
            destination[i] = readShort();
        }
    }

    @Override
    public void readArray(int[] destination, int offset, int length) throws IOException {
        for (int i = offset; i < (offset + length); i++) {
            destination[i] = readInt();
        }
    }

    @Override
    public void readArray(long[] destination, int offset, int length) throws IOException {
        for (int i = offset; i < (offset + length); i++) {
            destination[i] = readLong();
        }
    }

    @Override
    public void readArray(float[] destination, int offset, int length) throws IOException {
        for (int i = offset; i < (offset + length); i++) {
            destination[i] = readFloat();
        }
    }

    @Override
    public void readArray(double[] destination, int offset, int length) throws IOException {
        for (int i = offset; i < (offset + length); i++) {
            destination[i] = readDouble();
        }
    }

    @Override
    public void readByteBuffer(ByteBuffer value) throws IOException, ReadOnlyBufferException {
        for (int i = value.position(); i < value.limit(); i++) {
            value.put(readByte());
        }
    }
}
