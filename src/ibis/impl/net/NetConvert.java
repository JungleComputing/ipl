package ibis.impl.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Provide a set of methods to convert native Java types to and from bytes.
 */
public final class NetConvert {

        /**
         * The number of bits in a single 'byte'.
         */
	public final static int	BITS_PER_BYTE = 8;

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


        /* primitive types --> bytes */

        /* Core conversion routines. */
        public static byte boolean2byte(boolean value) {
                return (byte)(value?1:0);
        }

        public static void writeBoolean(boolean value, byte[] b, int o) {
                b[o] = boolean2byte(value);
        }

        public static void writeChar(char value, byte[] b, int o) {
                b[o++] = (byte) ( value         & 0xff);
                b[o]   = (byte) ((value >>> 8)  & 0xff);
        }

        public static void writeShort(short value, byte[] b, int o) {
                b[o++] = (byte) ( value         & 0xff);
                b[o]   = (byte) ((value >> 8)  & 0xff);
        }

        public static void writeInt(int value, byte[] b, int o) {
                b[o++] = (byte) ( value         & 0xff);
                b[o++] = (byte) ((value >>  8)  & 0xff);
                b[o++] = (byte) ((value >> 16)  & 0xff);
                b[o]   = (byte) ((value >> 24)  & 0xff);
        }

        public static void writeLong(long value, byte[] b, int o) {
                b[o++] = (byte) ( value         & 0xff);
                b[o++] = (byte) ((value >>  8)  & 0xff);
                b[o++] = (byte) ((value >> 16)  & 0xff);
                b[o++] = (byte) ((value >> 24)  & 0xff);
                b[o++] = (byte) ((value >> 32)  & 0xff);
                b[o++] = (byte) ((value >> 40)  & 0xff);
                b[o++] = (byte) ((value >> 48)  & 0xff);
                b[o]   = (byte) ((value >> 56)  & 0xff);
        }

        public static void writeFloat(float value, byte[] b, int o) {
                int _value = Float.floatToIntBits(value);
                writeInt(_value, b, o);
        }

        public static void writeDouble(double value, byte[] b, int o) {
                long _value = Double.doubleToLongBits(value);
                writeLong(_value, b, o);
        }


        /* Primitive to byte array. */
        public static void writeBoolean(boolean value, byte[] b) {
                writeBoolean(value, b, 0);
        }

        public static void writeChar(char value, byte[] b) {
                writeChar(value, b, 0);
        }

        public static void writeShort(short value, byte[] b) {
                writeShort(value, b, 0);
        }

        public static void writeInt(int value, byte[] b) {
                writeInt(value, b, 0);
        }

        public static void writeLong(long value, byte[] b) {
                writeLong(value, b, 0);
        }

        public static void writeFloat(float value, byte[] b) {
                writeFloat(value, b, 0);
        }

        public static void writeDouble(double value, byte[] b) {
                writeDouble(value, b, 0);
        }


        /* Primitive to new byte array. */
        public static byte[] writeBoolean(boolean value) {
                byte [] b = new byte[1];
                writeBoolean(value, b, 0);
                return b;
        }

        public static byte[] writeChar(char value) {
                byte [] b = new byte[CHAR_SIZE];
                writeChar(value, b, 0);
                return b;
        }

        public static byte[] writeShort(short value) {
                byte [] b = new byte[SHORT_SIZE];
                writeShort(value, b, 0);
                return b;
        }

        public static byte[] writeInt(int value) {
                byte [] b = new byte[INT_SIZE];
                writeInt(value, b, 0);
                return b;
        }

        public static byte[] writeLong(long value) {
                byte [] b = new byte[LONG_SIZE];
                writeLong(value, b, 0);
                return b;
        }

        public static byte[] writeFloat(float value) {
                byte [] b = new byte[FLOAT_SIZE];
                writeFloat(value, b, 0);
                return b;
        }

        public static byte[] writeDouble(double value) {
                byte [] b = new byte[DOUBLE_SIZE];
                writeDouble(value, b, 0);
                return b;
        }


        /* Primitive sub-array to byte sub-array. */
        public static void writeArray(boolean []a, int oa, int l, byte[] b, int ob) {
                int i = 0;
                while (i < l) {
                        writeBoolean(a[oa+i], b, ob+i);
                        i++;
                }
        }

        public static void writeArray(char []a, int oa, int l, byte[] b, int ob) {
                int i = 0;
                while (i < l) {
                        writeChar(a[oa+i], b, ob+CHAR_SIZE*i);
                        i++;
                }
        }

        public static void writeArray(short []a, int oa, int l, byte[] b, int ob) {
                int i = 0;
                while (i < l) {
                        writeShort(a[oa+i], b, ob+SHORT_SIZE*i);
                        i++;
                }
        }

