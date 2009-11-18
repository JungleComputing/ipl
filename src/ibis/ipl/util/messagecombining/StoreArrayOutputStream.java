/* $Id$ */

package ibis.ipl.util.messagecombining;

import ibis.io.DataOutputStream;
import ibis.ipl.WriteMessage;

import java.io.IOException;

final class StoreArrayOutputStream extends DataOutputStream {
    
    private static final int DEFAULT_SIZE = 16*1024;
        
    private int[] indices = new int[8];    
    
    private boolean[] boolean_store;
    private int boolean_count;
    private byte[] byte_store;
    private int byte_count;
    private short[] short_store;
    private int short_count;
    
    private char[] char_store;
    private int char_count;
    private int[] int_store;
    private int int_count;
    private long[] long_store;
    private int long_count;
    private float[] float_store;
    private int float_count;
    private double[] double_store;
    private int double_count;
    
    private long count = 0;
    
    private final int initialSize; 	
    
    public StoreArrayOutputStream() {
        this(DEFAULT_SIZE);
    }
    
    public StoreArrayOutputStream(int initialSize) {
        this.initialSize = initialSize;
        clear();
    }
    
    public int bufferSize() {
        return -1;
    }
    
    private void clear() {
        boolean_store = new boolean[initialSize];
        byte_store = new byte[initialSize];
        short_store = new short[initialSize];
        char_store = new char[initialSize];
        int_store = new int[initialSize];
        long_store = new long[initialSize];
        float_store = new float[initialSize];
        double_store = new double[initialSize];
        
        boolean_count = 0;
        byte_count = 0;
        short_count = 0;
        char_count = 0;
        int_count = 0;
        long_count = 0;
        float_count = 0;
        double_count = 0;
        
        count = 0;
    }
    
    private final void resize_boolean(int required) { 
        
        int new_size = boolean_store.length;
        
        while (new_size < required) { 
            new_size *= 2;
        }
        
        boolean [] tmp = new boolean[new_size];
        System.arraycopy(boolean_store, 0, tmp, 0, boolean_count);        
        boolean_store = tmp;
    }
    
    private final void resize_byte(int required) { 
        
        int new_size = byte_store.length;
        
        while (new_size < required) { 
            new_size *= 2;
        }
        
        byte [] tmp = new byte[new_size];
        System.arraycopy(byte_store, 0, tmp, 0, byte_count);        
        byte_store = tmp;
    }
    
    private final void resize_short(int required) { 
        
        int new_size = short_store.length;
        
        while (new_size < required) { 
            new_size *= 2;
        }
        
        short [] tmp = new short[new_size];
        System.arraycopy(short_store, 0, tmp, 0, short_count);        
        short_store = tmp;
    }
    
    private final void resize_char(int required) { 
        
        int new_size = char_store.length;
        
        while (new_size < required) { 
            new_size *= 2;
        }
        
        char [] tmp = new char[new_size];
        System.arraycopy(char_store, 0, tmp, 0, char_count);        
        char_store = tmp;
    }
    
    private final void resize_int(int required) { 
        
        int new_size = int_store.length;
        
        while (new_size < required) { 
            new_size *= 2;
        }
        
        int [] tmp = new int[new_size];
        System.arraycopy(int_store, 0, tmp, 0, int_count);        
        int_store = tmp;
    }
    
    private final void resize_long(int required) { 
        
        int new_size = long_store.length;
        
        while (new_size < required) { 
            new_size *= 2;
        }
        
        long [] tmp = new long[new_size];
        System.arraycopy(long_store, 0, tmp, 0, long_count);        
        long_store = tmp;
    }
    
    private final void resize_float(int required) { 
        
        int new_size = float_store.length;
        
        while (new_size < required) { 
            new_size *= 2;
        }
        
        float [] tmp = new float[new_size];
        System.arraycopy(float_store, 0, tmp, 0, float_count);        
        float_store = tmp;
    }
    
    private final void resize_double(int required) { 
        
        int new_size = double_store.length;
        
        while (new_size < required) { 
            new_size *= 2;
        }
        
        double [] tmp = new double[new_size];
        System.arraycopy(double_store, 0, tmp, 0, double_count);        
        double_store = tmp;
    }
          
    public void writeArray(boolean[] a, int off, int len) {
        
        if (boolean_count + len > boolean_store.length) {     
            resize_boolean(boolean_count+len);
        } 
        
        System.arraycopy(a, off, boolean_store, boolean_count, len);
        boolean_count += len;
        count += len;
    }
    
    public void writeArray(byte[] a, int off, int len) {
        
        if (byte_count + len > byte_store.length) {     
            resize_byte(byte_count+len);
        } 
        
        System.arraycopy(a, off, byte_store, byte_count, len);
        byte_count += len;
        count += len;
    }
    
   
    public void writeArray(short[] a, int off, int len) {
        
        if (short_count + len > short_store.length) {     
            resize_short(short_count+len);
        } 
        
        System.arraycopy(a, off, short_store, short_count, len);
        short_count += len;
        count += 2L*len;
    }
    
