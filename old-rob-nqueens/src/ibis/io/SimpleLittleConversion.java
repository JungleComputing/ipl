/* $Id$ */

package ibis.io;

public class SimpleLittleConversion extends Conversion {

    public final boolean bigEndian() {
        return false;
    }

    public final byte boolean2byte(boolean src) {
        return (src ? (byte) 1 : (byte) 0);
    }

    public final boolean byte2boolean(byte src) {
        return (src == (byte) 1);
    }

    public final void char2byte(char src, byte[] dst, int off) {
        dst[off + 0] = (byte) (src & 0xff);
        dst[off + 1] = (byte) ((src >>> 8) & 0xff);
    }

    public final char byte2char(byte[] src, int off) {
        int temp;

        temp = (src[off + 1] & 0xff);
        temp = temp << 8;
        temp |= (src[off] & 0xff);

        return (char) temp;
    }

    public final void short2byte(short src, byte[] dst, int off) {
        dst[off + 0] = (byte) (0xff & src);
        dst[off + 1] = (byte) (0xff & (src >> 8));
    }

    public final short byte2short(byte[] src, int off) {
        return (short) ((src[off + 1] << 8) | (src[off] & 0xff));

    }

    public final void int2byte(int src, byte[] dst, int off) {
        dst[off + 0] = (byte) (0xff & src);
        dst[off + 1] = (byte) (0xff & (src >> 8));
        dst[off + 2] = (byte) (0xff & (src >> 16));
        dst[off + 3] = (byte) (0xff & (src >> 24));
    }

    public final int byte2int(byte[] src, int off) {
        return (((src[off + 0] & 0xff) << 0) | ((src[off + 1] & 0xff) << 8)
                | ((src[off + 2] & 0xff) << 16)
                | ((src[off + 3] & 0xff) << 24));
    }

    public final void long2byte(long src, byte[] dst, int off) {
        int v1 = (int) (src >> 32);
        int v2 = (int) (src);

        int2byte(v1, dst, off + 4);
        int2byte(v2, dst, off);
    }

    public final long byte2long(byte[] src, int off) {
        int t1, t2;
        t1 = byte2int(src, off + 4);
        t2 = byte2int(src, off);

        return ((((long) t1) << 32) | (t2 & 0xffffffffL));
    }

    public final void float2byte(float src, byte[] dst, int off) {
        int2byte(Float.floatToIntBits(src), dst, off);
    }

    public final float byte2float(byte[] src, int off) {
        return Float.intBitsToFloat(byte2int(src, off));
    }

    public final void double2byte(double src, byte[] dst, int off) {
        long2byte(Double.doubleToLongBits(src), dst, off);
    }

    public final double byte2double(byte[] src, int off) {
        return Double.longBitsToDouble(byte2long(src, off));
    }

    public final void boolean2byte(boolean[] src, int off, int len, byte[] dst,
            int off2) {

        for (int i = 0; i < len; i++) {
            dst[off2 + i] = (src[off + i] ? (byte) 1 : (byte) 0);
        }
    }

    public final void byte2boolean(byte[] src, int index_src, boolean[] dst,
            int index_dst, int len) {

        for (int i = 0; i < len; i++) {
            dst[index_dst + i] = (src[index_src + i] == (byte) 1);
        }
    }

    // functions from here to EOF not final so Nio*Conversion can override
    // them

    public void char2byte(char[] src, int off, int len, byte[] dst, int off2) {
        int count = off2;
        int end = off + len;

        for (int i = off; i < end; i++) {
            char temp = src[i];
            dst[count + 0] = (byte) (temp & 0xff);
            dst[count + 1] = (byte) ((temp >>> 8) & 0xff);
            count += 2;
        }

    }

    public void byte2char(byte[] src, int index_src, char[] dst, int index_dst,
            int len) {
        int count = index_src;
        int end = index_dst + len;

        for (int i = index_dst; i < end; i++) {
            int temp = (src[count + 1] & 0xff);
            temp = temp << 8;
            temp |= (src[count] & 0xff);
            dst[i] = (char) temp;
            count += 2;
        }
    }

    public void short2byte(short[] src, int off, int len, byte[] dst, int off2) {
        int count = off2;
        int end = off + len;

        for (int i = off; i < end; i++) {
            short v = src[i];
            dst[count + 0] = (byte) (0xff & v);
            dst[count + 1] = (byte) (0xff & (v >> 8));
            count += 2;
        }
    }

    public void byte2short(byte[] src, int index_src, short[] dst,
            int index_dst, int len) {
        int count = index_src;
        int end = index_dst + len;

        for (int i = index_dst; i < end; i++) {
            dst[i] = (short) ((src[count + 1] << 8) | (src[count] & 0xff));
            count += 2;
        }
    }

    public void int2byte(int[] src, int off, int len, byte[] dst, int off2) {
        int count = off2;
        int end = off + len;

        for (int i = off; i < end; i++) {
            int v = src[i];

            dst[count + 0] = (byte) (0xff & v);
            dst[count + 1] = (byte) (0xff & (v >> 8));
            dst[count + 2] = (byte) (0xff & (v >> 16));
            dst[count + 3] = (byte) (0xff & (v >> 24));
            count += 4;
        }
    }

