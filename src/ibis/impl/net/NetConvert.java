package ibis.ipl.impl.net;

import ibis.ipl.IbisIOException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


public final  class NetConvert {

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
                byte [] b = new byte[2];
                writeChar(value, b, 0);
                return b;
        }
        
        public static byte[] writeShort(short value) {
                byte [] b = new byte[2];
                writeShort(value, b, 0);
                return b;
        }
        
        public static byte[] writeInt(int value) {
                byte [] b = new byte[4];
                writeInt(value, b, 0);
                return b;
        }
        
        public static byte[] writeLong(long value) {
                byte [] b = new byte[8];
                writeLong(value, b, 0);
                return b;
        }
        
        public static byte[] writeFloat(float value) {
                byte [] b = new byte[4];
                writeFloat(value, b, 0);
                return b;
        }
        
        public static byte[] writeDouble(double value) {
                byte [] b = new byte[8];
                writeDouble(value, b, 0);
                return b;
        }


        /* Primitive sub-array to byte sub-array. */
        public static void writeSubArrayBoolean(boolean []a, int oa, int l, byte[] b, int ob) {
                int i = 0;
                while (i < l) {
                        writeBoolean(a[oa+i], b, ob+i);
                        i++;
                }
        }
        
        public static void writeSubArrayChar(char []a, int oa, int l, byte[] b, int ob) {
                int i = 0;
                while (i < l) {
                        writeChar(a[oa+i], b, ob+2*i);
                        i++;
                }
        }
        
        public static void writeSubArrayShort(short []a, int oa, int l, byte[] b, int ob) {
                int i = 0;
                while (i < l) {
                        writeShort(a[oa+i], b, ob+2*i);
                        i++;
                }
        }
        
        public static void writeSubArrayInt(int []a, int oa, int l, byte[] b, int ob) {
                int i = 0;
                while (i < l) {
                        writeInt(a[oa+i], b, ob+4*i);
                        i++;
                }
        }
        
        public static void writeSubArrayLong(long []a, int oa, int l, byte[] b, int ob) {
                int i = 0;
                while (i < l) {
                        writeLong(a[oa+i], b, ob+8*i);
                        i++;
                }
        }
        
        public static void writeSubArrayFloat(float []a, int oa, int l, byte[] b, int ob) {
                int i = 0;
                while (i < l) {
                        writeFloat(a[oa+i], b, ob+4*i);
                        i++;
                }
        }
        
        public static void writeSubArrayDouble(double []a, int oa, int l, byte[] b, int ob) {
                int i = 0;
                while (i < l) {
                        writeDouble(a[oa+i], b, ob+8*i);
                        i++;
                }
        }
        

        /* Primitive sub-array to byte array. */
        public static void writeSubArrayBoolean(boolean []a, int oa, int l, byte[] b) {
                int i = 0;
                while (i < l) {
                        writeBoolean(a[oa+i], b, i);
                        i++;
                }
        }
        
        public static void writeSubArrayChar(char []a, int oa, int l, byte[] b) {
                int i = 0;
                while (i < l) {
                        writeChar(a[oa+i], b, 2*i);
                        i++;
                }
        }
        
        public static void writeSubArrayShort(short []a, int oa, int l, byte[] b) {
                int i = 0;
                while (i < l) {
                        writeShort(a[oa+i], b, 2*i);
                        i++;
                }
        }
        
        public static void writeSubArrayInt(int []a, int oa, int l, byte[] b) {
                int i = 0;
                while (i < l) {
                        writeInt(a[oa+i], b, 4*i);
                        i++;
                }
        }
        
        public static void writeSubArrayLong(long []a, int oa, int l, byte[] b) {
                int i = 0;
                while (i < l) {
                        writeLong(a[oa+i], b, 8*i);
                        i++;
                }
        }
        
        public static void writeSubArrayFloat(float []a, int oa, int l, byte[] b) {
                int i = 0;
                while (i < l) {
                        writeFloat(a[oa+i], b, 4*i);
                        i++;
                }
        }
        
        public static void writeSubArrayDouble(double []a, int oa, int l, byte[] b) {
                int i = 0;
                while (i < l) {
                        writeDouble(a[oa+i], b, 8*i);
                        i++;
                }
        }
        

        /* Primitive array prefix to byte sub-array. */
        public static void writeArrayBoolean(boolean []a, int l, byte[] b, int ob) {
                writeSubArrayBoolean(a, 0, l, b, ob);
        }
        
        public static void writeArrayChar(char []a, int l, byte[] b, int ob) {
                writeSubArrayChar(a, 0, l, b, ob);
        }
        
        public static void writeArrayShort(short []a, int l, byte[] b, int ob) {
                writeSubArrayShort(a, 0, l, b, ob);
        }
        
        public static void writeArrayInt(int []a, int l, byte[] b, int ob) {
                writeSubArrayInt(a, 0, l, b, ob);
        }
        
        public static void writeArrayLong(long []a, int l, byte[] b, int ob) {
                writeSubArrayLong(a, 0, l, b, ob);
        }
        
        public static void writeArrayFloat(float []a, int l, byte[] b, int ob) {
                writeSubArrayFloat(a, 0, l, b, ob);
        }
        
        public static void writeArrayDouble(double []a, int l, byte[] b, int ob) {
                writeSubArrayDouble(a, 0, l, b, ob);
        }
        

        /* Primitive array prefix to byte array. */
        public static void writeArrayBoolean(boolean []a, int l, byte[] b) {
                writeArrayBoolean(a, l, b, 0);
        }
        
        public static void writeArrayChar(char []a, int l, byte[] b) {
                writeArrayChar(a, l, b, 0);
        }
        
        public static void writeArrayShort(short []a, int l, byte[] b) {
                writeArrayShort(a, l, b, 0);
        }
        
        public static void writeArrayInt(int []a, int l, byte[] b) {
                writeArrayInt(a, l, b, 0);
        }
        
        public static void writeArrayLong(long []a, int l, byte[] b) {
                writeArrayLong(a, l, b, 0);
        }
        
        public static void writeArrayFloat(float []a, int l, byte[] b) {
                writeArrayFloat(a, l, b, 0);
        }
        
        public static void writeArrayDouble(double []a, int l, byte[] b) {
                writeArrayDouble(a, l, b, 0);
        }
        

        /* Primitive array to byte sub-array. */
        public static void writeArrayBoolean(boolean []a, byte[] b, int ob) {
                writeArrayBoolean(a, a.length, b, ob);
        }
        
        public static void writeArrayChar(char []a, byte[] b, int ob) {
                writeArrayChar(a, a.length, b, ob);
        }
        
        public static void writeArrayShort(short []a, byte[] b, int ob) {
                writeArrayShort(a, a.length, b, ob);
        }
        
        public static void writeArrayInt(int []a, byte[] b, int ob) {
                writeArrayInt(a, a.length, b, ob);
        }
        
        public static void writeArrayLong(long []a, byte[] b, int ob) {
                writeArrayLong(a, a.length, b, ob);
        }
        
        public static void writeArrayFloat(float []a, byte[] b, int ob) {
                writeArrayFloat(a, a.length, b, ob);
        }
        
        public static void writeArrayDouble(double []a, byte[] b, int ob) {
                writeArrayDouble(a, a.length, b, ob);
        }
        

        /* Primitive array to byte array. */
        public static void writeArrayBoolean(boolean []a, byte[] b) {
                writeArrayBoolean(a, a.length, b, 0);
        }
        
        public static void writeArrayChar(char []a, byte[] b) {
                writeArrayChar(a, a.length, b, 0);
        }
        
        public static void writeArrayShort(short []a, byte[] b) {
                writeArrayShort(a, a.length, b, 0);
        }
        
        public static void writeArrayInt(int []a, byte[] b) {
                writeArrayInt(a, a.length, b, 0);
        }
        
        public static void writeArrayLong(long []a, byte[] b) {
                writeArrayLong(a, a.length, b, 0);
        }
        
        public static void writeArrayFloat(float []a, byte[] b) {
                writeArrayFloat(a, a.length, b, 0);
        }
        
        public static void writeArrayDouble(double []a, byte[] b) {
                writeArrayDouble(a, a.length, b, 0);
        }


        /* Primitive sub-array to new byte array. */
        public static byte [] writeSubArrayBoolean(boolean []a, int oa, int l) {
                byte [] b = new byte[l];
                writeSubArrayBoolean(a, oa, l, b, 0);
                return b;
	}
        
        public static byte [] writeSubArrayChar(char []a, int oa, int l) {
                byte [] b = new byte[l];
                writeSubArrayChar(a, oa, l, b, 0);
                return b;
	}
        
        public static byte [] writeSubArrayShort(short []a, int oa, int l) {
                byte [] b = new byte[l];
                writeSubArrayShort(a, oa, l, b, 0);
                return b;
	}
        
        public static byte [] writeSubArrayInt(int []a, int oa, int l) {
                byte [] b = new byte[l];
                writeSubArrayInt(a, oa, l, b, 0);
                return b;
	}
        
        public static byte [] writeSubArrayLong(long []a, int oa, int l) {
                byte [] b = new byte[l];
                writeSubArrayLong(a, oa, l, b, 0);
                return b;
	}
        
        public static byte [] writeSubArrayFloat(float []a, int oa, int l) {
                byte [] b = new byte[l];
                writeSubArrayFloat(a, oa, l, b, 0);
                return b;
	}
        
        public static byte [] writeSubArrayDouble(double []a, int oa, int l) {
                byte [] b = new byte[l];
                writeSubArrayDouble(a, oa, l, b, 0);
                return b;
	}        


        /* Primitive array prefix to new byte array. */
        public static byte [] writeArrayBoolean(boolean []a, int l) {
                return writeSubArrayBoolean(a, 0, l);
	}
        
        public static byte [] writeArrayChar(char []a, int l) {
                return writeSubArrayChar(a, 0, l);
	}
        
        public static byte [] writeArrayShort(short []a, int l) {
                return writeSubArrayShort(a, 0, l);
	}
        
        public static byte [] writeArrayInt(int []a, int l) {
                return writeSubArrayInt(a, 0, l);
	}
        
        public static byte [] writeArrayLong(long []a, int l) {
                return writeSubArrayLong(a, 0, l);
	}
        
        public static byte [] writeArrayFloat(float []a, int l) {
                return writeSubArrayFloat(a, 0, l);
	}
        
        public static byte [] writeArrayDouble(double []a, int l) {
                return writeSubArrayDouble(a, 0, l);
	}


        /* Primitive array to new byte array. */
        public static byte [] writeArrayBoolean(boolean []a) {
                return writeSubArrayBoolean(a, 0, a.length);
	}
        
        public static byte [] writeArrayChar(char []a) {
                return writeSubArrayChar(a, 0, a.length);
	}
        
        public static byte [] writeArrayShort(short []a) {
                return writeSubArrayShort(a, 0, a.length);
	}
        
        public static byte [] writeArrayInt(int []a) {
                return writeSubArrayInt(a, 0, a.length);
	}
        
        public static byte [] writeArrayLong(long []a) {
                return writeSubArrayLong(a, 0, a.length);
	}
        
        public static byte [] writeArrayFloat(float []a) {
                return writeSubArrayFloat(a, 0, a.length);
	}
        
        public static byte [] writeArrayDouble(double []a) {
                return writeSubArrayDouble(a, 0, a.length);
	}
        

        /* Primitive sub-array to new byte array with offset. */
        public static byte [] writeSubArrayBoolean(int ob, boolean []a, int oa, int l) {
                byte [] b = new byte[l+ob];
                writeSubArrayBoolean(a, oa, l, b, ob);
                return b;
	}
        
        public static byte [] writeSubArrayChar(int ob, char []a, int oa, int l) {
                byte [] b = new byte[l+ob];
                writeSubArrayChar(a, oa, l, b, ob);
                return b;
	}
        
        public static byte [] writeSubArrayShort(int ob, short []a, int oa, int l) {
                byte [] b = new byte[l+ob];
                writeSubArrayShort(a, oa, l, b, ob);
                return b;
	}
        
        public static byte [] writeSubArrayInt(int ob, int []a, int oa, int l) {
                byte [] b = new byte[l+ob];
                writeSubArrayInt(a, oa, l, b, ob);
                return b;
	}
        
        public static byte [] writeSubArrayLong(int ob, long []a, int oa, int l) {
                byte [] b = new byte[l+ob];
                writeSubArrayLong(a, oa, l, b, ob);
                return b;
	}
        
        public static byte [] writeSubArrayFloat(int ob, float []a, int oa, int l) {
                byte [] b = new byte[l+ob];
                writeSubArrayFloat(a, oa, l, b, ob);
                return b;
	}
        
        public static byte [] writeSubArrayDouble(int ob, double []a, int oa, int l) {
                byte [] b = new byte[l+ob];
                writeSubArrayDouble(a, oa, l, b, ob);
                return b;
	}        


        /* Primitive array prefix to new byte array with offset. */
        public static byte [] writeArrayBoolean(int ob, boolean []a, int l) {
                return writeSubArrayBoolean(ob, a, 0, l);
	}
        
        public static byte [] writeArrayChar(int ob, char []a, int l) {
                return writeSubArrayChar(ob, a, 0, l);
	}
        
        public static byte [] writeArrayShort(int ob, short []a, int l) {
                return writeSubArrayShort(ob, a, 0, l);
	}
        
        public static byte [] writeArrayInt(int ob, int []a, int l) {
                return writeSubArrayInt(ob, a, 0, l);
	}
        
        public static byte [] writeArrayLong(int ob, long []a, int l) {
                return writeSubArrayLong(ob, a, 0, l);
	}
        
        public static byte [] writeArrayFloat(int ob, float []a, int l) {
                return writeSubArrayFloat(ob, a, 0, l);
	}
        
        public static byte [] writeArrayDouble(int ob, double []a, int l) {
                return writeSubArrayDouble(ob, a, 0, l);
	}


        /* Primitive array to new byte array with offset. */
        public static byte [] writeArrayBoolean(int ob, boolean []a) {
                return writeSubArrayBoolean(ob, a, 0, a.length);
	}
        
        public static byte [] writeArrayChar(int ob, char []a) {
                return writeSubArrayChar(ob, a, 0, a.length);
	}
        
        public static byte [] writeArrayShort(int ob, short []a) {
                return writeSubArrayShort(ob, a, 0, a.length);
	}
        
        public static byte [] writeArrayInt(int ob, int []a) {
                return writeSubArrayInt(ob, a, 0, a.length);
	}
        
        public static byte [] writeArrayLong(int ob, long []a) {
                return writeSubArrayLong(ob, a, 0, a.length);
	}
        
        public static byte [] writeArrayFloat(int ob, float []a) {
                return writeSubArrayFloat(ob, a, 0, a.length);
	}
        
        public static byte [] writeArrayDouble(int ob, double []a) {
                return writeSubArrayDouble(ob, a, 0, a.length);
	}


        /* Primitive sub-array to new byte array with offset and custom length. */
        public static byte [] writeSubArrayBoolean(int ob, int lb, boolean []a, int oa, int l) {
                byte [] b = new byte[lb];
                writeSubArrayBoolean(a, oa, l, b, ob);
                return b;
	}
        
        public static byte [] writeSubArrayChar(int ob, int lb, char []a, int oa, int l) {
                byte [] b = new byte[lb];
                writeSubArrayChar(a, oa, l, b, ob);
                return b;
	}
        
        public static byte [] writeSubArrayShort(int ob, int lb, short []a, int oa, int l) {
                byte [] b = new byte[lb];
                writeSubArrayShort(a, oa, l, b, ob);
                return b;
	}
        
        public static byte [] writeSubArrayInt(int ob, int lb, int []a, int oa, int l) {
                byte [] b = new byte[lb];
                writeSubArrayInt(a, oa, l, b, ob);
                return b;
	}
        
        public static byte [] writeSubArrayLong(int ob, int lb, long []a, int oa, int l) {
                byte [] b = new byte[lb];
                writeSubArrayLong(a, oa, l, b, ob);
                return b;
	}
        
        public static byte [] writeSubArrayFloat(int ob, int lb, float []a, int oa, int l) {
                byte [] b = new byte[lb];
                writeSubArrayFloat(a, oa, l, b, ob);
                return b;
	}
        
        public static byte [] writeSubArrayDouble(int ob, int lb, double []a, int oa, int l) {
                byte [] b = new byte[lb];
                writeSubArrayDouble(a, oa, l, b, ob);
                return b;
	}        


        /* Primitive array prefix to new byte array with offset and custom length. */
        public static byte [] writeArrayBoolean(int ob, int lb, boolean []a, int l) {
                return writeSubArrayBoolean(ob, lb, a, 0, l);
	}
        
        public static byte [] writeArrayChar(int ob, int lb, char []a, int l) {
                return writeSubArrayChar(ob, lb, a, 0, l);
	}
        
        public static byte [] writeArrayShort(int ob, int lb, short []a, int l) {
                return writeSubArrayShort(ob, lb, a, 0, l);
	}
        
        public static byte [] writeArrayInt(int ob, int lb, int []a, int l) {
                return writeSubArrayInt(ob, lb, a, 0, l);
	}
        
        public static byte [] writeArrayLong(int ob, int lb, long []a, int l) {
                return writeSubArrayLong(ob, lb, a, 0, l);
	}
        
        public static byte [] writeArrayFloat(int ob, int lb, float []a, int l) {
                return writeSubArrayFloat(ob, lb, a, 0, l);
	}
        
        public static byte [] writeArrayDouble(int ob, int lb, double []a, int l) {
                return writeSubArrayDouble(ob, lb, a, 0, l);
	}


        /* Primitive array to new byte array with offset and custom length. */
        public static byte [] writeArrayBoolean(int ob, int lb, boolean []a) {
                return writeSubArrayBoolean(ob, lb, a, 0, a.length);
	}
        
        public static byte [] writeArrayChar(int ob, int lb, char []a) {
                return writeSubArrayChar(ob, lb, a, 0, a.length);
	}
        
        public static byte [] writeArrayShort(int ob, int lb, short []a) {
                return writeSubArrayShort(ob, lb, a, 0, a.length);
	}
        
        public static byte [] writeArrayInt(int ob, int lb, int []a) {
                return writeSubArrayInt(ob, lb, a, 0, a.length);
	}
        
        public static byte [] writeArrayLong(int ob, int lb, long []a) {
                return writeSubArrayLong(ob, lb, a, 0, a.length);
	}
        
        public static byte [] writeArrayFloat(int ob, int lb, float []a) {
                return writeSubArrayFloat(ob, lb, a, 0, a.length);
	}
        
        public static byte [] writeArrayDouble(int ob, int lb, double []a) {
                return writeSubArrayDouble(ob, lb, a, 0, a.length);
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
        public static void readSubArrayBoolean(byte[] b, int ob, boolean []a, int oa, int l) {
                int i = 0;
                while (i < l) {
                        a[oa+i] = readBoolean(b, ob+i);
                        i++;
                }
        }
        
        public static void readSubArrayChar(byte[] b, int ob, char []a, int oa, int l) {
                int i = 0;
                while (i < l) {
                        a[oa+i] = readChar(b, ob+2*i);
                        i++;
                }
        }
        
        public static void readSubArrayShort(byte[] b, int ob, short []a, int oa, int l) {
                int i = 0;
                while (i < l) {
                        a[oa+i] = readShort(b, ob+2*i);
                        i++;
                }
        }
        
        public static void readSubArrayInt(byte[] b, int ob, int []a, int oa, int l) {
                int i = 0;
                while (i < l) {
                        a[oa+i] = readInt(b, ob+4*i);
                        i++;
                }
        }
        
        public static void readSubArrayLong(byte[] b, int ob, long []a, int oa, int l) {
                int i = 0;
                while (i < l) {
                        a[oa+i] = readLong(b, ob+8*i);
                        i++;
                }
        }
        
        public static void readSubArrayFloat(byte[] b, int ob, float []a, int oa, int l) {
                int i = 0;
                while (i < l) {
                        a[oa+i] = readFloat(b, ob+4*i);
                        i++;
                }
        }
        
        public static void readSubArrayDouble(byte[] b, int ob, double []a, int oa, int l) {
                int i = 0;
                while (i < l) {
                        a[oa+i] = readDouble(b, ob+8*i);
                        i++;
                }
        }
        

        /* Byte array to primitive sub-array */
        public static void readSubArrayBoolean(byte[] b, boolean []a, int oa, int l) {
                readSubArrayBoolean(b, 0, a, oa, l);
        }

        public static void readSubArrayChar(byte[] b, char []a, int oa, int l) {
                readSubArrayChar(b, 0, a, oa, l);
        }

        public static void readSubArrayShort(byte[] b, short []a, int oa, int l) {
                readSubArrayShort(b, 0, a, oa, l);
        }

        public static void readSubArrayInt(byte[] b, int []a, int oa, int l) {
                readSubArrayInt(b, 0, a, oa, l);
        }

        public static void readSubArrayLong(byte[] b, long []a, int oa, int l) {
                readSubArrayLong(b, 0, a, oa, l);
        }

        public static void readSubArrayFloat(byte[] b, float []a, int oa, int l) {
                readSubArrayFloat(b, 0, a, oa, l);
        }

        public static void readSubArrayDouble(byte[] b, double []a, int oa, int l) {
                readSubArrayDouble(b, 0, a, oa, l);
        }


        /* Byte sub-array to primitive array prefix */
        public static void readArrayBoolean(byte[] b, int ob, boolean []a, int l) {
                readSubArrayBoolean(b, ob, a, 0, l);
        }
        
        public static void readArrayChar(byte[] b, int ob, char []a, int l) {
                readSubArrayChar(b, ob, a, 0, l);
        }
        
        public static void readArrayShort(byte[] b, int ob, short []a, int l) {
                readSubArrayShort(b, ob, a, 0, l);
        }
        
        public static void readArrayInt(byte[] b, int ob, int []a, int l) {
                readSubArrayInt(b, ob, a, 0, l);
        }
        
        public static void readArrayLong(byte[] b, int ob, long []a, int l) {
                readSubArrayLong(b, ob, a, 0, l);
        }
        
        public static void readArrayFloat(byte[] b, int ob, float []a, int l) {
                readSubArrayFloat(b, ob, a, 0, l);
        }
        
        public static void readArrayDouble(byte[] b, int ob, double []a, int l) {
                readSubArrayDouble(b, ob, a, 0, l);
        }
        

        /* Byte array to primitive array prefix */
        public static void readArrayBoolean(byte[] b, boolean []a, int l) {
                readArrayBoolean(b, 0, a, l);
        }

        public static void readArrayChar(byte[] b, char []a, int l) {
                readArrayChar(b, 0, a, l);
        }

        public static void readArrayShort(byte[] b, short []a, int l) {
                readArrayShort(b, 0, a, l);
        }

        public static void readArrayInt(byte[] b, int []a, int l) {
                readArrayInt(b, 0, a, l);
        }

        public static void readArrayLong(byte[] b, long []a, int l) {
                readArrayLong(b, 0, a, l);
        }

        public static void readArrayFloat(byte[] b, float []a, int l) {
                readArrayFloat(b, 0, a, l);
        }

        public static void readArrayDouble(byte[] b, double []a, int l) {
                readArrayDouble(b, 0, a, l);
        }


        /* Byte sub-array to primitive array. */
        public static void readArrayBoolean(byte[] b, int ob, boolean []a) {
                readArrayBoolean(b, ob, a, a.length);
        }
        
        public static void readArrayChar(byte[] b, int ob, char []a) {
                readArrayChar(b, ob, a, a.length);
        }
        
        public static void readArrayShort(byte[] b, int ob, short []a) {
                readArrayShort(b, ob, a, a.length);
        }
        
        public static void readArrayInt(byte[] b, int ob, int []a) {
                readArrayInt(b, ob, a, a.length);
        }
        
        public static void readArrayLong(byte[] b, int ob, long []a) {
                readArrayLong(b, ob, a, a.length);
        }
        
        public static void readArrayFloat(byte[] b, int ob, float []a) {
                readArrayFloat(b, ob, a, a.length);
        }
        
        public static void readArrayDouble(byte[] b, int ob, double []a) {
                readArrayDouble(b, ob, a, a.length);
        }
        

        /* Byte array to primitive array. */
        public static void readArrayBoolean(byte[] b, boolean []a) {
                readArrayBoolean(b, 0, a);
        }

        public static void readArrayChar(byte[] b, char []a) {
                readArrayChar(b, 0, a);
        }

        public static void readArrayShort(byte[] b, short []a) {
                readArrayShort(b, 0, a);
        }

        public static void readArrayInt(byte[] b, int []a) {
                readArrayInt(b, 0, a);
        }

        public static void readArrayLong(byte[] b, long []a) {
                readArrayLong(b, 0, a);
        }

        public static void readArrayFloat(byte[] b, float []a) {
                readArrayFloat(b, 0, a);
        }

        public static void readArrayDouble(byte[] b, double []a) {
                readArrayDouble(b, 0, a);
        }


        /* Byte sub-array to new primitive array with custom offset, length and copylength. */
        public static boolean[] readArrayBoolean(int oa, int la, byte[] b, int ob, int l) {
                boolean [] a = new boolean[la];
                readSubArrayBoolean(b, ob, a, oa, l);
                return a;
        }
        
        public static char[] readArrayChar(int oa, int la, byte[] b, int ob, int l) {
                char [] a = new char[la];
                readSubArrayChar(b, ob, a, oa, l);
                return a;
        }
        
        public static short[] readArrayShort(int oa, int la, byte[] b, int ob, int l) {
                short [] a = new short[la];
                readSubArrayShort(b, ob, a, oa, l);
                return a;
        }
        
        public static int[] readArrayInt(int oa, int la, byte[] b, int ob, int l) {
                int [] a = new int[la];
                readSubArrayInt(b, ob, a, oa, l);
                return a;
        }
        
        public static long[] readArrayLong(int oa, int la, byte[] b, int ob, int l) {
                long [] a = new long[la];
                readSubArrayLong(b, ob, a, oa, l);
                return a;
        }
        
        public static float[] readArrayFloat(int oa, int la, byte[] b, int ob, int l) {
                float [] a = new float[la];
                readSubArrayFloat(b, ob, a, oa, l);
                return a;
        }
        
        public static double[] readArrayDouble(int oa, int la, byte[] b, int ob, int l) {
                double [] a = new double[la];
                readSubArrayDouble(b, ob, a, oa, l);
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
                int la = (b.length-ob)/2;
                return readArrayChar(oa, la, b, ob, la);
        }
        
        public static short[] readArrayShort(int oa, byte[] b, int ob) {
                int la = (b.length-ob)/2;
                return readArrayShort(oa, la, b, ob, la);
        }
        
        public static int[] readArrayInt(int oa, byte[] b, int ob) {
                int la = (b.length-ob)/4;
                return readArrayInt(oa, la, b, ob, la);
        }
        
        public static long[] readArrayLong(int oa, byte[] b, int ob) {
                int la = (b.length-ob)/8;
                return readArrayLong(oa, la, b, ob, la);
        }
        
        public static float[] readArrayFloat(int oa, byte[] b, int ob) {
                int la = (b.length-ob)/4;
                return readArrayFloat(oa, la, b, ob, la);
        }
        
        public static double[] readArrayDouble(int oa, byte[] b, int ob) {
                int la = (b.length-ob)/8;
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
                readSubArrayBoolean(b, 0, a, oa, l);
                return a;
        }
        
        public static char[] readArrayChar(int oa, int la, byte[] b, int l) {
                char [] a = new char[la];
                readSubArrayChar(b, 0, a, oa, l);
                return a;
        }
        
        public static short[] readArrayShort(int oa, int la, byte[] b, int l) {
                short [] a = new short[la];
                readSubArrayShort(b, 0, a, oa, l);
                return a;
        }
        
        public static int[] readArrayInt(int oa, int la, byte[] b, int l) {
                int [] a = new int[la];
                readSubArrayInt(b, 0, a, oa, l);
                return a;
        }
        
        public static long[] readArrayLong(int oa, int la, byte[] b, int l) {
                long [] a = new long[la];
                readSubArrayLong(b, 0, a, oa, l);
                return a;
        }
        
        public static float[] readArrayFloat(int oa, int la, byte[] b, int l) {
                float [] a = new float[la];
                readSubArrayFloat(b, 0, a, oa, l);
                return a;
        }
        
        public static double[] readArrayDouble(int oa, int la, byte[] b, int l) {
                double [] a = new double[la];
                readSubArrayDouble(b, 0, a, oa, l);
                return a;
        }
        

        /* Byte array to new primitive array with custom offset. */
        public static boolean[] readArrayBoolean(int oa, byte[] b) {
                int la = b.length;
                return readArrayBoolean(oa, la, b, la);
        }
        
        public static char[] readArrayChar(int oa, byte[] b) {
                int la = b.length/2;
                return readArrayChar(oa, la, b, la);
        }
        
        public static short[] readArrayShort(int oa, byte[] b) {
                int la = b.length/2;
                return readArrayShort(oa, la, b, la);
        }
        
        public static int[] readArrayInt(int oa, byte[] b) {
                int la = b.length/4;
                return readArrayInt(oa, la, b, la);
        }
        
        public static long[] readArrayLong(int oa, byte[] b) {
                int la = b.length/8;
                return readArrayLong(oa, la, b, la);
        }
        
        public static float[] readArrayFloat(int oa, byte[] b) {
                int la = b.length/4;
                return readArrayFloat(oa, la, b, la);
        }
        
        public static double[] readArrayDouble(int oa, byte[] b) {
                int la = b.length/8;
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
        public static byte[] object2bytes(Object o) throws IbisIOException {
                byte [] b = null;
                
                try {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(bos);
                        oos.writeObject(o);
                        oos.close();
                        oos = null;
                        b = bos.toByteArray();
                } catch (Exception e) {
                        throw new IbisIOException(e.getMessage());
                }
                
                return b;
        }

        public static Object bytes2object(byte [] b) throws IbisIOException {
                Object o = null;
                
                try {
                        ByteArrayInputStream bis = new ByteArrayInputStream(b);
                        ObjectInputStream    ois = new ObjectInputStream(bis);
                        o   = ois.readObject();
                        ois.close();
                        ois = null;
                } catch (Exception e) {
                        throw new IbisIOException(e.getMessage());
                }
                
                return o;
        }
        
}
