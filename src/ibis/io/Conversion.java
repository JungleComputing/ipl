package ibis.io;

import java.lang.reflect.Constructor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


public abstract class Conversion { 

    /* NOTE: 
     *  
     * All conversion methods in this class have the precondition that 
     * the data actually fits in the destination buffer. The user should 
     * do the buffering himself.  
     */

    public static Conversion defaultConversion;

    // load a SimpleConversion to and from networkorder as default
    static {
	defaultConversion = new SimpleBigConversion();

    }

    /**
     * The number of bits in a single 'byte'.
     */
    public final static int	BITS_PER_BYTE = 8;

    /**
     * The number of bytes in a single 'boolean'.
     */
    public final static int	BOOLEAN_SIZE = 1;

    /**
     * The number of bytes in a single 'short'.
     */
    public final static int	SHORT_SIZE = 2;

    /**
     * The number of bits in a single 'short'.
     */
    public final static int	BITS_PER_SHORT = BITS_PER_BYTE * SHORT_SIZE;

    /**
     * The number of bytes in a single 'char'.
     */
    public final static int	CHAR_SIZE = 2;

    /**
     * The number of bits in a single 'char'.
     */
    public final static int	BITS_PER_CHAR = BITS_PER_BYTE * CHAR_SIZE;

    /**
     * The number of bytes in a single 'int'.
     */
    public final static int	INT_SIZE = 4;

    /**
     * The number of bits in a single 'int'.
     */
    public final static int	BITS_PER_INT = BITS_PER_BYTE * INT_SIZE;

    /**
     * The number of bytes in a single 'long'.
     */
    public final static int	LONG_SIZE = 8;

    /**
     * The number of bits in a single 'long'.
     */
    public final static int	BITS_PER_LONG = BITS_PER_BYTE * LONG_SIZE;

    /**
     * The number of bytes in a single 'float'.
     */
    public final static int	FLOAT_SIZE = 4;

    /**
     * The number of bits in a single 'float'.
     */
    public final static int	BITS_PER_FLOAT = BITS_PER_BYTE * FLOAT_SIZE;

    /**
     * The number of bytes in a single 'double'.
     */
    public final static int	DOUBLE_SIZE = 8;

    /**
     * The number of bits in a single 'double'.
     */
    public final static int	BITS_PER_DOUBLE = BITS_PER_BYTE * DOUBLE_SIZE;


    /*
     * Return a conversion, given the class name of it.
     */
    private static final Conversion loadConversion(String className)
	throws Exception {

	return (Conversion)Class.forName(className).newInstance();

    }

    /**
     * Load a conversion
     */
    public static final Conversion loadConversion(boolean bigEndian) {

	try {
	    if(bigEndian) {
		return loadConversion("ibis.io.NioBigConversion");
	    } else {
		return loadConversion("ibis.io.NioLittleConversion");
	    }
	} catch (Exception e) {
	    // loading of nio conversion failed.

	    if(bigEndian) {
		return new SimpleBigConversion();
	    } else {
		return new SimpleLittleConversion();
	    }
	}
    }


    /**
     * Returns if this conversion iconverts to big-endian or not
     */
    public abstract boolean bigEndian();

    public abstract byte boolean2byte(boolean src);

    public abstract boolean byte2boolean(byte src);

    public abstract void char2byte(char src, byte[] dst, int off);

    public abstract char byte2char(byte[] src, int off);

    public abstract void short2byte(short src, byte[] dst, int off);

    public abstract short byte2short(byte[] src, int off);

    public abstract void int2byte(int src, byte[] dst, int off);

    public abstract int byte2int(byte[] src, int off);

    public abstract void long2byte(long src, byte[] dst, int off);

    public abstract long byte2long(byte[] src, int off);

    public abstract void float2byte(float src, byte[] dst, int off);

    public abstract float byte2float(byte[] src, int off);

    public abstract void double2byte(double src, byte[] dst, int off);

    public abstract double byte2double(byte[] src, int off);

    public abstract void boolean2byte(boolean[] src, int off, int len,
	    byte [] dst, int off2);

    public abstract void byte2boolean(byte[] src, int index_src,
	    boolean[] dst, int index_dst, int len);

    public abstract void char2byte(char[] src, int off, int len, 
	    byte [] dst, int off2);

    public abstract void byte2char(byte[] src, int index_src, 
	    char[] dst, int index_dst, int len);

    public abstract void short2byte(short[] src, int off, int len, 
	    byte [] dst, int off2);

    public abstract void byte2short(byte[] src, int index_src, 
	    short[] dst, int index_dst, int len);

    public abstract void int2byte(int[] src, int off, int len, 
	    byte [] dst, int off2);

    public abstract void byte2int(byte[] src, int index_src, 
	    int[] dst, int index_dst, int len);

    public abstract void long2byte(long[] src, int off, int len, 
	    byte [] dst, int off2);

    public abstract void byte2long(byte[] src, int index_src, 
	    long[] dst, int index_dst, int len);

    public abstract void float2byte(float[] src, int off, int len, 
	    byte [] dst, int off2);

    public abstract void byte2float(byte[] src, int index_src, 
	    float[] dst, int index_dst, int len); 

    public abstract void double2byte(double[] src, int off, int len, 
	    byte [] dst, int off2);

    public abstract void byte2double(byte[] src, int index_src, 
	    double[] dst, int index_dst, int len); 

    /**
     * Writes an object to a byte[].
     */

    public static final byte[] object2byte(Object o) throws IOException {
	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	ObjectOutputStream    oos = new ObjectOutputStream(bos);
	oos.writeObject(o);
	oos.close();
	oos = null;
	return bos.toByteArray();
    }

    /**
     * Reads an object from byte[].
     */

    public static final Object byte2object(byte [] b)
	throws IOException, ClassNotFoundException {
	    ByteArrayInputStream bis = new ByteArrayInputStream(b);
	    ObjectInputStream    ois = new ObjectInputStream(bis);
	    Object               o   = ois.readObject();
	    ois.close();
	    return o;
	}

} 
