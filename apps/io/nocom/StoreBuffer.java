/* $Id$ */

final class StoreBuffer {

    // This is the write part. It's slow, but we don't care
    // (only read part matters).
    int doubleLen = 0;

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

    public void writeArray(boolean[] a, int off, int len) {
        if (boolean_store == null) {
            boolean_store = new boolean[len];
            System.arraycopy(a, off, boolean_store, 0, len);
        } else {
            boolean[] temp = new boolean[boolean_store.length + len];
            System.arraycopy(boolean_store, 0, temp, 0, boolean_store.length);
            System.arraycopy(a, off, temp, boolean_store.length, len);
            boolean_store = temp;
        }
        count += len;
    }

    public void writeArray(byte[] a, int off, int len) {
        if (byte_store == null) {
            byte_store = new byte[len];
            System.arraycopy(a, off, byte_store, 0, len);
        } else {
            byte[] temp = new byte[byte_store.length + len];
            System.arraycopy(byte_store, 0, temp, 0, byte_store.length);
            System.arraycopy(a, off, temp, byte_store.length, len);
            byte_store = temp;
        }
        count += len;
    }

    public void writeArray(short[] a, int off, int len) {
        if (short_store == null) {
            short_store = new short[len];
            System.arraycopy(a, off, short_store, 0, len);
        } else {
            short[] temp = new short[short_store.length + len];
            System.arraycopy(short_store, 0, temp, 0, short_store.length);
            System.arraycopy(a, off, temp, short_store.length, len);
            short_store = temp;
        }
        count += len * 2;
    }

    public void writeArray(char[] a, int off, int len) {
        if (char_store == null) {
            char_store = new char[len];
            System.arraycopy(a, off, char_store, 0, len);
        } else {
            char[] temp = new char[char_store.length + len];
            System.arraycopy(char_store, 0, temp, 0, char_store.length);
            System.arraycopy(a, off, temp, char_store.length, len);
            char_store = temp;
        }
        count += len * 2;
    }

    public void writeArray(int[] a, int off, int len) {
        if (int_store == null) {
            int_store = new int[len];
            System.arraycopy(a, off, int_store, 0, len);
        } else {
            int[] temp = new int[int_store.length + len];
            System.arraycopy(int_store, 0, temp, 0, int_store.length);
            System.arraycopy(a, off, temp, int_store.length, len);
            int_store = temp;
        }
        count += len * 4;
    }

    public void writeArray(long[] a, int off, int len) {
        if (long_store == null) {
            long_store = new long[len];
            System.arraycopy(a, off, long_store, 0, len);
        } else {
            long[] temp = new long[long_store.length + len];
            System.arraycopy(long_store, 0, temp, 0, long_store.length);
            System.arraycopy(a, off, temp, long_store.length, len);
            long_store = temp;
        }
        count += len * 8;
    }

    public void writeArray(float[] a, int off, int len) {
        if (float_store == null) {
            float_store = new float[len];
            System.arraycopy(a, off, float_store, 0, len);
        } else {
            float[] temp = new float[float_store.length + len];
            System.arraycopy(float_store, 0, temp, 0, float_store.length);
            System.arraycopy(a, off, temp, float_store.length, len);
            float_store = temp;
        }
        count += len * 4;
    }

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
    }

    public long bytesWritten() {
        return count;
    }

    public void resetBytesWritten() {
        count = 0;
    }
}