        public static void writeArray(int []a, int oa, int l, byte[] b, int ob) {
                int i = 0;
                while (i < l) {
                        writeInt(a[oa+i], b, ob+INT_SIZE*i);
                        i++;
                }
        }

        public static void writeArray(long []a, int oa, int l, byte[] b, int ob) {
                int i = 0;
                while (i < l) {
                        writeLong(a[oa+i], b, ob+LONG_SIZE*i);
                        i++;
                }
        }

        public static void writeArray(float []a, int oa, int l, byte[] b, int ob) {
                int i = 0;
                while (i < l) {
                        writeFloat(a[oa+i], b, ob+FLOAT_SIZE*i);
                        i++;
                }
        }

        public static void writeArray(double []a, int oa, int l, byte[] b, int ob) {
                int i = 0;
                while (i < l) {
                        writeDouble(a[oa+i], b, ob+DOUBLE_SIZE*i);
                        i++;
                }
        }


        /* Primitive sub-array to byte array. */
        public static void writeArray(boolean []a, int oa, int l, byte[] b) {
                int i = 0;
                while (i < l) {
                        writeBoolean(a[oa+i], b, i);
                        i++;
                }
        }

        public static void writeArray(char []a, int oa, int l, byte[] b) {
                int i = 0;
                while (i < l) {
                        writeChar(a[oa+i], b, CHAR_SIZE*i);
                        i++;
                }
        }

        public static void writeArray(short []a, int oa, int l, byte[] b) {
                int i = 0;
                while (i < l) {
                        writeShort(a[oa+i], b, SHORT_SIZE*i);
                        i++;
                }
        }

        public static void writeArray(int []a, int oa, int l, byte[] b) {
                int i = 0;
                while (i < l) {
                        writeInt(a[oa+i], b, INT_SIZE*i);
                        i++;
                }
        }

        public static void writeArray(long []a, int oa, int l, byte[] b) {
                int i = 0;
                while (i < l) {
                        writeLong(a[oa+i], b, LONG_SIZE*i);
                        i++;
                }
        }

        public static void writeArray(float []a, int oa, int l, byte[] b) {
                int i = 0;
                while (i < l) {
                        writeFloat(a[oa+i], b, FLOAT_SIZE*i);
                        i++;
                }
        }

        public static void writeArray(double []a, int oa, int l, byte[] b) {
                int i = 0;
                while (i < l) {
                        writeDouble(a[oa+i], b, DOUBLE_SIZE*i);
                        i++;
                }
        }


        /* Primitive array prefix to byte sub-array. */
        public static void writeArray(boolean []a, int l, byte[] b, int ob) {
                writeArray(a, 0, l, b, ob);
        }

        public static void writeArray(char []a, int l, byte[] b, int ob) {
                writeArray(a, 0, l, b, ob);
        }

        public static void writeArray(short []a, int l, byte[] b, int ob) {
                writeArray(a, 0, l, b, ob);
        }

        public static void writeArray(int []a, int l, byte[] b, int ob) {
                writeArray(a, 0, l, b, ob);
        }

        public static void writeArray(long []a, int l, byte[] b, int ob) {
                writeArray(a, 0, l, b, ob);
        }

        public static void writeArray(float []a, int l, byte[] b, int ob) {
                writeArray(a, 0, l, b, ob);
        }

        public static void writeArray(double []a, int l, byte[] b, int ob) {
                writeArray(a, 0, l, b, ob);
        }


        /* Primitive array prefix to byte array. */
        public static void writeArray(boolean []a, int l, byte[] b) {
                writeArray(a, l, b, 0);
        }

        public static void writeArray(char []a, int l, byte[] b) {
                writeArray(a, l, b, 0);
        }

        public static void writeArray(short []a, int l, byte[] b) {
                writeArray(a, l, b, 0);
        }

        public static void writeArray(int []a, int l, byte[] b) {
                writeArray(a, l, b, 0);
        }

        public static void writeArray(long []a, int l, byte[] b) {
                writeArray(a, l, b, 0);
        }

        public static void writeArray(float []a, int l, byte[] b) {
                writeArray(a, l, b, 0);
        }

        public static void writeArray(double []a, int l, byte[] b) {
                writeArray(a, l, b, 0);
        }


        /* Primitive array to byte sub-array. */
        public static void writeArray(boolean []a, byte[] b, int ob) {
                writeArray(a, a.length, b, ob);
        }

        public static void writeArray(char []a, byte[] b, int ob) {
                writeArray(a, a.length, b, ob);
        }

        public static void writeArray(short []a, byte[] b, int ob) {
                writeArray(a, a.length, b, ob);
        }

        public static void writeArray(int []a, byte[] b, int ob) {
                writeArray(a, a.length, b, ob);
        }

