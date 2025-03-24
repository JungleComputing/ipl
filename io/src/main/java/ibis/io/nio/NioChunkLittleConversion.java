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

package ibis.io.nio;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import ibis.io.IOProperties;
import ibis.io.SimpleLittleConversion;

public final class NioChunkLittleConversion extends SimpleLittleConversion {

    public static final int BUFFER_SIZE = IOProperties.CONVERSION_BUFFER_SIZE;

    private ByteOrder order;

    private ByteBuffer byteBuffer;

    private CharBuffer charBuffer;

    private ShortBuffer shortBuffer;

    private IntBuffer intBuffer;

    private LongBuffer longBuffer;

    private FloatBuffer floatBuffer;

    private DoubleBuffer doubleBuffer;

    public NioChunkLittleConversion() {

        // big/little endian difference one liner
        order = ByteOrder.LITTLE_ENDIAN;

        byteBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE).order(order);

        // views on the bytebuffer to fill/drain it efficiently
        charBuffer = byteBuffer.asCharBuffer();
        shortBuffer = byteBuffer.asShortBuffer();
        intBuffer = byteBuffer.asIntBuffer();
        longBuffer = byteBuffer.asLongBuffer();
        floatBuffer = byteBuffer.asFloatBuffer();
        doubleBuffer = byteBuffer.asDoubleBuffer();
    }

    @Override
    public void char2byte(char[] src, int off, int len, byte[] dst, int off2) {

        while (len > 0) {
            int chunkSize = Math.min(BUFFER_SIZE / CHAR_SIZE, len);

            charBuffer.clear();
            charBuffer.put(src, off, chunkSize);

            byteBuffer.position(0).limit(chunkSize * CHAR_SIZE);
            byteBuffer.get(dst, off2, chunkSize * CHAR_SIZE);

            len -= chunkSize;
            off += chunkSize;
            off2 += chunkSize * CHAR_SIZE;
        }
    }

    @Override
    public void byte2char(byte[] src, int index_src, char[] dst, int index_dst, int len) {

        while (len > 0) {
            int chunkSize = Math.min(BUFFER_SIZE / CHAR_SIZE, len);

            byteBuffer.clear();
            byteBuffer.put(src, index_src, chunkSize * CHAR_SIZE);

            charBuffer.position(0).limit(chunkSize);
            charBuffer.get(dst, index_dst, chunkSize);

            len -= chunkSize;
            index_src += chunkSize * CHAR_SIZE;
            index_dst += chunkSize;
        }
    }

    @Override
    public void short2byte(short[] src, int off, int len, byte[] dst, int off2) {

        while (len > 0) {
            int chunkSize = Math.min(BUFFER_SIZE / SHORT_SIZE, len);

            shortBuffer.clear();
            shortBuffer.put(src, off, chunkSize);

            byteBuffer.position(0).limit(chunkSize * SHORT_SIZE);
            byteBuffer.get(dst, off2, chunkSize * SHORT_SIZE);

            len -= chunkSize;
            off += chunkSize;
            off2 += chunkSize * SHORT_SIZE;
        }
    }

    @Override
    public void byte2short(byte[] src, int index_src, short[] dst, int index_dst, int len) {

        while (len > 0) {
            int chunkSize = Math.min(BUFFER_SIZE / SHORT_SIZE, len);

            byteBuffer.clear();
            byteBuffer.put(src, index_src, chunkSize * SHORT_SIZE);

            shortBuffer.position(0).limit(chunkSize);
            shortBuffer.get(dst, index_dst, chunkSize);

            len -= chunkSize;
            index_src += chunkSize * SHORT_SIZE;
            index_dst += chunkSize;
        }
    }

    @Override
    public void int2byte(int[] src, int off, int len, byte[] dst, int off2) {

        while (len > 0) {
            int chunkSize = Math.min(BUFFER_SIZE / INT_SIZE, len);

            intBuffer.clear();
            intBuffer.put(src, off, chunkSize);

            byteBuffer.position(0).limit(chunkSize * INT_SIZE);
            byteBuffer.get(dst, off2, chunkSize * INT_SIZE);

            len -= chunkSize;
            off += chunkSize;
            off2 += chunkSize * INT_SIZE;
        }
    }

    @Override
    public void byte2int(byte[] src, int index_src, int[] dst, int index_dst, int len) {
        while (len > 0) {
            int chunkSize = Math.min(BUFFER_SIZE / INT_SIZE, len);

            byteBuffer.clear();
            byteBuffer.put(src, index_src, chunkSize * INT_SIZE);

            intBuffer.position(0).limit(chunkSize);
            intBuffer.get(dst, index_dst, chunkSize);

            len -= chunkSize;
            index_src += chunkSize * INT_SIZE;
            index_dst += chunkSize;
        }

    }

    @Override
    public void long2byte(long[] src, int off, int len, byte[] dst, int off2) {
        while (len > 0) {
            int chunkSize = Math.min(BUFFER_SIZE / LONG_SIZE, len);

            longBuffer.clear();
            longBuffer.put(src, off, chunkSize);

            byteBuffer.position(0).limit(chunkSize * LONG_SIZE);
            byteBuffer.get(dst, off2, chunkSize * LONG_SIZE);

            len -= chunkSize;
            off += chunkSize;
            off2 += chunkSize * LONG_SIZE;
        }

    }

    @Override
    public void byte2long(byte[] src, int index_src, long[] dst, int index_dst, int len) {

        while (len > 0) {
            int chunkSize = Math.min(BUFFER_SIZE / LONG_SIZE, len);

            byteBuffer.clear();
            byteBuffer.put(src, index_src, chunkSize * LONG_SIZE);

            longBuffer.position(0).limit(chunkSize);
            longBuffer.get(dst, index_dst, chunkSize);

            len -= chunkSize;
            index_src += chunkSize * LONG_SIZE;
            index_dst += chunkSize;
        }
    }

    @Override
    public void float2byte(float[] src, int off, int len, byte[] dst, int off2) {
        while (len > 0) {
            int chunkSize = Math.min(BUFFER_SIZE / FLOAT_SIZE, len);

            floatBuffer.clear();
            floatBuffer.put(src, off, chunkSize);

            byteBuffer.position(0).limit(chunkSize * FLOAT_SIZE);
            byteBuffer.get(dst, off2, chunkSize * FLOAT_SIZE);

            len -= chunkSize;
            off += chunkSize;
            off2 += chunkSize * FLOAT_SIZE;
        }

    }

    @Override
    public void byte2float(byte[] src, int index_src, float[] dst, int index_dst, int len) {

        while (len > 0) {
            int chunkSize = Math.min(BUFFER_SIZE / FLOAT_SIZE, len);

            byteBuffer.clear();
            byteBuffer.put(src, index_src, chunkSize * FLOAT_SIZE);

            floatBuffer.position(0).limit(chunkSize);
            floatBuffer.get(dst, index_dst, chunkSize);

            len -= chunkSize;
            index_src += chunkSize * FLOAT_SIZE;
            index_dst += chunkSize;
        }
    }

    @Override
    public void double2byte(double[] src, int off, int len, byte[] dst, int off2) {
        while (len > 0) {
            int chunkSize = Math.min(BUFFER_SIZE / DOUBLE_SIZE, len);

            doubleBuffer.clear();
            doubleBuffer.put(src, off, chunkSize);

            byteBuffer.position(0).limit(chunkSize * DOUBLE_SIZE);
            byteBuffer.get(dst, off2, chunkSize * DOUBLE_SIZE);

            len -= chunkSize;
            off += chunkSize;
            off2 += chunkSize * DOUBLE_SIZE;
        }
    }

    @Override
    public void byte2double(byte[] src, int index_src, double[] dst, int index_dst, int len) {

        while (len > 0) {
            int chunkSize = Math.min(BUFFER_SIZE / DOUBLE_SIZE, len);

            byteBuffer.clear();
            byteBuffer.put(src, index_src, chunkSize * DOUBLE_SIZE);

            doubleBuffer.position(0).limit(chunkSize);
            doubleBuffer.get(dst, index_dst, chunkSize);

            len -= chunkSize;
            index_src += chunkSize * DOUBLE_SIZE;
            index_dst += chunkSize;
        }
    }
}
