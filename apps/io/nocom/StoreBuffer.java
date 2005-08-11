
/* $Id$ */

final class StoreBuffer {
    /* We have to copy, serialization reuses the buffers */
    int booleanLen = 0;
    int byteLen = 0;
    int charLen = 0;
    int shortLen = 0;
    int doubleLen = 0;
    int intLen = 0;
    int floatLen = 0;
    int longLen = 0;
    
    boolean[] boolean_store = null;

    byte[] byte_store = null;

    short[] short_store = null;

    char[] char_store = null;

    int[] int_store = null;

    long[] long_store = null;

    float[] float_store = null;

    double[] double_store = null;

    public long count = 0;

    public void write(byte[] a, int off, int len) {
        writeArray(a, off, len);
    }

    public void write(byte[] a) {
        writeArray(a, 0, a.length);
    }

    public void write(int b) {
        byte[] temp = new byte[1];
        temp[0] = (byte) b;
        write(temp);
    }

    public void writeByte(byte b) {
        byte[] temp = new byte[1];
        temp[0] = b;
        writeArray(temp, 0, 1);
    }

    public void writeBoolean(boolean b) {
        boolean[] temp = new boolean[1];
        temp[0] = b;
        writeArray(temp, 0, 1);
    }

    public void writeChar(char b) {
        char[] temp = new char[1];
        temp[0] = b;
        writeArray(temp, 0, 1);
    }

    public void writeShort(short b) {
        short[] temp = new short[1];
        temp[0] = b;
        writeArray(temp, 0, 1);
    }

    public void writeInt(int b) {
        int[] temp = new int[1];
        temp[0] = b;
        writeArray(temp, 0, 1);
    }

    public void writeFloat(float b) {
        float[] temp = new float[1];
        temp[0] = b;
        writeArray(temp, 0, 1);
    }

    public void writeDouble(double b) {
        double[] temp = new double[1];
        temp[0] = b;
        writeArray(temp, 0, 1);
    }

    public void writeLong(long b) {
        long[] temp = new long[1];
        temp[0] = b;
        writeArray(temp, 0, 1);
    }

    /* this is the smarter copying verstion that doubles the destination buffer */
    public void writeArray(boolean[] a, int off, int len) {
        if (boolean_store == null) {
            boolean_store = new boolean[len];
            System.arraycopy(a, off, boolean_store, 0, len);
            booleanLen = len;
        } else {
            if (booleanLen + len < boolean_store.length) { // it fits
                System.arraycopy(a, off, boolean_store, booleanLen, len);
                booleanLen += len;
            } else { // it does not fit
                boolean[] temp = new boolean[booleanLen * 2];
                System.arraycopy(boolean_store, 0, temp, 0, booleanLen);
                System.arraycopy(a, off, temp, booleanLen, len);
                boolean_store = temp;
                booleanLen += len;
            }
        }
        count += len;
    }

    /* this is the smarter copying verstion that doubles the destination buffer */
    public void writeArray(byte[] a, int off, int len) {
        if (byte_store == null) {
            byte_store = new byte[len];
            System.arraycopy(a, off, byte_store, 0, len);
            byteLen = len;
        } else {
            if (byteLen + len < byte_store.length) { // it fits
                System.arraycopy(a, off, byte_store, byteLen, len);
                byteLen += len;
            } else { // it does not fit
                byte[] temp = new byte[byteLen * 2];
                System.arraycopy(byte_store, 0, temp, 0, byteLen);
                System.arraycopy(a, off, temp, byteLen, len);
                byte_store = temp;
                byteLen += len;
            }
        }
        count += len;
    }

    /* this is the smarter copying verstion that doubles the destination buffer */
    public void writeArray(short[] a, int off, int len) {
        if (short_store == null) {
            short_store = new short[len];
            System.arraycopy(a, off, short_store, 0, len);
            shortLen = len;
        } else {
            if (shortLen + len < short_store.length) { // it fits
                System.arraycopy(a, off, short_store, shortLen, len);
                shortLen += len;
            } else { // it does not fit
                short[] temp = new short[shortLen * 2];
                System.arraycopy(short_store, 0, temp, 0, shortLen);
                System.arraycopy(a, off, temp, shortLen, len);
                short_store = temp;
                shortLen += len;
            }
        }
        count += len * 2;
    }