        public static void writeArray(long []a, byte[] b, int ob) {
                writeArray(a, a.length, b, ob);
        }

        public static void writeArray(float []a, byte[] b, int ob) {
                writeArray(a, a.length, b, ob);
        }

        public static void writeArray(double []a, byte[] b, int ob) {
                writeArray(a, a.length, b, ob);
        }


        /* Primitive array to byte array. */
        public static void writeArray(boolean []a, byte[] b) {
                writeArray(a, a.length, b, 0);
        }

        public static void writeArray(char []a, byte[] b) {
                writeArray(a, a.length, b, 0);
        }

        public static void writeArray(short []a, byte[] b) {
                writeArray(a, a.length, b, 0);
        }

        public static void writeArray(int []a, byte[] b) {
                writeArray(a, a.length, b, 0);
        }

        public static void writeArray(long []a, byte[] b) {
                writeArray(a, a.length, b, 0);
        }

        public static void writeArray(float []a, byte[] b) {
                writeArray(a, a.length, b, 0);
        }

        public static void writeArray(double []a, byte[] b) {
                writeArray(a, a.length, b, 0);
        }


        /* Primitive sub-array to new byte array. */
        public static byte [] writeArray(boolean []a, int oa, int l) {
                byte [] b = new byte[l];
                writeArray(a, oa, l, b, 0);
                return b;
	}

        public static byte [] writeArray(char []a, int oa, int l) {
                byte [] b = new byte[l*CHAR_SIZE];
                writeArray(a, oa, l, b, 0);
                return b;
	}

        public static byte [] writeArray(short []a, int oa, int l) {
                byte [] b = new byte[l*SHORT_SIZE];
                writeArray(a, oa, l, b, 0);
                return b;
	}

        public static byte [] writeArray(int []a, int oa, int l) {
                byte [] b = new byte[l*INT_SIZE];
                writeArray(a, oa, l, b, 0);
                return b;
	}

        public static byte [] writeArray(long []a, int oa, int l) {
                byte [] b = new byte[l*LONG_SIZE];
                writeArray(a, oa, l, b, 0);
                return b;
	}

        public static byte [] writeArray(float []a, int oa, int l) {
                byte [] b = new byte[l*FLOAT_SIZE];
                writeArray(a, oa, l, b, 0);
                return b;
	}

        public static byte [] writeArray(double []a, int oa, int l) {
                byte [] b = new byte[l*DOUBLE_SIZE];
                writeArray(a, oa, l, b, 0);
                return b;
	}


        /* Primitive array prefix to new byte array. */
        public static byte [] writeArray(boolean []a, int l) {
                return writeArray(a, 0, l);
	}

        public static byte [] writeArray(char []a, int l) {
                return writeArray(a, 0, l);
	}

        public static byte [] writeArray(short []a, int l) {
                return writeArray(a, 0, l);
	}

        public static byte [] writeArray(int []a, int l) {
                return writeArray(a, 0, l);
	}

        public static byte [] writeArray(long []a, int l) {
                return writeArray(a, 0, l);
	}

        public static byte [] writeArray(float []a, int l) {
                return writeArray(a, 0, l);
	}

        public static byte [] writeArray(double []a, int l) {
                return writeArray(a, 0, l);
	}


        /* Primitive array to new byte array. */
        public static byte [] writeArray(boolean []a) {
                return writeArray(a, 0, a.length);
	}

        public static byte [] writeArray(char []a) {
                return writeArray(a, 0, a.length);
	}

        public static byte [] writeArray(short []a) {
                return writeArray(a, 0, a.length);
	}

        public static byte [] writeArray(int []a) {
                return writeArray(a, 0, a.length);
	}

        public static byte [] writeArray(long []a) {
                return writeArray(a, 0, a.length);
	}

        public static byte [] writeArray(float []a) {
                return writeArray(a, 0, a.length);
	}

        public static byte [] writeArray(double []a) {
                return writeArray(a, 0, a.length);
	}


        /* Primitive sub-array to new byte array with offset. */
        public static byte [] writeArray(int ob, boolean []a, int oa, int l) {
                byte [] b = new byte[l+ob];
                writeArray(a, oa, l, b, ob);
                return b;
	}

        public static byte [] writeArray(int ob, char []a, int oa, int l) {
                byte [] b = new byte[CHAR_SIZE*l+ob];
                writeArray(a, oa, l, b, ob);
                return b;
	}

        public static byte [] writeArray(int ob, short []a, int oa, int l) {
                byte [] b = new byte[SHORT_SIZE*l+ob];
                writeArray(a, oa, l, b, ob);
                return b;
	}

        public static byte [] writeArray(int ob, int []a, int oa, int l) {
                byte [] b = new byte[INT_SIZE*l+ob];
                writeArray(a, oa, l, b, ob);
                return b;
	}

