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

package ibis.io;

// import java.lang.reflect.Constructor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Properties;

public abstract class Conversion {

    /*
     * NOTE:
     *
     * All conversion methods in this class have the precondition that the data
     * actually fits in the destination buffer. The user should do the buffering
     * himself.
     */

    public static final Conversion defaultConversion;

    private static final Conversion simpleBig;

    private static final Conversion simpleLittle;

    // load a SimpleConversion to and from networkorder as default
    static {
        simpleBig = new SimpleBigConversion();
        simpleLittle = new SimpleLittleConversion();
        defaultConversion = simpleBig;
    }

    /** The number of bits in a single 'byte'. */
    public final static int BITS_PER_BYTE = 8;

    /** The number of bytes in a single 'boolean'. */
    public final static int BOOLEAN_SIZE = 1;

    /** The number of bytes in a single 'byte'. */
    public final static int BYTE_SIZE = 1;

    /** The number of bytes in a single 'short'. */
    public final static int SHORT_SIZE = 2;

    /** The number of bits in a single 'short'. */
    public final static int BITS_PER_SHORT = BITS_PER_BYTE * SHORT_SIZE;

    /** The number of bytes in a single 'char'. */
    public final static int CHAR_SIZE = 2;

    /** The number of bits in a single 'char'. */
    public final static int BITS_PER_CHAR = BITS_PER_BYTE * CHAR_SIZE;

    /** The number of bytes in a single 'int'. */
    public final static int INT_SIZE = 4;

    /** The number of bits in a single 'int'. */
    public final static int BITS_PER_INT = BITS_PER_BYTE * INT_SIZE;

    /** The number of bytes in a single 'long'. */
    public final static int LONG_SIZE = 8;

    /** The number of bits in a single 'long'. */
    public final static int BITS_PER_LONG = BITS_PER_BYTE * LONG_SIZE;

    /** The number of bytes in a single 'float'. */
    public final static int FLOAT_SIZE = 4;

    /** The number of bits in a single 'float'. */
    public final static int BITS_PER_FLOAT = BITS_PER_BYTE * FLOAT_SIZE;

    /** The number of bytes in a single 'double' */
    public final static int DOUBLE_SIZE = 8;

    /** The number of bits in a single 'double' */
    public final static int BITS_PER_DOUBLE = BITS_PER_BYTE * DOUBLE_SIZE;

    /**
     * Returns a conversion, given the class name of it.
     *
     * @param className the class name
     * @return the conversion object
     * @throws Exception when the class could not be loaded for some reason
     */
    public static final Conversion loadConversion(String className) throws Exception {

        return (Conversion) Class.forName(className).getDeclaredConstructor().newInstance();

    }

    /**
     * Load a conversion.
     *
     * @param bigEndian determines whether big-endian or little-endian is used
     * @return the conversion object
     */
    public static final Conversion loadConversion(boolean bigEndian) {
        Properties properties = IOProperties.properties;

        String conversion = properties.getProperty(IOProperties.s_conversion);

        if ("wrap".equalsIgnoreCase(conversion)) {
            // System.err.println("nio/wrap conversion selected");
            try {
                if (bigEndian) {
                    return new ibis.io.nio.NioWrapBigConversion();
                }
                return new ibis.io.nio.NioWrapLittleConversion();
            } catch (Exception e) {
                // nio conversion loading failed
            }
        } else if ("chunk".equalsIgnoreCase(conversion)) {
            // System.err.println("nio/chunk conversion selected");
            try {
                if (bigEndian) {
                    return new ibis.io.nio.NioChunkBigConversion();
                }
                return new ibis.io.nio.NioChunkLittleConversion();
            } catch (Exception e) {
                // nio conversion loading failed
            }
        } else if ("hybridwrap".equalsIgnoreCase(conversion)) {
            // System.err.println("nio/buf conversion selected");
            try {
                if (bigEndian) {
                    return new ibis.io.nio.HybridWrapBigConversion();
                }
                return new ibis.io.nio.HybridWrapLittleConversion();
            } catch (Exception e) {
                // nio conversion loading failed
            }
        } else if (conversion == null || conversion.equalsIgnoreCase("hybrid")) {
            // default conversion
            // if (conversion != null) {
            // System.err.println("hybrid conversion selected");
            // }

            try {
                if (bigEndian) {
                    return new ibis.io.nio.HybridChunkBigConversion();
                }
                return new ibis.io.nio.HybridChunkLittleConversion();
            } catch (Exception e) {
                // hybrid conversion loading failed
            }
        }

        // loading of nio type conversions failed, return simple conversion

        // System.err.println("falling back to simple conversion");

        if (bigEndian) {
            return simpleBig;
        }
        return simpleLittle;
    }

    /**
     * Returns if this conversion converts to big-endian or not.
     *
     * @return if this conversion converts to big-endian
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

    public abstract void boolean2byte(boolean[] src, int off, int len, byte[] dst, int off2);

    public abstract void byte2boolean(byte[] src, int index_src, boolean[] dst, int index_dst, int len);

    public abstract void char2byte(char[] src, int off, int len, byte[] dst, int off2);

    public abstract void byte2char(byte[] src, int index_src, char[] dst, int index_dst, int len);

    public abstract void short2byte(short[] src, int off, int len, byte[] dst, int off2);

    public abstract void byte2short(byte[] src, int index_src, short[] dst, int index_dst, int len);

    public abstract void int2byte(int[] src, int off, int len, byte[] dst, int off2);

    public abstract void byte2int(byte[] src, int index_src, int[] dst, int index_dst, int len);

    public abstract void long2byte(long[] src, int off, int len, byte[] dst, int off2);

    public abstract void byte2long(byte[] src, int index_src, long[] dst, int index_dst, int len);

    public abstract void float2byte(float[] src, int off, int len, byte[] dst, int off2);

    public abstract void byte2float(byte[] src, int index_src, float[] dst, int index_dst, int len);

    public abstract void double2byte(double[] src, int off, int len, byte[] dst, int off2);

    public abstract void byte2double(byte[] src, int index_src, double[] dst, int index_dst, int len);

    /**
     * Writes an object to a byte[].
     *
     * @param o the object
     * @return the byte array
     * @throws IOException on I/O error
     */
    public static final byte[] object2byte(Object o) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(o);
        oos.close();
        oos = null;
        return bos.toByteArray();
    }

    /**
     * Reads an object from byte[].
     *
     * @param b the byte array
     * @return the object
     * @throws IOException            on IO error
     * @throws ClassNotFoundException when the object class could not be loaded for
     *                                some reason
     */
    public static final Object byte2object(byte[] b) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(b);
        ObjectInputStream ois = new ObjectInputStream(bis);
        Object o = ois.readObject();
        ois.close();
        return o;
    }

    /**
     * Upwards-round <code>a</code> to a multiple of <code>d</code>. <code>d</code>
     * <b>MUST</b> be a power of two.
     *
     * @param a the number to be rounded
     * @param d the power of two
     * @return the rounded number
     */
    @SuppressWarnings("unused")
    public static int align(int a, int d) {
        if (false) {
            if ((d | (d - 1)) != 2 * d - 1) {
                throw new NumberFormatException("d should be a power of 2");
            }
        }

        return (a + d - 1) & ~(d - 1);
    }
}