    /* this is the smarter copying verstion that doubles the destination buffer */
    public void writeArray(char[] a, int off, int len) {
        if (char_store == null) {
            char_store = new char[len];
            System.arraycopy(a, off, char_store, 0, len);
            charLen = len;
        } else {
            if (charLen + len < char_store.length) { // it fits
                System.arraycopy(a, off, char_store, charLen, len);
                charLen += len;
            } else { // it does not fit
                char[] temp = new char[charLen * 2];
                System.arraycopy(char_store, 0, temp, 0, charLen);
                System.arraycopy(a, off, temp, charLen, len);
                char_store = temp;
                charLen += len;
            }
        }
        count += len * 2;
    }

    /* this is the smarter copying verstion that doubles the destination buffer */
    public void writeArray(int[] a, int off, int len) {
        if (int_store == null) {
            int_store = new int[len];
            System.arraycopy(a, off, int_store, 0, len);
            intLen = len;
        } else {
            if (intLen + len < int_store.length) { // it fits
                System.arraycopy(a, off, int_store, intLen, len);
                intLen += len;
            } else { // it does not fit
                int[] temp = new int[intLen * 2];
                System.arraycopy(int_store, 0, temp, 0, intLen);
                System.arraycopy(a, off, temp, intLen, len);
                int_store = temp;
                intLen += len;
            }
        }
        count += len * 4;
    }

    /* this is the smarter copying verstion that doubles the destination buffer */
    public void writeArray(long[] a, int off, int len) {
        if (long_store == null) {
            long_store = new long[len];
            System.arraycopy(a, off, long_store, 0, len);
            longLen = len;
        } else {
            if (longLen + len < long_store.length) { // it fits
                System.arraycopy(a, off, long_store, longLen, len);
                longLen += len;
            } else { // it does not fit
                long[] temp = new long[longLen * 2];
                System.arraycopy(long_store, 0, temp, 0, longLen);
                System.arraycopy(a, off, temp, longLen, len);
                long_store = temp;
                longLen += len;
            }
        }
        count += len * 8;
    }

    /* this is the smarter copying verstion that doubles the destination buffer */
    public void writeArray(float[] a, int off, int len) {
        if (float_store == null) {
            float_store = new float[len];
            System.arraycopy(a, off, float_store, 0, len);
            floatLen = len;
        } else {
            if (floatLen + len < float_store.length) { // it fits
                System.arraycopy(a, off, float_store, floatLen, len);
                floatLen += len;
            } else { // it does not fit
                float[] temp = new float[floatLen * 2];
                System.arraycopy(float_store, 0, temp, 0, floatLen);
                System.arraycopy(a, off, temp, floatLen, len);
                float_store = temp;
                floatLen += len;
            }
        }
        count += len * 4;
    }

/* this is the smarter copying verstion that doubles the destination buffer */
    public void writeArray(double[] a, int off, int len) {
        if (double_store == null) {
            double_store = new double[len];
            System.arraycopy(a, off, double_store, 0, len);
            doubleLen = len;
        } else {
            if (doubleLen + len < double_store.length) { // it fits
                System.arraycopy(a, off, double_store, doubleLen, len);
                doubleLen += len;
            } else { // it does not fit
                double[] temp = new double[doubleLen * 2];
                System.arraycopy(double_store, 0, temp, 0, doubleLen);
                System.arraycopy(a, off, temp, doubleLen, len);
                double_store = temp;
                doubleLen += len;
            }
        }
        count += len * 8;
    }

    public void clear() {
        boolean_store = null;
        byte_store = null;
        short_store = null;
        char_store = null;
        int_store = null;
        long_store = null;
        float_store = null;
        double_store = null;
        count = 0;

        booleanLen = 0;
        byteLen = 0;
        charLen = 0;
        shortLen = 0;
        doubleLen = 0;
        intLen = 0;
        floatLen = 0;
        longLen = 0;
    }

    public long bytesWritten() {
        return count;
    }

    public void resetBytesWritten() {
        count = 0;
    }
}