    public void byte2int(byte[] src, int index_src, int[] dst, int index_dst,
            int len) {
        int count = index_src;
        int end = index_dst + len;

        for (int i = index_dst; i < end; i++) {
            dst[i] = (((src[count + 0] & 0xff) << 0)
                    | ((src[count + 1] & 0xff) << 8)
                    | ((src[count + 2] & 0xff) << 16)
                    | ((src[count + 3] & 0xff) << 24));
            count += 4;
        }
    }

    public void long2byte(long[] src, int off, int len, byte[] dst, int off2) {
        int count = off2;
        int end = off + len;

        for (int i = off; i < end; i++) {
            long v = src[i];
            int v1 = (int) (v >> 32);
            int v2 = (int) (v);

            dst[count + 0] = (byte) (0xff & (v2 >> 0));
            dst[count + 1] = (byte) (0xff & (v2 >> 8));
            dst[count + 2] = (byte) (0xff & (v2 >> 16));
            dst[count + 3] = (byte) (0xff & (v2 >> 24));
            dst[count + 4] = (byte) (0xff & (v1 >> 0));
            dst[count + 5] = (byte) (0xff & (v1 >> 8));
            dst[count + 6] = (byte) (0xff & (v1 >> 16));
            dst[count + 7] = (byte) (0xff & (v1 >> 24));

            count += 8;
        }
    }

    public void byte2long(byte[] src, int index_src, long[] dst, int index_dst,
            int len) {
        int count = index_src;
        int end = index_dst + len;

        for (int i = index_dst; i < end; i++) {
            int t1 = (((src[count + 7] & 0xff) << 24)
                    | ((src[count + 6] & 0xff) << 16)
                    | ((src[count + 5] & 0xff) << 8)
                    | ((src[count + 4] & 0xff) << 0));
            int t2 = (((src[count + 3] & 0xff) << 24)
                    | ((src[count + 2] & 0xff) << 16)
                    | ((src[count + 1] & 0xff) << 8)
                    | ((src[count + 0] & 0xff) << 0));
            dst[i] = ((((long) t1) << 32) | (t2 & 0xffffffffL));
            count += 8;
        }
    }

    public void float2byte(float[] src, int off, int len, byte[] dst, int off2) {
        int count = off2;
        int end = off + len;

        for (int i = off; i < end; i++) {
            int v = Float.floatToIntBits(src[i]);
            dst[count + 3] = (byte) (0xff & (v >> 24));
            dst[count + 2] = (byte) (0xff & (v >> 16));
            dst[count + 1] = (byte) (0xff & (v >> 8));
            dst[count + 0] = (byte) (0xff & v);
            count += 4;
        }
    }

    public void byte2float(byte[] src, int index_src, float[] dst,
            int index_dst, int len) {
        int count = index_src;
        int end = index_dst + len;

        for (int i = index_dst; i < end; i++) {
            int temp = (((src[count + 3] & 0xff) << 24)
                    | ((src[count + 2] & 0xff) << 16)
                    | ((src[count + 1] & 0xff) << 8)
                    | ((src[count + 0] & 0xff) << 0));
            dst[i] = Float.intBitsToFloat(temp);
            count += 4;
        }
    }

    public void double2byte(double[] src, int off, int len, byte[] dst,
            int off2) {
        int count = off2;
        int end = off + len;

        for (int i = off; i < end; i++) {
            long v = Double.doubleToLongBits(src[i]);
            int v1 = (int) (v >> 32);
            int v2 = (int) (v);

            dst[count + 7] = (byte) (0xff & (v1 >> 24));
            dst[count + 6] = (byte) (0xff & (v1 >> 16));
            dst[count + 5] = (byte) (0xff & (v1 >> 8));
            dst[count + 4] = (byte) (0xff & (v1 >> 0));
            dst[count + 3] = (byte) (0xff & (v2 >> 24));
            dst[count + 2] = (byte) (0xff & (v2 >> 16));
            dst[count + 1] = (byte) (0xff & (v2 >> 8));
            dst[count + 0] = (byte) (0xff & (v2 >> 0));
            count += 8;
        }
    }

    public void byte2double(byte[] src, int index_src, double[] dst,
            int index_dst, int len) {
        int count = index_src;
        int end = index_dst + len;

        for (int i = index_dst; i < end; i++) {
            int t1 = (((src[count + 7] & 0xff) << 24)
                    | ((src[count + 6] & 0xff) << 16)
                    | ((src[count + 5] & 0xff) << 8)
                    | ((src[count + 4] & 0xff) << 0));
            int t2 = (((src[count + 3] & 0xff) << 24)
                    | ((src[count + 2] & 0xff) << 16)
                    | ((src[count + 1] & 0xff) << 8)
                    | ((src[count + 0] & 0xff) << 0));

            dst[i] = Double.longBitsToDouble((((long) t1) << 32)
                    | (t2 & 0xffffffffL));
            count += 8;
        }
    }
}