        public static byte [] writeArray(int ob, long []a, int oa, int l) {
                byte [] b = new byte[LONG_SIZE*l+ob];
                writeArray(a, oa, l, b, ob);
                return b;
	}

        public static byte [] writeArray(int ob, float []a, int oa, int l) {
                byte [] b = new byte[FLOAT_SIZE*l+ob];
                writeArray(a, oa, l, b, ob);
                return b;
	}

        public static byte [] writeArray(int ob, double []a, int oa, int l) {
                byte [] b = new byte[DOUBLE_SIZE*l+ob];
                writeArray(a, oa, l, b, ob);
                return b;
	}


        /* Primitive array prefix to new byte array with offset. */
        public static byte [] writeArray(int ob, boolean []a, int l) {
                return writeArray(ob, a, 0, l);
	}

        public static byte [] writeArray(int ob, char []a, int l) {
                return writeArray(ob, a, 0, l);
	}

        public static byte [] writeArray(int ob, short []a, int l) {
                return writeArray(ob, a, 0, l);
	}

        public static byte [] writeArray(int ob, int []a, int l) {
                return writeArray(ob, a, 0, l);
	}

        public static byte [] writeArray(int ob, long []a, int l) {
                return writeArray(ob, a, 0, l);
	}

        public static byte [] writeArray(int ob, float []a, int l) {
                return writeArray(ob, a, 0, l);
	}

        public static byte [] writeArray(int ob, double []a, int l) {
                return writeArray(ob, a, 0, l);
	}


        /* Primitive array to new byte array with offset. */
        public static byte [] writeArray(int ob, boolean []a) {
                return writeArray(ob, a, 0, a.length);
	}

        public static byte [] writeArray(int ob, char []a) {
                return writeArray(ob, a, 0, a.length);
	}

        public static byte [] writeArray(int ob, short []a) {
                return writeArray(ob, a, 0, a.length);
	}

        public static byte [] writeArray(int ob, int []a) {
                return writeArray(ob, a, 0, a.length);
	}

        public static byte [] writeArray(int ob, long []a) {
                return writeArray(ob, a, 0, a.length);
	}

        public static byte [] writeArray(int ob, float []a) {
                return writeArray(ob, a, 0, a.length);
	}

        public static byte [] writeArray(int ob, double []a) {
                return writeArray(ob, a, 0, a.length);
	}


        /* Primitive sub-array to new byte array with offset and custom length. */
        public static byte [] writeArray(int ob, int lb, boolean []a, int oa, int l) {
                byte [] b = new byte[lb];
                writeArray(a, oa, l, b, ob);
                return b;
	}

        public static byte [] writeArray(int ob, int lb, char []a, int oa, int l) {
                byte [] b = new byte[lb];
                writeArray(a, oa, l, b, ob);
                return b;
	}

        public static byte [] writeArray(int ob, int lb, short []a, int oa, int l) {
                byte [] b = new byte[lb];
                writeArray(a, oa, l, b, ob);
                return b;
	}

        public static byte [] writeArray(int ob, int lb, int []a, int oa, int l) {
                byte [] b = new byte[lb];
                writeArray(a, oa, l, b, ob);
                return b;
	}

        public static byte [] writeArray(int ob, int lb, long []a, int oa, int l) {
                byte [] b = new byte[lb];
                writeArray(a, oa, l, b, ob);
                return b;
	}

        public static byte [] writeArray(int ob, int lb, float []a, int oa, int l) {
                byte [] b = new byte[lb];
                writeArray(a, oa, l, b, ob);
                return b;
	}

        public static byte [] writeArray(int ob, int lb, double []a, int oa, int l) {
                byte [] b = new byte[lb];
                writeArray(a, oa, l, b, ob);
                return b;
	}


        /* Primitive array prefix to new byte array with offset and custom length. */
        public static byte [] writeArray(int ob, int lb, boolean []a, int l) {
                return writeArray(ob, lb, a, 0, l);
	}

        public static byte [] writeArray(int ob, int lb, char []a, int l) {
                return writeArray(ob, lb, a, 0, l);
	}

        public static byte [] writeArray(int ob, int lb, short []a, int l) {
                return writeArray(ob, lb, a, 0, l);
	}

        public static byte [] writeArray(int ob, int lb, int []a, int l) {
                return writeArray(ob, lb, a, 0, l);
	}

        public static byte [] writeArray(int ob, int lb, long []a, int l) {
                return writeArray(ob, lb, a, 0, l);
	}

        public static byte [] writeArray(int ob, int lb, float []a, int l) {
                return writeArray(ob, lb, a, 0, l);
	}

        public static byte [] writeArray(int ob, int lb, double []a, int l) {
                return writeArray(ob, lb, a, 0, l);
	}


