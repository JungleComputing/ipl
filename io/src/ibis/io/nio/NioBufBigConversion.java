/* $Id:$ */

package ibis.io.nio;

import ibis.io.IOProperties;
import ibis.io.SimpleBigConversion;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

public final class NioBufBigConversion extends SimpleBigConversion {

    private ByteOrder order;

    private ByteBuffer byteBuffer;

    private byte[] buf;

    public NioBufBigConversion() {

        // big/little endian difference one liner
        order = ByteOrder.BIG_ENDIAN;
    }

    private void checkBuf(byte[] buf) {
        if (this.buf != buf) {
            byteBuffer = ByteBuffer.wrap(buf).order(order);
        }
    }

    public void char2byte(char[] src, int off, int len, byte[] dst, int off2) {
        checkBuf(buf);
        byteBuffer.position(off2);
        byteBuffer.asCharBuffer().put(src, off, len);
    }

    public void byte2char(byte[] src, int index_src, char[] dst, int index_dst,
            int len) {
        checkBuf(buf);
        byteBuffer.position(index_src);
        byteBuffer.asCharBuffer().get(dst, index_dst, len);
    }

    public void short2byte(short[] src, int off, int len, byte[] dst,
            int off2) {
        checkBuf(buf);
        byteBuffer.position(off2);
        byteBuffer.asShortBuffer().put(src, off, len);
    }

    public void byte2short(byte[] src, int index_src, short[] dst,
            int index_dst, int len) {
        checkBuf(buf);
        byteBuffer.position(index_src);
        byteBuffer.asShortBuffer().get(dst, index_dst, len);
    }

    public void int2byte(int[] src, int off, int len, byte[] dst,
            int off2) {
        checkBuf(buf);
        byteBuffer.position(off2);
        byteBuffer.asIntBuffer().put(src, off, len);
    }

    public void byte2int(byte[] src, int index_src, int[] dst,
            int index_dst, int len) {
        checkBuf(buf);
        byteBuffer.position(index_src);
        byteBuffer.asIntBuffer().get(dst, index_dst, len);
    }

    public void long2byte(long[] src, int off, int len, byte[] dst,
            int off2) {
        checkBuf(buf);
        byteBuffer.position(off2);
        byteBuffer.asLongBuffer().put(src, off, len);
    }

    public void long2int(byte[] src, int index_src, long[] dst,
            int index_dst, int len) {
        checkBuf(buf);
        byteBuffer.position(index_src);
        byteBuffer.asLongBuffer().get(dst, index_dst, len);
    }

    public void float2byte(float[] src, int off, int len, byte[] dst,
            int off2) {
        checkBuf(buf);
        byteBuffer.position(off2);
        byteBuffer.asFloatBuffer().put(src, off, len);
    }

    public void float2int(byte[] src, int index_src, float[] dst,
            int index_dst, int len) {
        checkBuf(buf);
        byteBuffer.position(index_src);
        byteBuffer.asFloatBuffer().get(dst, index_dst, len);
    }

    public void double2byte(double[] src, int off, int len, byte[] dst,
            int off2) {
        checkBuf(buf);
        byteBuffer.position(off2);
        byteBuffer.asDoubleBuffer().put(src, off, len);
    }

    public void double2int(byte[] src, int index_src, double[] dst,
            int index_dst, int len) {
        checkBuf(buf);
        byteBuffer.position(index_src);
        byteBuffer.asDoubleBuffer().get(dst, index_dst, len);
    }

}
