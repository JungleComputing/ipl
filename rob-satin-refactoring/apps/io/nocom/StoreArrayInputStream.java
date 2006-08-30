/* $Id$ */


import java.io.IOException;

/**
 * Extends InputStream with read of array of primitives and readSingleInt
 */
public class StoreArrayInputStream extends ibis.io.DataInputStream {

    StoreBuffer buf;

    int boolean_count = 0;

    int byte_count = 0;

    int short_count = 0;

    int char_count = 0;

    int int_count = 0;

    int long_count = 0;

    int float_count = 0;

    int double_count = 0;

    public StoreArrayInputStream(StoreBuffer buf) {
        this.buf = buf;
    }

    public void reset() {
        boolean_count = 0;
        byte_count = 0;
        short_count = 0;
        char_count = 0;
        int_count = 0;
        long_count = 0;
        float_count = 0;
        double_count = 0;
    }

    public byte readByte() {
        return buf.byte_store[byte_count++];
    }

    public boolean readBoolean() {
        return buf.boolean_store[boolean_count++];
    }

    public char readChar() {
        return buf.char_store[char_count++];
    }

    public short readShort() {
        return buf.short_store[short_count++];
    }

    public int readInt() {
        return buf.int_store[int_count++];
    }

    public long readLong() {
        return buf.long_store[long_count++];
    }

    public float readFloat() {
        return buf.float_store[float_count++];
    }

    public double readDouble() {
        return buf.double_store[double_count++];
    }

    public void readArray(boolean[] a, int off, int len) {
        // System.out.println("readArray boolean " + len);
        System.arraycopy(buf.boolean_store, boolean_count, a, off, len);
        boolean_count += len;
    }

    public void readArray(byte[] a, int off, int len) {
        // System.out.println("readArray byte " + len);
        System.arraycopy(buf.byte_store, byte_count, a, off, len);
        byte_count += len;
    }

    public void readArray(short[] a, int off, int len) {
        // System.out.println("readArray short " + len);
        System.arraycopy(buf.short_store, short_count, a, off, len);
        short_count += len;
    }

    public void readArray(char[] a, int off, int len) {
        // System.out.println("readArray char " + len);
        System.arraycopy(buf.char_store, char_count, a, off, len);
        char_count += len;
    }

    public void readArray(int[] a, int off, int len) {
        // System.out.println("readArray int " + len);
        System.arraycopy(buf.int_store, int_count, a, off, len);
        int_count += len;
    }

    public void readArray(long[] a, int off, int len) {
        // System.out.println("readArray long " + len);
        System.arraycopy(buf.long_store, long_count, a, off, len);
        long_count += len;
    }

    public void readArray(float[] a, int off, int len) {
        // System.out.println("readArray float " + len);
        System.arraycopy(buf.float_store, float_count, a, off, len);
        float_count += len;
    }

    public void readArray(double[] a, int off, int len) {
//        System.err.println("stream read double array, off = " + off + " len = " + len);
        System.arraycopy(buf.double_store, double_count, a, off, len);
        double_count += len;
    }

/*    
    public void readArray(double[] a, int off, int len) {
        System.err.println("stream read double array, off = " + off + " len = " + len);
        buf.readArray(a, off, len);
        double_count += len;
    }
  */  
    public int read() {
        if (buf.byte_store.length <= byte_count) {
            return -1;
        }
        return (buf.byte_store[byte_count++] & 0377);
    }

    public int read(byte[] b) {
        if (byte_count >= buf.byte_store.length) return -1;
        if (byte_count + b.length > buf.byte_store.length) {
            System.arraycopy(buf.byte_store, byte_count, b, 0, buf.byte_store.length - byte_count);
            int rval = buf.byte_store.length - byte_count;
            byte_count = buf.byte_store.length;
            return rval;
        }
        readArray(b, 0, b.length);
        return b.length;
    }

    public int read(byte[] b, int off, int len) {
        if (byte_count >= buf.byte_store.length) return -1;
        if (byte_count + len > buf.byte_store.length) {
            System.arraycopy(buf.byte_store, byte_count, b, off, buf.byte_store.length - byte_count);
            int rval = buf.byte_store.length - byte_count;
            byte_count = buf.byte_store.length;
            return rval;
        }
        readArray(b, off, len);
        return len;
    }

    public long bytesRead() {
        return 0L;
    }

    public void resetBytesRead() {
    }

    public int available() throws IOException {
        return 0;
    }

    public void close() throws IOException {
    }
}