        /* Primitive array to new byte array with offset and custom length. */
        public static byte [] writeArray(int ob, int lb, boolean []a) {
                return writeArray(ob, lb, a, 0, a.length);
	}

        public static byte [] writeArray(int ob, int lb, char []a) {
                return writeArray(ob, lb, a, 0, a.length);
	}

        public static byte [] writeArray(int ob, int lb, short []a) {
                return writeArray(ob, lb, a, 0, a.length);
	}

        public static byte [] writeArray(int ob, int lb, int []a) {
                return writeArray(ob, lb, a, 0, a.length);
	}

        public static byte [] writeArray(int ob, int lb, long []a) {
                return writeArray(ob, lb, a, 0, a.length);
	}

        public static byte [] writeArray(int ob, int lb, float []a) {
                return writeArray(ob, lb, a, 0, a.length);
	}

        public static byte [] writeArray(int ob, int lb, double []a) {
                return writeArray(ob, lb, a, 0, a.length);
	}



        /* primitive types <-- bytes */

        /* Core conversion routines. */
        public static boolean byte2boolean(byte b) {
                char value = 0;
                return (b != 0);
        }

        public static boolean readBoolean(byte[] b, int o) {
                return byte2boolean(b[o]);
        }

        public static char readChar(byte[] b, int o) {
                char value = 0;
                value  = (char)((b[o++] & 0xFF)     );
                value |= (char)((b[o]   & 0xFF) << 8);
                return value;
        }

        public static short readShort(byte[] b, int o) {
                short value = 0;
                value  = (short)((b[o++] & 0xFF)     );
                value |= (short)((b[o]   & 0xFF) << 8);
                return value;
       }

        public static int readInt(byte[] b, int o) {
                int value = 0;
                value  =  ((int)b[o++]) & 0xFF;
                value |= (((int)b[o++]) & 0xFF) <<  8;
                value |= (((int)b[o++]) & 0xFF) << 16;
                value |= (((int)b[o]  ) & 0xFF) << 24;
                return value;
        }

        public static long readLong(byte[] b, int o) {
                long value = 0;
                value  =  ((long)b[o++]) & 0xFF;
                value |= (((long)b[o++]) & 0xFF) <<  8;
                value |= (((long)b[o++]) & 0xFF) << 16;
                value |= (((long)b[o++]) & 0xFF) << 24;
                value |= (((long)b[o++]) & 0xFF) << 32;
                value |= (((long)b[o++]) & 0xFF) << 40;
                value |= (((long)b[o++]) & 0xFF) << 48;
                value |= (((long)b[o]  ) & 0xFF) << 56;
                return value;
        }

        public static float readFloat(byte[] b, int o) {
                int   _value = readInt(b, o);
                float value  = Float.intBitsToFloat(_value);
                return value;
        }

        public static double readDouble(byte[] b, int o) {
                long   _value = readLong(b, o);
                double value  = Double.longBitsToDouble(_value);
                return value;
        }


        /* Byte array to primitive. */
        public static boolean readBoolean(byte[] b) {
                return readBoolean(b, 0);
        }

        public static char readChar(byte[] b) {
                return readChar(b, 0);
        }

        public static short readShort(byte[] b) {
                return readShort(b, 0);
        }

        public static int readInt(byte[] b) {
                return readInt(b, 0);
        }

        public static long readLong(byte[] b) {
                return readLong(b, 0);
        }

        public static float readFloat(byte[] b) {
                return readFloat(b, 0);
        }

        public static double readDouble(byte[] b) {
                return readDouble(b, 0);
        }


        /* Byte sub-array to primitive sub-array */
        public static void readArray(byte[] b, int ob, boolean []a, int oa, int l) {
                int i = 0;
                while (i < l) {
                        a[oa+i] = readBoolean(b, ob+i);
                        i++;
                }
        }

        public static void readArray(byte[] b, int ob, char []a, int oa, int l) {
                int i = 0;
                while (i < l) {
                        a[oa+i] = readChar(b, ob+CHAR_SIZE*i);
                        i++;
                }
        }

        public static void readArray(byte[] b, int ob, short []a, int oa, int l) {
                int i = 0;
                while (i < l) {
                        a[oa+i] = readShort(b, ob+SHORT_SIZE*i);
                        i++;
                }
        }

        public static void readArray(byte[] b, int ob, int []a, int oa, int l) {
                int i = 0;
                while (i < l) {
                        a[oa+i] = readInt(b, ob+INT_SIZE*i);
                        i++;
                }
        }

        public static void readArray(byte[] b, int ob, long []a, int oa, int l) {
                int i = 0;
                while (i < l) {
                        a[oa+i] = readLong(b, ob+LONG_SIZE*i);
                        i++;
                }
        }