    public void writeArray(char[] a, int off, int len) {
        
        if (char_count + len > char_store.length) {     
            resize_char(char_count+len);
        } 
        
        System.arraycopy(a, off, char_store, char_count, len);
        char_count += len;
        count += 2L*len;
    }
    
    public void writeArray(int[] a, int off, int len) {
        
        if (int_count + len > int_store.length) {     
            resize_int(int_count+len);
        } 
        
        System.arraycopy(a, off, int_store, int_count, len);
        int_count += len;
        count += 4L*len;
    }
    
    public void writeArray(long[] a, int off, int len) {
        
        if (long_count + len > long_store.length) {     
            resize_long(long_count+len);
        } 
        
        System.arraycopy(a, off, long_store, long_count, len);
        long_count += len;
        count += 8L*len;
    }
    
    public void writeArray(float[] a, int off, int len) {
        
        if (float_count + len > float_store.length) {     
            resize_float(float_count+len);
        } 
        
        System.arraycopy(a, off, float_store, float_count, len);
        float_count += len;
        count += 4L*len;
    }
    
    public void writeArray(double[] a, int off, int len) {
        
        if (double_count + len > double_store.length) {     
            resize_double(double_count+len);
        } 
        
        System.arraycopy(a, off, double_store, double_count, len);
        double_count += len;
        count += 8L*len;
    }
    
    public void write(byte[] a, int off, int len) {
        writeArray(a, off, len);
    }
    
    public void write(byte[] a) {
        writeArray(a, 0, a.length);
    }
    
    public void write(int b) {        
        writeByte((byte) b);
    }
    
    public void writeByte(byte b) {
        
        if (byte_count + 1 > byte_store.length) {     
            resize_byte(byte_count+1);
        } 
        
        byte_store[byte_count++] = b;
        count++;
    }
    
    public void writeBoolean(boolean b) {
        
        if (boolean_count + 1 > boolean_store.length) {     
            resize_boolean(boolean_count+1);
        } 
        
        boolean_store[boolean_count++] = b;
        count++;
    }
    
    
    public void writeShort(short b) {
        if (short_count + 1 > short_store.length) {     
            resize_short(short_count+1);
        } 
        
        short_store[short_count++] = b;
        count += 2;
    }
    
    public void writeChar(char b) {
        if (char_count + 1 > char_store.length) {     
            resize_char(char_count+1);
        } 
        
        char_store[char_count++] = b;
        count += 2;
    }
    
    public void writeInt(int b) {
        if (int_count + 1 > int_store.length) {     
            resize_int(int_count+1);
        } 
        
        int_store[int_count++] = b;
        count += 4;
    }

    
    public void writeLong(long b) {
        if (long_count + 1 > long_store.length) {     
            resize_long(long_count+1);
        } 
        
        long_store[long_count++] = b;
        count += 8;
    }

    public void writeFloat(float b) {
        if (float_count + 1 > float_store.length) {     
            resize_float(float_count+1);
        } 
        
        float_store[float_count++] = b;
        count += 4;
    }
    
    public void writeDouble(double b) {
        if (double_count + 1 > double_store.length) {     
            resize_double(double_count+1);
        } 
        
        double_store[double_count++] = b;
        count += 8;
    }
          
    public long bytesWritten() {
        return count;
    }
    
    public void resetBytesWritten() {
        count = 0;
    }
    
    public void flush() throws IOException {
    	// nothing to flush
    }
    
    public boolean finished() {
        return true;
    }
    
    public void finish() throws IOException {
        flush();
    }
    
    public void close() throws IOException {
        flush();
    }
    
    /**
     * @param message
     * @throws IOException
     */
    public void writeToMessage(WriteMessage message) throws IOException {  
       
        indices[0] = boolean_count;
        indices[1] = byte_count;
        indices[2] = short_count;
        indices[3] = char_count;
        indices[4] = int_count;
        indices[5] = long_count;
        indices[6] = float_count;
        indices[7] = double_count;
        
        message.writeArray(indices);        
        
        if (boolean_count > 0) { 
            message.writeArray(boolean_store, 0, boolean_count);
        } 
        
        if (byte_count > 0) { 
            message.writeArray(byte_store, 0, byte_count);
        } 
        
        if (short_count > 0) { 
            message.writeArray(short_store, 0, short_count);
        } 
        
        if (char_count > 0) { 
            message.writeArray(char_store, 0, char_count);
        } 
        
        if (int_count > 0) { 
            message.writeArray(int_store, 0, int_count);
        } 
        
        if (long_count > 0) { 
            message.writeArray(long_store, 0, long_count);
        } 
        
        if (float_count > 0) { 
            message.writeArray(float_store, 0, float_count);
        } 
        
        if (double_count > 0) { 
            message.writeArray(double_store, 0, double_count);
        } 
        
        message.finish();
                
        boolean_count = 0;
        byte_count = 0;
        short_count = 0;
        char_count = 0;
        int_count = 0;
        long_count = 0;
        float_count = 0;
        double_count = 0;
        
        count = 0;               
    }
}
