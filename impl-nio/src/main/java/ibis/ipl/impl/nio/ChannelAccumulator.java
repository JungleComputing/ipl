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

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;

import ibis.io.DataOutputStream;

/**
 * Writes data to a channel (using big endian byte order)
 */
public final class ChannelAccumulator extends DataOutputStream {

    public static final int SIZEOF_BYTE = 1;

    public static final int SIZEOF_CHAR = 2;

    public static final int SIZEOF_SHORT = 2;

    public static final int SIZEOF_INT = 4;

    public static final int SIZEOF_LONG = 8;

    public static final int SIZEOF_FLOAT = 4;

    public static final int SIZEOF_DOUBLE = 8;

    public static final int BUFFER_SIZE = 1024;

    long count = 0;

    ByteBuffer buffer;

    WritableByteChannel channel;

    public ChannelAccumulator(WritableByteChannel channel) {
        buffer = ByteBuffer.allocate(BUFFER_SIZE).order(ByteOrder.BIG_ENDIAN);
        this.channel = channel;
    }

    @Override
    public int bufferSize() {
        return BUFFER_SIZE;
    }

    @Override
    public void flush() throws IOException {
        buffer.flip();
        while (buffer.hasRemaining()) {
            count += channel.write(buffer);
        }
        buffer.clear();
    }

    @Override
    public void close() throws IOException {
        flush();
    }

    public void reallyClose() throws IOException {
        flush();
        channel.close();
    }

    @Override
    public long bytesWritten() {
        return count;
    }

    @Override
    public void resetBytesWritten() {
        count = 0;
    }

    @Override
    public void writeBoolean(boolean value) throws IOException {
        if (value) {
            writeByte((byte) 1);
        } else {
            writeByte((byte) 0);
        }
    }

    @Override
    public void writeByte(byte value) throws IOException {
        try {
            buffer.put(value);
        } catch (BufferOverflowException e) {
            flush();
            buffer.put(value);
        }
    }

    @Override
    public void write(int b) throws IOException {
        writeByte((byte) b);
    }

    @Override
    public void writeChar(char value) throws IOException {
        try {
            buffer.putChar(value);
        } catch (BufferOverflowException e) {
            flush();
            buffer.putChar(value);
        }
    }

    @Override
    public void writeShort(short value) throws IOException {
        try {
            buffer.putShort(value);
        } catch (BufferOverflowException e) {
            flush();
            buffer.putShort(value);
        }
    }

    @Override
    public void writeInt(int value) throws IOException {
        try {
            buffer.putInt(value);
        } catch (BufferOverflowException e) {
            flush();
            buffer.putInt(value);
        }
    }

    @Override
    public void writeLong(long value) throws IOException {
        try {
            buffer.putLong(value);
        } catch (BufferOverflowException e) {
            flush();
            buffer.putLong(value);
        }
    }

    @Override
    public void writeFloat(float value) throws IOException {
        try {
            buffer.putFloat(value);
        } catch (BufferOverflowException e) {
            flush();
            buffer.putFloat(value);
        }
    }

    @Override
    public void writeDouble(double value) throws IOException {
        try {
            buffer.putDouble(value);
        } catch (BufferOverflowException e) {
            flush();
            buffer.putDouble(value);
        }
    }

    @Override
    public void writeArray(boolean[] source, int offset, int length) throws IOException {
        for (int i = offset; i < (offset + length); i++) {
            writeBoolean(source[i]);
        }
    }

    @Override
    public void writeArray(byte[] source, int offset, int length) throws IOException {
        for (int i = offset; i < (offset + length); i++) {
            writeByte(source[i]);
        }
    }

    @Override
    public void writeArray(char[] source, int offset, int length) throws IOException {
        for (int i = offset; i < (offset + length); i++) {
            writeChar(source[i]);
        }
    }

    @Override
    public void writeArray(short[] source, int offset, int length) throws IOException {
        for (int i = offset; i < (offset + length); i++) {
            writeShort(source[i]);
        }
    }

    @Override
    public void writeArray(int[] source, int offset, int length) throws IOException {
        for (int i = offset; i < (offset + length); i++) {
            writeInt(source[i]);
        }
    }

    @Override
    public void writeArray(long[] source, int offset, int length) throws IOException {
        for (int i = offset; i < (offset + length); i++) {
            writeLong(source[i]);
        }
    }

    @Override
    public void writeArray(float[] source, int offset, int length) throws IOException {
        for (int i = offset; i < (offset + length); i++) {
            writeFloat(source[i]);
        }
    }

    @Override
    public void writeArray(double[] source, int offset, int length) throws IOException {
        for (int i = offset; i < (offset + length); i++) {
            writeDouble(source[i]);
        }
    }

    @Override
    public void writeByteBuffer(ByteBuffer value) throws IOException {
        for (int i = value.position(); i < value.limit(); i++) {
            writeByte(value.get());
        }
    }
}