        public static void readArray(byte[] b, int ob, float []a, int oa, int l) {
                int i = 0;
                while (i < l) {
                        a[oa+i] = readFloat(b, ob+FLOAT_SIZE*i);
                        i++;
                }
        }

        public static void readArray(byte[] b, int ob, double []a, int oa, int l) {
                int i = 0;
                while (i < l) {
                        a[oa+i] = readDouble(b, ob+DOUBLE_SIZE*i);
                        i++;
                }
        }


        /* Byte array to primitive sub-array */
        public static void readArray(byte[] b, boolean []a, int oa, int l) {
                readArray(b, 0, a, oa, l);
        }

        public static void readArray(byte[] b, char []a, int oa, int l) {
                readArray(b, 0, a, oa, l);
        }

        public static void readArray(byte[] b, short []a, int oa, int l) {
                readArray(b, 0, a, oa, l);
        }

        public static void readArray(byte[] b, int []a, int oa, int l) {
                readArray(b, 0, a, oa, l);
        }

        public static void readArray(byte[] b, long []a, int oa, int l) {
                readArray(b, 0, a, oa, l);
        }

        public static void readArray(byte[] b, float []a, int oa, int l) {
                readArray(b, 0, a, oa, l);
        }

        public static void readArray(byte[] b, double []a, int oa, int l) {
                readArray(b, 0, a, oa, l);
        }


        /* Byte sub-array to primitive array prefix */
        public static void readArray(byte[] b, int ob, boolean []a, int l) {
                readArray(b, ob, a, 0, l);
        }

        public static void readArray(byte[] b, int ob, char []a, int l) {
                readArray(b, ob, a, 0, l);
        }

        public static void readArray(byte[] b, int ob, short []a, int l) {
                readArray(b, ob, a, 0, l);
        }

        public static void readArray(byte[] b, int ob, int []a, int l) {
                readArray(b, ob, a, 0, l);
        }

        public static void readArray(byte[] b, int ob, long []a, int l) {
                readArray(b, ob, a, 0, l);
        }

        public static void readArray(byte[] b, int ob, float []a, int l) {
                readArray(b, ob, a, 0, l);
        }

        public static void readArray(byte[] b, int ob, double []a, int l) {
                readArray(b, ob, a, 0, l);
        }


        /* Byte array to primitive array prefix */
        public static void readArray(byte[] b, boolean []a, int l) {
                readArray(b, 0, a, l);
        }

        public static void readArray(byte[] b, char []a, int l) {
                readArray(b, 0, a, l);
        }

        public static void readArray(byte[] b, short []a, int l) {
                readArray(b, 0, a, l);
        }

        public static void readArray(byte[] b, int []a, int l) {
                readArray(b, 0, a, l);
        }

        public static void readArray(byte[] b, long []a, int l) {
                readArray(b, 0, a, l);
        }

        public static void readArray(byte[] b, float []a, int l) {
                readArray(b, 0, a, l);
        }

        public static void readArray(byte[] b, double []a, int l) {
                readArray(b, 0, a, l);
        }


        /* Byte sub-array to primitive array. */
        public static void readArray(byte[] b, int ob, boolean []a) {
                readArray(b, ob, a, a.length);
        }

        public static void readArray(byte[] b, int ob, char []a) {
                readArray(b, ob, a, a.length);
        }

        public static void readArray(byte[] b, int ob, short []a) {
                readArray(b, ob, a, a.length);
        }

        public static void readArray(byte[] b, int ob, int []a) {
                readArray(b, ob, a, a.length);
        }

        public static void readArray(byte[] b, int ob, long []a) {
                readArray(b, ob, a, a.length);
        }

        public static void readArray(byte[] b, int ob, float []a) {
                readArray(b, ob, a, a.length);
        }

        public static void readArray(byte[] b, int ob, double []a) {
                readArray(b, ob, a, a.length);
        }


        /* Byte array to primitive array. */
        public static void readArray(byte[] b, boolean []a) {
                readArray(b, 0, a);
        }

        public static void readArray(byte[] b, char []a) {
                readArray(b, 0, a);
        }

        public static void readArray(byte[] b, short []a) {
                readArray(b, 0, a);
        }

        public static void readArray(byte[] b, int []a) {
                readArray(b, 0, a);
        }

        public static void readArray(byte[] b, long []a) {
                readArray(b, 0, a);
        }

        public static void readArray(byte[] b, float []a) {
                readArray(b, 0, a);
        }

        public static void readArray(byte[] b, double []a) {
                readArray(b, 0, a);
        }


        /* Byte sub-array to new primitive array with custom offset, length and copylength. */
        public static boolean[] readArrayBoolean(int oa, int la, byte[] b, int ob, int l) {
                boolean [] a = new boolean[la];
                readArray(b, ob, a, oa, l);
                return a;
        }

