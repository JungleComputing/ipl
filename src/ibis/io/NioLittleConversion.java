package ibis.io;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.ShortBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.FloatBuffer;
import java.nio.DoubleBuffer;

import java.nio.BufferUnderflowException;

public final class NioLittleConversion extends SimpleLittleConversion { 

    public static final int BUFFER_SIZE = 10 * 1024;

    public static final int CHAR_THRESHOLD = 600 / CHAR_SIZE;
    public static final int SHORT_THRESHOLD = 600 / SHORT_SIZE;
    public static final int INT_THRESHOLD = 600 / INT_SIZE;
    public static final int LONG_THRESHOLD = 600 / LONG_SIZE;
    public static final int FLOAT_THRESHOLD = 60 / FLOAT_SIZE;
    public static final int DOUBLE_THRESHOLD = 96 / DOUBLE_SIZE;

    private final ByteOrder order;

    private final ByteBuffer byteBuffer;
    private final CharBuffer charBuffer;
    private final ShortBuffer shortBuffer;
    private final IntBuffer intBuffer;
    private final LongBuffer longBuffer;
    private final FloatBuffer floatBuffer;
    private final DoubleBuffer doubleBuffer;

    public NioLittleConversion() {
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

    public void char2byte(char[] src, int off, int len, 
	    byte [] dst, int off2) {

	if (len < CHAR_THRESHOLD) {
	    super.char2byte(src, off, len, dst, off2);
	} else if(len > (BUFFER_SIZE / 2)) {
	    CharBuffer buffer = ByteBuffer.wrap(dst,off2,len * 2).
		order(order).asCharBuffer();
	    buffer.put(src,off,len);
	} else {
	    charBuffer.clear();
	    charBuffer.put(src, off, len);

	    byteBuffer.position(0).limit(len * 2);
	    byteBuffer.get(dst, off2, len * 2);
	}
    }

    public void byte2char(byte[] src, int index_src, 
	    char[] dst, int index_dst, int len) {

	if (len < CHAR_THRESHOLD) {
	    super.byte2char(src, index_src, dst, index_dst, len);
	} else if(len > (BUFFER_SIZE / 2)) {
	    CharBuffer buffer = ByteBuffer.wrap(src,index_src,len * 2).
		order(order).asCharBuffer();
	    buffer.get(dst,index_dst,len);
	} else {
	    byteBuffer.clear();
	    byteBuffer.put(src, index_src, len * 2);

	    charBuffer.position(0).limit(len);
	    charBuffer.get(dst, index_dst, len);
	}
    }

    public void short2byte(short[] src, int off, int len, 
	    byte [] dst, int off2) {

	if (len < SHORT_THRESHOLD) {
	    super.short2byte(src, off, len, dst, off2);
	} else if(len > (BUFFER_SIZE / 2)) {
	    ShortBuffer buffer = ByteBuffer.wrap(dst,off2,len * 2).
		order(order).asShortBuffer();
	    buffer.put(src,off,len);
	} else {
	    shortBuffer.clear();
	    shortBuffer.put(src, off, len);

	    byteBuffer.position(0).limit(len * 2);
	    byteBuffer.get(dst, off2, len * 2);
	}
    }

    public void byte2short(byte[] src, int index_src, 
	    short[] dst, int index_dst, int len) {

	if (len < SHORT_THRESHOLD) {
	    super.byte2short(src, index_src, dst, index_dst, len);
	} else if(len > (BUFFER_SIZE / 2)) {
	    ShortBuffer buffer = ByteBuffer.wrap(src,index_src,len * 2).
		order(order).asShortBuffer();
	    buffer.get(dst,index_dst,len);
	} else {
	    byteBuffer.clear();
	    byteBuffer.put(src, index_src, len * 2);

	    shortBuffer.position(0).limit(len);
	    shortBuffer.get(dst, index_dst, len);
	}

    }

    public void int2byte(int[] src, int off, int len, byte [] dst, int off2) {

	if (len < INT_THRESHOLD) {
	    super.int2byte(src, off, len, dst, off2);
	} else if(len > (BUFFER_SIZE / 4)) {
	    IntBuffer buffer = ByteBuffer.wrap(dst,off2,len * 4).
		order(order).asIntBuffer();
	    buffer.put(src,off,len);
	} else {
	    intBuffer.clear();
	    intBuffer.put(src, off, len);

	    byteBuffer.position(0).limit(len * 4);
	    byteBuffer.get(dst, off2, len * 4);
	}
    }

    public void byte2int(byte[] src, int index_src, int[] dst, 
	    int index_dst, int len) {

	if (len < INT_THRESHOLD) {
	    super.byte2int(src, index_src, dst, index_dst, len);
	} else if(len > (BUFFER_SIZE / 4)) {
	    IntBuffer buffer = ByteBuffer.wrap(src,index_src,len * 4).
		order(order).asIntBuffer();
	    buffer.get(dst,index_dst,len);
	} else {
	    byteBuffer.clear();
	    byteBuffer.put(src, index_src, len * 4);

	    intBuffer.position(0).limit(len);
	    intBuffer.get(dst, index_dst, len);
	}
    }

    public void long2byte(long[] src, int off, int len, 
	    byte [] dst, int off2) {

	if (len < LONG_THRESHOLD) {
	    super.long2byte(src, off, len, dst, off2);
	} else if(len > (BUFFER_SIZE / 8)) {
	    LongBuffer buffer = ByteBuffer.wrap(dst,off2,len * 8).
		order(order).asLongBuffer();
	    buffer.put(src,off,len);
	} else {
	    longBuffer.clear();
	    longBuffer.put(src, off, len);

	    byteBuffer.position(0).limit(len * 8);
	    byteBuffer.get(dst, off2, len * 8);
	}
    }

    public void byte2long(byte[] src, int index_src, 
	    long[] dst, int index_dst, int len) { 		

	if (len < LONG_THRESHOLD) {
	    super.byte2long(src, index_src, dst, index_dst, len);
	} else if(len > (BUFFER_SIZE / 8)) {
	    LongBuffer buffer = ByteBuffer.wrap(src,index_src,len * 8).
		order(order).asLongBuffer();
	    buffer.get(dst,index_dst,len);
	} else {
	    byteBuffer.clear();
	    byteBuffer.put(src, index_src, len * 8);

	    longBuffer.position(0).limit(len);
	    longBuffer.get(dst, index_dst, len);
	}
    } 

    public void float2byte(float[] src, int off, int len, 
	    byte [] dst, int off2) {

	if (len < FLOAT_THRESHOLD) {
	    super.float2byte(src, off, len, dst, off2);
	} else if(len > (BUFFER_SIZE / 4)) {
	    FloatBuffer buffer = ByteBuffer.wrap(dst,off2,len * 4).
		order(order).asFloatBuffer();
	    buffer.put(src,off,len);
	} else {
	    floatBuffer.clear();
	    floatBuffer.put(src, off, len);

	    byteBuffer.position(0).limit(len * 4);
	    byteBuffer.get(dst, off2, len * 4);
	}
    }

    public void byte2float(byte[] src, int index_src, 
	    float[] dst, int index_dst, int len) { 

	if (len < FLOAT_THRESHOLD) {
	    super.byte2float(src, index_src, dst, index_dst, len);
	} else if(len > (BUFFER_SIZE / 4)) {
	    FloatBuffer buffer = ByteBuffer.wrap(src,index_src,len * 4).
		order(order).asFloatBuffer();
	    buffer.get(dst,index_dst,len);
	} else {
	    byteBuffer.clear();
	    byteBuffer.put(src, index_src, len * 4);

	    floatBuffer.position(0).limit(len);
	    floatBuffer.get(dst, index_dst, len);
	}
    } 


    public void double2byte(double[] src, int off, int len, 
	    byte [] dst, int off2) {

	if (len < DOUBLE_THRESHOLD) {
	    super.double2byte(src, off, len, dst, off2);
	} else if (len > (BUFFER_SIZE / 8)) {
	    DoubleBuffer buffer = ByteBuffer.wrap(dst,off2,len * 8).
		order(order).asDoubleBuffer();
	    buffer.put(src,off,len);
	} else {
	    doubleBuffer.clear();
	    doubleBuffer.put(src, off, len);

	    byteBuffer.position(0).limit(len * 8);
	    byteBuffer.get(dst, off2, len * 8);
	}
    }


    public void byte2double(byte[] src, int index_src, 
	    double[] dst, int index_dst, int len) { 

	if (len < DOUBLE_THRESHOLD) {
	    super.byte2double(src, index_src, dst, index_dst, len);
	} else if (len > (BUFFER_SIZE / 8)) {
	    DoubleBuffer buffer = ByteBuffer.wrap(src,index_src,len * 8).
		order(order).asDoubleBuffer();
	    buffer.get(dst,index_dst,len);
	} else {
	    byteBuffer.clear();
	    byteBuffer.put(src, index_src, len * 8);

	    doubleBuffer.position(0).limit(len);
	    doubleBuffer.get(dst, index_dst, len);
	}
    }
}
