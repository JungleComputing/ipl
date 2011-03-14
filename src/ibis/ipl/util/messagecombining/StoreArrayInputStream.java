/* $Id$ */

package ibis.ipl.util.messagecombining;

import ibis.ipl.ReadMessage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;

/**
 * Extends InputStream with read of array of primitives and readSingleInt
 */
public class StoreArrayInputStream extends ibis.io.DataInputStream {
       
    private int boolean_count = 0;
    private boolean[] boolean_store = new boolean[0];

    private int byte_count = 0;
    private byte[] byte_store = new byte[0];

    private int short_count = 0;
    private short[] short_store = new short[0];
    
    private int char_count = 0;
    private char[] char_store = new char[0];

    private int int_count = 0;
    private int[] int_store = new int[0];

    private int long_count = 0;
    private long[] long_store = new long[0];

    private int float_count = 0;
    private float[] float_store = new float[0];

    private int double_count = 0;
    private double[] double_store = new double[0];
    
    private int [] indices = new int[8];

    private int count = 0;
    private int read = 0;
    
    /**
     * Construct a new StoreArrayInputStream
     * 
     * 
     */
    public StoreArrayInputStream() {
    	// nothing here
    }
       
    public int bufferSize() {
        return -1;
    }
    
    public void reset(ReadMessage m) throws IOException {                     
        
        boolean_count = 0;
        byte_count = 0;
        short_count = 0;
        char_count = 0;
        int_count = 0;
        long_count = 0;
        float_count = 0;
        double_count = 0;
        
        m.readArray(indices);
        
        count = indices[0];
        count += indices[1];
        count += 2*indices[2];       
        count += 2*indices[3];
        count += 4*indices[4];
        count += 8*indices[5];
        count += 4*indices[6];
        count += 8*indices[7];
        
        if (indices[0] > 0) {
            if (boolean_store.length < indices[0]) { 
                boolean_store = new boolean[indices[0]];
            }                
            m.readArray(boolean_store, 0, indices[0]);
        }
        
        if (indices[1] > 0) {
            if (byte_store.length < indices[1]) {         
                byte_store = new byte[indices[1]];
            } 
            m.readArray(byte_store, 0, indices[1]);
        }
        
        if (indices[2] > 0) {
            if (short_store.length < indices[2]) {            
                short_store = new short[indices[2]];
            } 
            m.readArray(short_store, 0, indices[2]);
        }
        
        if (indices[3] > 0) {
            if (char_store.length < indices[3]) {            
                char_store = new char[indices[3]];
            } 
            m.readArray(char_store, 0, indices[3]);
        }
        
        if (indices[4] > 0) {
            if (int_store.length < indices[4]) {            
                int_store = new int[indices[4]];
            } 
            m.readArray(int_store, 0, indices[4]);
        }
        
        if (indices[5] > 0) {
            if (long_store.length < indices[5]) { 
                long_store = new long[indices[5]];
            }           
            m.readArray(long_store, 0, indices[5]);
        }
        
        if (indices[6] > 0) {
            if (float_store.length < indices[6]) { 
                float_store = new float[indices[6]];
            }  
            m.readArray(float_store, 0, indices[6]);
        }
        
        if (indices[7] > 0) {
            if (double_store.length < indices[7]) { 
                double_store = new double[indices[7]];
            } 
            m.readArray(double_store, 0, indices[7]);
        }
        
       // System.out.println("Read " + ((8*4) + count) + " bytes.");
    }

    public byte readByte() {
        read++;        
        count--;
        return byte_store[byte_count++];
    }

    public boolean readBoolean() {
        read++;
        count--;
        return boolean_store[boolean_count++];
    }

    public char readChar() {
        read+=2;
        count-=2;
        return char_store[char_count++];
    }

    public short readShort() {
        read+=2;
        count-=2;
        return short_store[short_count++];
    }

    public int readInt() {
        read+=4;
        count-=4;
        return int_store[int_count++];
    }

    public long readLong() {
        read+=8;
        count-=8;
        return long_store[long_count++];
    }

    public float readFloat() {
        read+=4;
        count-=4;
        return float_store[float_count++];
    }

    public double readDouble() {
        read+=8;
        count-=8;
        return double_store[double_count++];
    }

    public void readArray(boolean[] a, int off, int len) {
        read+=len;
        count-=len;
        System.arraycopy(boolean_store, boolean_count, a, off, len);
        boolean_count += len;
    }

    public void readArray(byte[] a, int off, int len) {
        read+=len;
        count-=len;
        System.arraycopy(byte_store, byte_count, a, off, len);
        byte_count += len;
    }

    public void readArray(short[] a, int off, int len) {
        read+=2*len;
        count-=2*len;
   //     System.out.println("Copying from short store[" + short_store.length + 
   //             "] " + short_count + "..." + (short_count+len)); 
        
        System.arraycopy(short_store, short_count, a, off, len);
        short_count += len;
    }

    public void readArray(char[] a, int off, int len) {
        read+=2*len;
        count-=2*len;
        System.arraycopy(char_store, char_count, a, off, len);
        char_count += len;
    }

    public void readArray(int[] a, int off, int len) {
        read+=4*len;
        count-=4*len;
        System.arraycopy(int_store, int_count, a, off, len);
        int_count += len;
    }

    public void readArray(long[] a, int off, int len) {
        read+=8*len;
        count-=8*len;
        System.arraycopy(long_store, long_count, a, off, len);
        long_count += len;
    }

    public void readArray(float[] a, int off, int len) {
        read+=4*len;
        count-=4*len;
        System.arraycopy(float_store, float_count, a, off, len);
        float_count += len;
    }

    public void readArray(double[] a, int off, int len) {
        read+=8*len;
        count-=8*len;
        System.arraycopy(double_store, double_count, a, off, len);
        double_count += len;
    }

    public int read() {
        if (byte_store.length <= byte_count) {
            return -1;
        }
        read++;
        count--;
        return (byte_store[byte_count++] & 0377);
    }

    public int read(byte[] b) {
        if (byte_count >= byte_store.length) return -1;
        if (byte_count + b.length > byte_store.length) {
            System.arraycopy(byte_store, byte_count, b, 0, byte_store.length - byte_count);
            int rval = byte_store.length - byte_count;
            byte_count = byte_store.length;
            read+=rval;
            count-=rval;
            return rval;
        }        
        readArray(b, 0, b.length);
        read += b.length;
        count-=b.length;
        return b.length;
    }

    public int read(byte[] b, int off, int len) {
        if (byte_count >= byte_store.length) return -1;
        if (byte_count + len > byte_store.length) {
            System.arraycopy(byte_store, byte_count, b, off, byte_store.length - byte_count);
            int rval = byte_store.length - byte_count;
            byte_count = byte_store.length;
            read+=rval;
            count-=rval;
            return rval;
        }
        readArray(b, off, len);
        read+=len;
        count-=len;
        return len;
    }

    public long bytesRead() {
        return read;
    }

    public void resetBytesRead() {     
        read = 0;
    }

    public int available() throws IOException {
        return count;
    }

    public void close() throws IOException {
    	// nothing to do here
    }

    public void readByteBuffer(ByteBuffer value) throws IOException,
	    ReadOnlyBufferException {
	int len = value.limit() - value.position();
        read += len;
        count -= len;
        value.put(byte_store, byte_count, len);
        byte_count += len;	
    }
}
