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

public final class NioConversion extends Conversion { 

    public final int BUFFER_SIZE = 10 * 1024;

    private ByteOrder order;

    private ByteBuffer byteBuffer;
    private CharBuffer charBuffer;
    private ShortBuffer shortBuffer;
    private IntBuffer intBuffer;
    private LongBuffer longBuffer;
    private FloatBuffer floatBuffer;
    private DoubleBuffer doubleBuffer;

    NioConversion(boolean bigEndian) {

	if(bigEndian) {
	    order = ByteOrder.BIG_ENDIAN;
	} else {
	    order = ByteOrder.LITTLE_ENDIAN;
	}

	byteBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE).order(order);

	// views on the bytebuffer to fill/drain it efficiently
	charBuffer = byteBuffer.asCharBuffer();
	shortBuffer = byteBuffer.asShortBuffer();
	intBuffer = byteBuffer.asIntBuffer();
	longBuffer = byteBuffer.asLongBuffer();
	floatBuffer = byteBuffer.asFloatBuffer();
	doubleBuffer = byteBuffer.asDoubleBuffer();
    }

    public boolean bigEndian() {
	return order == (ByteOrder.BIG_ENDIAN);
    }


    public byte boolean2byte(boolean src) {
	return (src ? (byte) 1 : (byte) 0);
    }

    public boolean byte2boolean(byte src) {
	return (src == 1);
    }

    public void char2byte(char src, byte[] dst, int off) {
	    byteBuffer.clear();
	    byteBuffer.putChar(src);
	    byteBuffer.flip();
	    byteBuffer.get(dst, off, CHAR_SIZE);
    }

    public char byte2char(byte[] src, int off) {
	    byteBuffer.clear();
	    byteBuffer.put(src, off, CHAR_SIZE);
	    byteBuffer.flip();
	    return byteBuffer.getChar();
    }

    public void short2byte(short src, byte[] dst, int off) {
	    byteBuffer.clear();
	    byteBuffer.putShort(src);
	    byteBuffer.flip();
	    byteBuffer.get(dst, off, SHORT_SIZE);

    }

    public short byte2short(byte[] src, int off) {
	    byteBuffer.clear();
	    byteBuffer.put(src, off, SHORT_SIZE);
	    byteBuffer.flip();
	    return byteBuffer.getShort();
    }

    public void int2byte(int src, byte[] dst, int off) {
	    byteBuffer.clear();
	    byteBuffer.putInt(src);
	    byteBuffer.flip();
	    byteBuffer.get(dst, off, INT_SIZE);
    }

    public int byte2int(byte[] src, int off) {
	    byteBuffer.clear();
	    byteBuffer.put(src, off, INT_SIZE);
	    byteBuffer.flip();
	    return byteBuffer.getInt();
    }

    public void long2byte(long src, byte[] dst, int off) {
	    byteBuffer.clear();
	    byteBuffer.putLong(src);
	    byteBuffer.flip();
	    byteBuffer.get(dst, off, LONG_SIZE);
    }

    public long byte2long(byte[] src, int off) {
	    byteBuffer.clear();
	    byteBuffer.put(src, off, LONG_SIZE);
	    byteBuffer.flip();
	    return byteBuffer.getLong();
    }

    public void float2byte(float src, byte[] dst, int off) {
	    byteBuffer.clear();
	    byteBuffer.putFloat(src);
	    byteBuffer.flip();
	    byteBuffer.get(dst, off, FLOAT_SIZE);
    }

    public float byte2float(byte[] src, int off) {
	    byteBuffer.clear();
	    byteBuffer.put(src, off, FLOAT_SIZE);
	    byteBuffer.flip();
	    return byteBuffer.getFloat();
    }

    public void double2byte(double src, byte[] dst, int off) {
	    byteBuffer.clear();
	    byteBuffer.putDouble(src);
	    byteBuffer.flip();
	    byteBuffer.get(dst, off, DOUBLE_SIZE);
    }

    public double byte2double(byte[] src, int off) {
	    byteBuffer.clear();
	    byteBuffer.put(src, off, DOUBLE_SIZE);
	    byteBuffer.flip();
	    return byteBuffer.getDouble();
    }

    public void boolean2byte(boolean[] src, int off, int len, 
	    byte [] dst, int off2) {

	// booleans aren't supported in nio.
	for (int i=0;i<len;i++) {                       
	    dst[off2+i] = (src[off+i] ? (byte)1 : (byte)0);
	} 
    }


    public void byte2boolean(byte[] src, int index_src, 
	    boolean[] dst, int index_dst, int len) { 

	// booleans aren't supported in nio.
	for (int i=0;i<len;i++) {                       
	    dst[index_dst+i] = (src[index_src + i] == 1);
	}
    } 


    public void char2byte(char[] src, int off, int len, 
	    byte [] dst, int off2) {

	if(len > (BUFFER_SIZE / 2)) {
	    CharBuffer buffer = ByteBuffer.wrap(dst,off2,len * 2).
		order(order).asCharBuffer();
	    buffer.put(src,off,len);
	} else {
	    charBuffer.clear();
	    charBuffer.put(src, off, len);

	    byteBuffer.position(0).limit(charBuffer.position() * 2);
	    byteBuffer.get(dst, off2, len * 2);
	}
    }

    public void byte2char(byte[] src, int index_src, 
	    char[] dst, int index_dst, int len) {

	if(len > (BUFFER_SIZE / 2)) {
	    CharBuffer buffer = ByteBuffer.wrap(src,index_src,len * 2).
		order(order).asCharBuffer();
	    buffer.get(dst,index_dst,len);
	} else {
	    byteBuffer.clear();
	    byteBuffer.put(src, index_src, len * 2);

	    charBuffer.position(0).limit(byteBuffer.position() / 2);
	    charBuffer.get(dst, index_dst, len);
	}
    }

    public void short2byte(short[] src, int off, int len, 
	    byte [] dst, int off2) {

	if(len > (BUFFER_SIZE / 2)) {
	    ShortBuffer buffer = ByteBuffer.wrap(dst,off2,len * 2).
		order(order).asShortBuffer();
	    buffer.put(src,off,len);
	} else {
	    shortBuffer.clear();
	    shortBuffer.put(src, off, len);

	    byteBuffer.position(0).limit(shortBuffer.position() * 2);
	    byteBuffer.get(dst, off2, len * 2);
	}
    }

    public void byte2short(byte[] src, int index_src, 
	    short[] dst, int index_dst, int len) {

	if(len > (BUFFER_SIZE / 2)) {
	    ShortBuffer buffer = ByteBuffer.wrap(src,index_src,len * 2).
		order(order).asShortBuffer();
	    buffer.get(dst,index_dst,len);
	} else {
	    byteBuffer.clear();
	    byteBuffer.put(src, index_src, len * 2);

	    shortBuffer.position(0).limit(byteBuffer.position() / 2);
	    shortBuffer.get(dst, index_dst, len);
	}

    }

    public void int2byte(int[] src, int off, int len, byte [] dst, int off2) {

	if(len > (BUFFER_SIZE / 4)) {
	    IntBuffer buffer = ByteBuffer.wrap(dst,off2,len * 4).
		order(order).asIntBuffer();
	    buffer.put(src,off,len);
	} else {
	    intBuffer.clear();
	    intBuffer.put(src, off, len);

	    byteBuffer.position(0).limit(intBuffer.position() * 4);
	    byteBuffer.get(dst, off2, len * 4);
	}
    }

    public void byte2int(byte[] src, int index_src, int[] dst, 
	    int index_dst, int len) {

	if(len > (BUFFER_SIZE / 4)) {
	    IntBuffer buffer = ByteBuffer.wrap(src,index_src,len * 4).
		order(order).asIntBuffer();
	    buffer.get(dst,index_dst,len);
	} else {
	    byteBuffer.clear();
	    byteBuffer.put(src, index_src, len * 4);

	    intBuffer.position(0).limit(byteBuffer.position() / 4);
	    intBuffer.get(dst, index_dst, len);
	}
    }

    public void long2byte(long[] src, int off, int len, 
	    byte [] dst, int off2) {

	if(len > (BUFFER_SIZE / 8)) {
	    LongBuffer buffer = ByteBuffer.wrap(dst,off2,len * 8).
		order(order).asLongBuffer();
	    buffer.put(src,off,len);
	} else {
	    longBuffer.clear();
	    longBuffer.put(src, off, len);

	    byteBuffer.position(0).limit(longBuffer.position() * 8);
	    byteBuffer.get(dst, off2, len * 8);
	}
    }

    public void byte2long(byte[] src, int index_src, 
	    long[] dst, int index_dst, int len) { 		

	if(len > (BUFFER_SIZE / 8)) {
	    LongBuffer buffer = ByteBuffer.wrap(src,index_src,len * 8).
		order(order).asLongBuffer();
	    buffer.get(dst,index_dst,len);
	} else {
	    byteBuffer.clear();
	    byteBuffer.put(src, index_src, len * 8);

	    longBuffer.position(0).limit(byteBuffer.position() / 8);
	    longBuffer.get(dst, index_dst, len);
	}
    } 

    public void float2byte(float[] src, int off, int len, 
	    byte [] dst, int off2) {

	if(len > (BUFFER_SIZE / 4)) {
	    FloatBuffer buffer = ByteBuffer.wrap(dst,off2,len * 4).
		order(order).asFloatBuffer();
	    buffer.put(src,off,len);
	} else {
	    floatBuffer.clear();
	    floatBuffer.put(src, off, len);

	    byteBuffer.position(0).limit(floatBuffer.position() * 4);
	    byteBuffer.get(dst, off2, len * 4);
	}
    }

    public void byte2float(byte[] src, int index_src, 
	    float[] dst, int index_dst, int len) { 

	if(len > (BUFFER_SIZE / 4)) {
	    FloatBuffer buffer = ByteBuffer.wrap(src,index_src,len * 4).
		order(order).asFloatBuffer();
	    buffer.get(dst,index_dst,len);
	} else {
	    byteBuffer.clear();
	    byteBuffer.put(src, index_src, len * 4);

	    floatBuffer.position(0).limit(byteBuffer.position() / 4);
	    floatBuffer.get(dst, index_dst, len);
	}
    } 


    public void double2byte(double[] src, int off, int len, 
	    byte [] dst, int off2) {

	if (len > (BUFFER_SIZE / 8)) {
	    DoubleBuffer buffer = ByteBuffer.wrap(dst,off2,len * 8).
		order(order).asDoubleBuffer();
	    buffer.put(src,off,len);
	} else {
	    doubleBuffer.clear();
	    doubleBuffer.put(src, off, len);

	    byteBuffer.position(0).limit(doubleBuffer.position() * 8);
	    byteBuffer.get(dst, off2, len * 8);
	}
    }


    public void byte2double(byte[] src, int index_src, 
	    double[] dst, int index_dst, int len) { 

	if (len > (BUFFER_SIZE / 8)) {
	    DoubleBuffer buffer = ByteBuffer.wrap(src,index_src,len * 8).
		order(order).asDoubleBuffer();
	    buffer.get(dst,index_dst,len);
	} else {
	    byteBuffer.clear();
	    byteBuffer.put(src, index_src, len * 8);

	    doubleBuffer.position(0).limit(byteBuffer.position() / 8);
	    doubleBuffer.get(dst, index_dst, len);
	}
    }
}