        public static char[] readArrayChar(int oa, int la, byte[] b, int ob, int l) {
                char [] a = new char[la];
                readArray(b, ob, a, oa, l);
                return a;
        }

        public static short[] readArrayShort(int oa, int la, byte[] b, int ob, int l) {
                short [] a = new short[la];
                readArray(b, ob, a, oa, l);
                return a;
        }

        public static int[] readArrayInt(int oa, int la, byte[] b, int ob, int l) {
                int [] a = new int[la];
                readArray(b, ob, a, oa, l);
                return a;
        }

        public static long[] readArrayLong(int oa, int la, byte[] b, int ob, int l) {
                long [] a = new long[la];
                readArray(b, ob, a, oa, l);
                return a;
        }

        public static float[] readArrayFloat(int oa, int la, byte[] b, int ob, int l) {
                float [] a = new float[la];
                readArray(b, ob, a, oa, l);
                return a;
        }

        public static double[] readArrayDouble(int oa, int la, byte[] b, int ob, int l) {
                double [] a = new double[la];
                readArray(b, ob, a, oa, l);
                return a;
        }


        /* Byte sub-array to new primitive array with custom offset and copylength. */
        public static boolean[] readArrayBoolean(int oa, byte[] b, int ob, int l) {
                int la = oa+l;
                return readArrayBoolean(oa, la, b, ob, l);
        }

        public static char[] readArrayChar(int oa, byte[] b, int ob, int l) {
                int la = oa+l;
                return readArrayChar(oa, la, b, ob, l);
        }

        public static short[] readArrayShort(int oa, byte[] b, int ob, int l) {
                int la = oa+l;
                return readArrayShort(oa, la, b, ob, l);
        }

        public static int[] readArrayInt(int oa, byte[] b, int ob, int l) {
                int la = oa+l;
                return readArrayInt(oa, la, b, ob, l);
        }

        public static long[] readArrayLong(int oa, byte[] b, int ob, int l) {
                int la = oa+l;
                return readArrayLong(oa, la, b, ob, l);
        }

        public static float[] readArrayFloat(int oa, byte[] b, int ob, int l) {
                int la = oa+l;
                return readArrayFloat(oa, la, b, ob, l);
        }

        public static double[] readArrayDouble(int oa, byte[] b, int ob, int l) {
                int la = oa+l;
                return readArrayDouble(oa, la, b, ob, l);
        }


        /* Byte sub-array to new primitive array with custom offset. */
        public static boolean[] readArrayBoolean(int oa, byte[] b, int ob) {
                int la = (b.length-ob);
                return readArrayBoolean(oa, la, b, ob, la);
        }

        public static char[] readArrayChar(int oa, byte[] b, int ob) {
                int la = (b.length-ob)/CHAR_SIZE;
                return readArrayChar(oa, la, b, ob, la);
        }

        public static short[] readArrayShort(int oa, byte[] b, int ob) {
                int la = (b.length-ob)/SHORT_SIZE;
                return readArrayShort(oa, la, b, ob, la);
        }

        public static int[] readArrayInt(int oa, byte[] b, int ob) {
                int la = (b.length-ob)/INT_SIZE;
                return readArrayInt(oa, la, b, ob, la);
        }

        public static long[] readArrayLong(int oa, byte[] b, int ob) {
                int la = (b.length-ob)/LONG_SIZE;
                return readArrayLong(oa, la, b, ob, la);
        }

        public static float[] readArrayFloat(int oa, byte[] b, int ob) {
                int la = (b.length-ob)/FLOAT_SIZE;
                return readArrayFloat(oa, la, b, ob, la);
        }

        public static double[] readArrayDouble(int oa, byte[] b, int ob) {
                int la = (b.length-ob)/DOUBLE_SIZE;
                return readArrayDouble(oa, la, b, ob, la);
        }


        /* Byte sub-array to new primitive array with custom copylength. */
        public static boolean[] readArrayBoolean(byte[] b, int ob, int l) {
                return readArrayBoolean(0, l, b, ob, l);
        }

        public static char[] readArrayChar(byte[] b, int ob, int l) {
                return readArrayChar(0, l, b, ob, l);
        }

        public static short[] readArrayShort(byte[] b, int ob, int l) {
                return readArrayShort(0, l, b, ob, l);
        }

        public static int[] readArrayInt(byte[] b, int ob, int l) {
                return readArrayInt(0, l, b, ob, l);
        }

        public static long[] readArrayLong(byte[] b, int ob, int l) {
                return readArrayLong(0, l, b, ob, l);
        }

        public static float[] readArrayFloat(byte[] b, int ob, int l) {
                return readArrayFloat(0, l, b, ob, l);
        }

