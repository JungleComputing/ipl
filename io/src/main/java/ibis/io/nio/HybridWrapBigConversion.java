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
import ibis.io.SimpleBigConversion;

public final class HybridWrapBigConversion extends SimpleBigConversion {

    public static final int BUFFER_SIZE = IOProperties.CONVERSION_BUFFER_SIZE;

    public static final int THRESHOLD = 512; // bytes

    public static final int FP_THRESHOLD = 64; // bytes

    private final ByteOrder order;

    private final ByteBuffer byteBuffer;

    private final CharBuffer charBuffer;

    private final ShortBuffer shortBuffer;

    private final IntBuffer intBuffer;

    private final LongBuffer longBuffer;

    private final FloatBuffer floatBuffer;

    private final DoubleBuffer doubleBuffer;

    private byte[] buf;

    private ByteBuffer bufBuffer;

    public HybridWrapBigConversion() {
        // big/little endian difference one liner
        order = ByteOrder.BIG_ENDIAN;

        byteBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE).order(order);

        // views on the bytebuffer to fill/drain it efficiently
        charBuffer = byteBuffer.asCharBuffer();
        shortBuffer = byteBuffer.asShortBuffer();
        intBuffer = byteBuffer.asIntBuffer();
        longBuffer = byteBuffer.asLongBuffer();
        floatBuffer = byteBuffer.asFloatBuffer();
        doubleBuffer = byteBuffer.asDoubleBuffer();
    }

    private void checkBuf(byte[] buf, int position) {
        if (this.buf != buf) {
            bufBuffer = ByteBuffer.wrap(buf).order(order);
            this.buf = buf;
        }
        bufBuffer.position(position);
    }

    @Override
    public void char2byte(char[] src, int off, int len, byte[] dst, int off2) {

        if (len < THRESHOLD / CHAR_SIZE) {
            super.char2byte(src, off, len, dst, off2);
        } else if (len > (BUFFER_SIZE / CHAR_SIZE)) {
            checkBuf(dst, off2);
            CharBuffer buffer = bufBuffer.asCharBuffer();
            buffer.put(src, off, len);
        } else {
            charBuffer.clear();
            charBuffer.put(src, off, len);

            byteBuffer.position(0).limit(len * CHAR_SIZE);
            byteBuffer.get(dst, off2, len * CHAR_SIZE);
        }
    }

    @Override
    public void byte2char(byte[] src, int index_src, char[] dst, int index_dst, int len) {

        if (len < THRESHOLD / CHAR_SIZE) {
            super.byte2char(src, index_src, dst, index_dst, len);
        } else if (len > (BUFFER_SIZE / CHAR_SIZE)) {
            checkBuf(src, index_src);
            CharBuffer buffer = bufBuffer.asCharBuffer();
            buffer.get(dst, index_dst, len);
        } else {
            byteBuffer.clear();
            byteBuffer.put(src, index_src, len * CHAR_SIZE);

            charBuffer.position(0).limit(len);
            charBuffer.get(dst, index_dst, len);
        }
    }

    @Override
    public void short2byte(short[] src, int off, int len, byte[] dst, int off2) {

        if (len < THRESHOLD / SHORT_SIZE) {
            super.short2byte(src, off, len, dst, off2);
        } else if (len > (BUFFER_SIZE / SHORT_SIZE)) {
            ShortBuffer buffer = ByteBuffer.wrap(dst, off2, len * SHORT_SIZE).order(order).asShortBuffer();
            buffer.put(src, off, len);
        } else {
            shortBuffer.clear();
            shortBuffer.put(src, off, len);

            byteBuffer.position(0).limit(len * SHORT_SIZE);
            byteBuffer.get(dst, off2, len * SHORT_SIZE);
        }
    }

    @Override
    public void byte2short(byte[] src, int index_src, short[] dst, int index_dst, int len) {

        if (len < THRESHOLD / SHORT_SIZE) {
            super.byte2short(src, index_src, dst, index_dst, len);
        } else if (len > (BUFFER_SIZE / SHORT_SIZE)) {
            ShortBuffer buffer = ByteBuffer.wrap(src, index_src, len * SHORT_SIZE).order(order).asShortBuffer();
            buffer.get(dst, index_dst, len);
        } else {
            byteBuffer.clear();
            byteBuffer.put(src, index_src, len * SHORT_SIZE);

            shortBuffer.position(0).limit(len);
            shortBuffer.get(dst, index_dst, len);
        }

    }

    @Override
    public void int2byte(int[] src, int off, int len, byte[] dst, int off2) {

        if (len < THRESHOLD / INT_SIZE) {
            super.int2byte(src, off, len, dst, off2);
        } else if (len > (BUFFER_SIZE / INT_SIZE)) {
            IntBuffer buffer = ByteBuffer.wrap(dst, off2, len * INT_SIZE).order(order).asIntBuffer();
            buffer.put(src, off, len);
        } else {
            intBuffer.clear();
            intBuffer.put(src, off, len);

            byteBuffer.position(0).limit(len * INT_SIZE);
            byteBuffer.get(dst, off2, len * INT_SIZE);
        }
    }

    @Override
    public void byte2int(byte[] src, int index_src, int[] dst, int index_dst, int len) {

        if (len < THRESHOLD / INT_SIZE) {
            super.byte2int(src, index_src, dst, index_dst, len);
        } else if (len > (BUFFER_SIZE / INT_SIZE)) {
            IntBuffer buffer = ByteBuffer.wrap(src, index_src, len * INT_SIZE).order(order).asIntBuffer();
            buffer.get(dst, index_dst, len);
        } else {
            byteBuffer.clear();
            byteBuffer.put(src, index_src, len * INT_SIZE);

            intBuffer.position(0).limit(len);
            intBuffer.get(dst, index_dst, len);
        }
    }

    @Override
    public void long2byte(long[] src, int off, int len, byte[] dst, int off2) {

        if (len < THRESHOLD / LONG_SIZE) {
            super.long2byte(src, off, len, dst, off2);
        } else if (len > (BUFFER_SIZE / LONG_SIZE)) {
            LongBuffer buffer = ByteBuffer.wrap(dst, off2, len * LONG_SIZE).order(order).asLongBuffer();
            buffer.put(src, off, len);
        } else {
            longBuffer.clear();
            longBuffer.put(src, off, len);

            byteBuffer.position(0).limit(len * LONG_SIZE);
            byteBuffer.get(dst, off2, len * LONG_SIZE);
        }
    }

    @Override
    public void byte2long(byte[] src, int index_src, long[] dst, int index_dst, int len) {

        if (len < THRESHOLD / LONG_SIZE) {
            super.byte2long(src, index_src, dst, index_dst, len);
        } else if (len > (BUFFER_SIZE / LONG_SIZE)) {
            LongBuffer buffer = ByteBuffer.wrap(src, index_src, len * LONG_SIZE).order(order).asLongBuffer();
            buffer.get(dst, index_dst, len);
        } else {
            byteBuffer.clear();
            byteBuffer.put(src, index_src, len * LONG_SIZE);

            longBuffer.position(0).limit(len);
            longBuffer.get(dst, index_dst, len);
        }
    }

    @Override
    public void float2byte(float[] src, int off, int len, byte[] dst, int off2) {

        if (len < FP_THRESHOLD / FLOAT_SIZE) {
            super.float2byte(src, off, len, dst, off2);
        } else if (len > (BUFFER_SIZE / FLOAT_SIZE)) {
            checkBuf(dst, off2);
            FloatBuffer buffer = bufBuffer.asFloatBuffer();
            buffer.put(src, off, len);
        } else {
            floatBuffer.clear();
            floatBuffer.put(src, off, len);

            byteBuffer.position(0).limit(len * FLOAT_SIZE);
            byteBuffer.get(dst, off2, len * FLOAT_SIZE);
        }
    }

    @Override
    public void byte2float(byte[] src, int index_src, float[] dst, int index_dst, int len) {

        if (len < FP_THRESHOLD / FLOAT_SIZE) {
            super.byte2float(src, index_src, dst, index_dst, len);
        } else if (len > (BUFFER_SIZE / FLOAT_SIZE)) {
            checkBuf(src, index_src);
            FloatBuffer buffer = bufBuffer.asFloatBuffer();
            buffer.get(dst, index_dst, len);
        } else {
            byteBuffer.clear();
            byteBuffer.put(src, index_src, len * FLOAT_SIZE);

            floatBuffer.position(0).limit(len);
            floatBuffer.get(dst, index_dst, len);
        }
    }

    @Override
    public void double2byte(double[] src, int off, int len, byte[] dst, int off2) {

        if (len < FP_THRESHOLD / DOUBLE_SIZE) {
            super.double2byte(src, off, len, dst, off2);
        } else if (len > (BUFFER_SIZE / DOUBLE_SIZE)) {
            DoubleBuffer buffer = ByteBuffer.wrap(dst, off2, len * DOUBLE_SIZE).order(order).asDoubleBuffer();
            buffer.put(src, off, len);
        } else {
            doubleBuffer.clear();
            doubleBuffer.put(src, off, len);

            byteBuffer.position(0).limit(len * DOUBLE_SIZE);
            byteBuffer.get(dst, off2, len * DOUBLE_SIZE);
        }
    }

    @Override
    public void byte2double(byte[] src, int index_src, double[] dst, int index_dst, int len) {

        if (len < FP_THRESHOLD / DOUBLE_SIZE) {
            super.byte2double(src, index_src, dst, index_dst, len);
        } else if (len > (BUFFER_SIZE / DOUBLE_SIZE)) {
            DoubleBuffer buffer = ByteBuffer.wrap(src, index_src, len * DOUBLE_SIZE).order(order).asDoubleBuffer();
            buffer.get(dst, index_dst, len);
        } else {
            byteBuffer.clear();
            byteBuffer.put(src, index_src, len * DOUBLE_SIZE);

            doubleBuffer.position(0).limit(len);
            doubleBuffer.get(dst, index_dst, len);
        }
    }
}
