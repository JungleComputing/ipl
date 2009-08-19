/* $Id$ */

package ibis.io.nio;

import ibis.io.IOProperties;
import ibis.io.SimpleLittleConversion;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

public final class NioWrapLittleConversion extends SimpleLittleConversion {

    public static final int BUFFER_SIZE = IOProperties.CONVERSION_BUFFER_SIZE;

    private ByteOrder order;

    private ByteBuffer byteBuffer;

    private CharBuffer charBuffer;

    private ShortBuffer shortBuffer;

    private IntBuffer intBuffer;

    private LongBuffer longBuffer;

    private FloatBuffer floatBuffer;

    private DoubleBuffer doubleBuffer;

    public NioWrapLittleConversion() {

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

    public void char2byte(char[] src, int off, int len, byte[] dst, int off2) {

        if (len > (BUFFER_SIZE / 2)) {
            CharBuffer buffer = ByteBuffer.wrap(dst, off2, len * 2)
                    .order(order).asCharBuffer();
            buffer.put(src, off, len);
        } else {
            charBuffer.clear();
            charBuffer.put(src, off, len);

            byteBuffer.position(0).limit(len * 2);
            byteBuffer.get(dst, off2, len * 2);
        }
    }

    public void byte2char(byte[] src, int index_src, char[] dst, int index_dst,
            int len) {

        if (len > (BUFFER_SIZE / 2)) {
            CharBuffer buffer = ByteBuffer.wrap(src, index_src, len * 2).order(
                    order).asCharBuffer();
            buffer.get(dst, index_dst, len);
        } else {
            byteBuffer.clear();
            byteBuffer.put(src, index_src, len * 2);

            charBuffer.position(0).limit(len);
            charBuffer.get(dst, index_dst, len);
        }
    }

    public void short2byte(short[] src, int off, int len, byte[] dst,
            int off2) {

        if (len > (BUFFER_SIZE / 2)) {
            ShortBuffer buffer = ByteBuffer.wrap(dst, off2, len * 2).order(
                    order).asShortBuffer();
            buffer.put(src, off, len);
        } else {
            shortBuffer.clear();
            shortBuffer.put(src, off, len);

            byteBuffer.position(0).limit(len * 2);
            byteBuffer.get(dst, off2, len * 2);
        }
    }

    public void byte2short(byte[] src, int index_src, short[] dst,
            int index_dst, int len) {

        if (len > (BUFFER_SIZE / 2)) {
            ShortBuffer buffer = ByteBuffer.wrap(src, index_src, len * 2)
                    .order(order).asShortBuffer();
            buffer.get(dst, index_dst, len);
        } else {
            byteBuffer.clear();
            byteBuffer.put(src, index_src, len * 2);

            shortBuffer.position(0).limit(len);
            shortBuffer.get(dst, index_dst, len);
        }

    }

    public void int2byte(int[] src, int off, int len, byte[] dst, int off2) {

        if (len > (BUFFER_SIZE / 4)) {
            IntBuffer buffer = ByteBuffer.wrap(dst, off2, len * 4).order(order)
                    .asIntBuffer();
            buffer.put(src, off, len);
        } else {
            intBuffer.clear();
            intBuffer.put(src, off, len);

            byteBuffer.position(0).limit(len * 4);
            byteBuffer.get(dst, off2, len * 4);
        }
    }

    public void byte2int(byte[] src, int index_src, int[] dst, int index_dst,
            int len) {

        if (len > (BUFFER_SIZE / 4)) {
            IntBuffer buffer = ByteBuffer.wrap(src, index_src, len * 4).order(
                    order).asIntBuffer();
            buffer.get(dst, index_dst, len);
        } else {
            byteBuffer.clear();
            byteBuffer.put(src, index_src, len * 4);

            intBuffer.position(0).limit(len);
            intBuffer.get(dst, index_dst, len);
        }
    }

    public void long2byte(long[] src, int off, int len, byte[] dst, int off2) {

        if (len > (BUFFER_SIZE / 8)) {
            LongBuffer buffer = ByteBuffer.wrap(dst, off2, len * 8)
                    .order(order).asLongBuffer();
            buffer.put(src, off, len);
        } else {
            longBuffer.clear();
            longBuffer.put(src, off, len);

            byteBuffer.position(0).limit(len * 8);
            byteBuffer.get(dst, off2, len * 8);
        }
    }

    public void byte2long(byte[] src, int index_src, long[] dst, int index_dst,
            int len) {

        if (len > (BUFFER_SIZE / 8)) {
            LongBuffer buffer = ByteBuffer.wrap(src, index_src, len * 8).order(
                    order).asLongBuffer();
            buffer.get(dst, index_dst, len);
        } else {
            byteBuffer.clear();
            byteBuffer.put(src, index_src, len * 8);

            longBuffer.position(0).limit(len);
            longBuffer.get(dst, index_dst, len);
        }
    }

    public void float2byte(float[] src, int off, int len, byte[] dst,
            int off2) {

        if (len > (BUFFER_SIZE / 4)) {
            FloatBuffer buffer = ByteBuffer.wrap(dst, off2, len * 4).order(
                    order).asFloatBuffer();
            buffer.put(src, off, len);
        } else {
            floatBuffer.clear();
            floatBuffer.put(src, off, len);

            byteBuffer.position(0).limit(len * 4);
            byteBuffer.get(dst, off2, len * 4);
        }
    }

    public void byte2float(byte[] src, int index_src, float[] dst,
            int index_dst, int len) {

        if (len > (BUFFER_SIZE / 4)) {
            FloatBuffer buffer = ByteBuffer.wrap(src, index_src, len * 4)
                    .order(order).asFloatBuffer();
            buffer.get(dst, index_dst, len);
        } else {
            byteBuffer.clear();
            byteBuffer.put(src, index_src, len * 4);

            floatBuffer.position(0).limit(len);
            floatBuffer.get(dst, index_dst, len);
        }
    }

    public void double2byte(double[] src, int off, int len, byte[] dst,
            int off2) {

        if (len > (BUFFER_SIZE / 8)) {
            DoubleBuffer buffer = ByteBuffer.wrap(dst, off2, len * 8).order(
                    order).asDoubleBuffer();
            buffer.put(src, off, len);
        } else {
            doubleBuffer.clear();
            doubleBuffer.put(src, off, len);

            byteBuffer.position(0).limit(len * 8);
            byteBuffer.get(dst, off2, len * 8);
        }
    }

    public void byte2double(byte[] src, int index_src, double[] dst,
            int index_dst, int len) {

        if (len > (BUFFER_SIZE / 8)) {
            DoubleBuffer buffer = ByteBuffer.wrap(src, index_src, len * 8)
                    .order(order).asDoubleBuffer();
            buffer.get(dst, index_dst, len);
        } else {
            byteBuffer.clear();
            byteBuffer.put(src, index_src, len * 8);

            doubleBuffer.position(0).limit(len);
            doubleBuffer.get(dst, index_dst, len);
        }
    }
}