        public static double[] readArrayDouble(byte[] b, int ob, int l) {
                return readArrayDouble(0, l, b, ob, l);
        }


        /* Byte sub-array to new primitive array. */
        public static boolean[] readArrayBoolean(byte[] b, int ob) {
                return readArrayBoolean(0, b, ob);
        }

        public static char[] readArrayChar(byte[] b, int ob) {
                return readArrayChar(0, b, ob);
        }

        public static short[] readArrayShort(byte[] b, int ob) {
                return readArrayShort(0, b, ob);
        }

        public static int[] readArrayInt(byte[] b, int ob) {
                return readArrayInt(0, b, ob);
        }

        public static long[] readArrayLong(byte[] b, int ob) {
                return readArrayLong(0, b, ob);
        }

        public static float[] readArrayFloat(byte[] b, int ob) {
                return readArrayFloat(0, b, ob);
        }

        public static double[] readArrayDouble(byte[] b, int ob) {
                return readArrayDouble(0, b, ob);
        }


        /* Byte array to new primitive array with custom offset, length and copylength. */
        public static boolean[] readArrayBoolean(int oa, int la, byte[] b, int l) {
                boolean [] a = new boolean[la];
                readArray(b, 0, a, oa, l);
                return a;
        }

        public static char[] readArrayChar(int oa, int la, byte[] b, int l) {
                char [] a = new char[la];
                readArray(b, 0, a, oa, l);
                return a;
        }

        public static short[] readArrayShort(int oa, int la, byte[] b, int l) {
                short [] a = new short[la];
                readArray(b, 0, a, oa, l);
                return a;
        }

        public static int[] readArrayInt(int oa, int la, byte[] b, int l) {
                int [] a = new int[la];
                readArray(b, 0, a, oa, l);
                return a;
        }

        public static long[] readArrayLong(int oa, int la, byte[] b, int l) {
                long [] a = new long[la];
                readArray(b, 0, a, oa, l);
                return a;
        }

        public static float[] readArrayFloat(int oa, int la, byte[] b, int l) {
                float [] a = new float[la];
                readArray(b, 0, a, oa, l);
                return a;
        }

        public static double[] readArrayDouble(int oa, int la, byte[] b, int l) {
                double [] a = new double[la];
                readArray(b, 0, a, oa, l);
                return a;
        }


        /* Byte array to new primitive array with custom offset. */
        public static boolean[] readArrayBoolean(int oa, byte[] b) {
                int la = b.length;
                return readArrayBoolean(oa, la, b, la);
        }

        public static char[] readArrayChar(int oa, byte[] b) {
                int la = b.length/CHAR_SIZE;
                return readArrayChar(oa, la, b, la);
        }

        public static short[] readArrayShort(int oa, byte[] b) {
                int la = b.length/SHORT_SIZE;
                return readArrayShort(oa, la, b, la);
        }

        public static int[] readArrayInt(int oa, byte[] b) {
                int la = b.length/INT_SIZE;
                return readArrayInt(oa, la, b, la);
        }

        public static long[] readArrayLong(int oa, byte[] b) {
                int la = b.length/LONG_SIZE;
                return readArrayLong(oa, la, b, la);
        }

        public static float[] readArrayFloat(int oa, byte[] b) {
                int la = b.length/FLOAT_SIZE;
                return readArrayFloat(oa, la, b, la);
        }

        public static double[] readArrayDouble(int oa, byte[] b) {
                int la = b.length/DOUBLE_SIZE;
                return readArrayDouble(oa, la, b, la);
        }


        /* Byte array to new primitive array. */
        public static boolean[] readArrayBoolean(byte[] b) {
                return readArrayBoolean(0, b);
        }

        public static char[] readArrayChar(byte[] b) {
                return readArrayChar(0, b);
        }

        public static short[] readArrayShort(byte[] b) {
                return readArrayShort(0, b);
        }

        public static int[] readArrayInt(byte[] b) {
                return readArrayInt(0, b);
        }

        public static long[] readArrayLong(byte[] b) {
                return readArrayLong(0, b);
        }

        public static float[] readArrayFloat(byte[] b) {
                return readArrayFloat(0, b);
        }

        public static double[] readArrayDouble(byte[] b) {
                return readArrayDouble(0, b);
        }


        /* Object to/from byte array */
        public static byte[] object2bytes(Object o) throws IOException {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream    oos = new ObjectOutputStream(bos);
                oos.writeObject(o);
                oos.close();
                oos = null;
                return bos.toByteArray();
        }

        public static Object bytes2object(byte [] b) throws IOException, ClassNotFoundException {
                ByteArrayInputStream bis = new ByteArrayInputStream(b);
                ObjectInputStream    ois = new ObjectInputStream(bis);
                Object               o   = ois.readObject();
                ois.close();
                return o;
        }

}
