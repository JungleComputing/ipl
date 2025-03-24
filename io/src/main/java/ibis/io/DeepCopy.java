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

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Hashtable;

/**
 * Exports a single static method that creates a deep copy of any serializable
 * object.
 */
public class DeepCopy {

    private static final String serialization = IOProperties.properties.getProperty(IOProperties.s_deepcopy_ser, "ibis");

    /** Prevent creation of a DeepCopy object. */
    private DeepCopy() {
        // Nothing. Not to be called.
    }

    /** Storage for the input and output streams. */
    static class Store {
        /** Size of buffers. */
        static final int BUFSIZ = 4096;

        private boolean closed = false;

        /** Byte storage. */
        private byte[] byte_store = new byte[BUFSIZ];

        /** Boolean storage. */
        private boolean[] boolean_store = new boolean[BUFSIZ];

        /** Char storage. */
        private char[] char_store = new char[BUFSIZ / 2];

        /** Short storage. */
        private short[] short_store = new short[BUFSIZ / 2];

        /** Int storage. */
        private int[] int_store = new int[BUFSIZ / 4];

        /** Float storage. */
        private float[] float_store = new float[BUFSIZ / 4];

        /** Long storage. */
        private long[] long_store = new long[BUFSIZ / 8];

        /** Double storage. */
        private double[] double_store = new double[BUFSIZ / 8];

        /** Current byte input index. */
        private int byte_in;

        /** Current boolean input index. */
        private int boolean_in;

        /** Current char input index. */
        private int char_in;

        /** Current short input index. */
        private int short_in;

        /** Current int input index. */
        private int int_in;

        /** Current float input index. */
        private int float_in;

        /** Current long input index. */
        private int long_in;

        /** Current double input index. */
        private int double_in;

        /** Current number of bytes in byte buffer. */
        private int byte_len;

        /** Current number of booleans in boolean buffer. */
        private int boolean_len;

        /** Current number of chars in char buffer. */
        private int char_len;

        /** Current number of shorts in short buffer. */
        private int short_len;

        /** Current number of ints in int buffer. */
        private int int_len;

        /** Current number of floats in float buffer. */
        private int float_len;

        /** Current number of longs in long buffer. */
        private int long_len;

        /** Current number of doubles in double buffer. */
        private int double_len;

        /** Number of waiters for input. */
        private int in_waiters;

        /** Number of waiters for output. */
        private int out_waiters;

        synchronized void reset() {
            byte_len = 0;
            boolean_len = 0;
            char_len = 0;
            short_len = 0;
            int_len = 0;
            float_len = 0;
            long_len = 0;
            double_len = 0;
            byte_in = 0;
            boolean_in = 0;
            char_in = 0;
            short_in = 0;
            int_in = 0;
            float_in = 0;
            long_in = 0;
            double_in = 0;
            closed = false;
            notifyAll();
        }

        synchronized void close() {
            closed = true;
            notifyAll();
        }

        synchronized int get() {
            while (!closed && byte_len == 0) {
                in_waiters++;
                try {
                    wait();
                } catch (Exception e) {
                    // nothing
                }
                in_waiters--;
            }

            if (byte_len == 0) {
                return -1;
            }

            int retval = byte_store[byte_in] & 255;
            byte_in++;
            byte_len--;
            if (byte_in == byte_store.length) {
                byte_in = 0;
            }
            if (out_waiters != 0) {
                notifyAll();
            }
            return retval;
        }

        synchronized byte getByte() {
            while (byte_len == 0) {
                in_waiters++;
                try {
                    wait();
                } catch (Exception e) {
                    // nothing
                }
                in_waiters--;
            }
            byte retval = byte_store[byte_in];
            byte_in++;
            byte_len--;
            if (byte_in == byte_store.length) {
                byte_in = 0;
            }
            if (out_waiters != 0) {
                notifyAll();
            }
            return retval;
        }

        synchronized boolean getBoolean() {
            while (boolean_len == 0) {
                in_waiters++;
                try {
                    wait();
                } catch (Exception e) {
                    // nothing
                }
                in_waiters--;
            }
            boolean retval = boolean_store[boolean_in];
            boolean_in++;
            boolean_len--;
            if (boolean_in == boolean_store.length) {
                boolean_in = 0;
            }
            if (out_waiters != 0) {
                notifyAll();
            }
            return retval;
        }

        synchronized char getChar() {
            while (char_len == 0) {
                in_waiters++;
                try {
                    wait();
                } catch (Exception e) {
                    // nothing
                }
                in_waiters--;
            }
            char retval = char_store[char_in];
            char_in++;
            char_len--;
            if (char_in == char_store.length) {
                char_in = 0;
            }
            if (out_waiters != 0) {
                notifyAll();
            }
            return retval;
        }

        synchronized short getShort() {
            while (short_len == 0) {
                in_waiters++;
                try {
                    wait();
                } catch (Exception e) {
                    // nothing
                }
                in_waiters--;
            }
            short retval = short_store[short_in];
            short_in++;
            short_len--;
            if (short_in == short_store.length) {
                short_in = 0;
            }
            if (out_waiters != 0) {
                notifyAll();
            }
            return retval;
        }

        synchronized int getInt() {
            while (int_len == 0) {
                in_waiters++;
                try {
                    wait();
                } catch (Exception e) {
                    // nothing
                }
                in_waiters--;
            }
            int retval = int_store[int_in];
            int_in++;
            int_len--;
            if (int_in == int_store.length) {
                int_in = 0;
            }
            if (out_waiters != 0) {
                notifyAll();
            }
            return retval;
        }

        synchronized float getFloat() {
            while (float_len == 0) {
                in_waiters++;
                try {
                    wait();
                } catch (Exception e) {
                    // nothing
                }
                in_waiters--;
            }
            float retval = float_store[float_in];
            float_in++;
            float_len--;
            if (float_in == float_store.length) {
                float_in = 0;
            }
            if (out_waiters != 0) {
                notifyAll();
            }
            return retval;
        }

        synchronized long getLong() {
            while (long_len == 0) {
                in_waiters++;
                try {
                    wait();
                } catch (Exception e) {
                    // nothing
                }
                in_waiters--;
            }
            long retval = long_store[long_in];
            long_in++;
            long_len--;
            if (long_in == long_store.length) {
                long_in = 0;
            }
            if (out_waiters != 0) {
                notifyAll();
            }
            return retval;
        }

        synchronized double getDouble() {
            while (double_len == 0) {
                in_waiters++;
                try {
                    wait();
                } catch (Exception e) {
                    // nothing
                }
                in_waiters--;
            }
            double retval = double_store[double_in];
            double_in++;
            double_len--;
            if (double_in == double_store.length) {
                double_in = 0;
            }
            if (out_waiters != 0) {
                notifyAll();
            }
            return retval;
        }

        synchronized void putByte(byte b) {
            while (byte_len == byte_store.length) {
                out_waiters++;
                try {
                    wait();
                } catch (Exception e) {
                    // nothing
                }
                out_waiters--;
            }
            int i = byte_in + byte_len;
            if (i >= byte_store.length) {
                i -= byte_store.length;
            }
            byte_store[i] = b;
            byte_len++;
            if (in_waiters > 0) {
                notifyAll();
            }
        }

        synchronized void put(int b) {
            while (byte_len == byte_store.length) {
                out_waiters++;
                try {
                    wait();
                } catch (Exception e) {
                    // nothing
                }
                out_waiters--;
            }
            int i = byte_in + byte_len;
            if (i >= byte_store.length) {
                i -= byte_store.length;
            }
            byte_store[i] = (byte) b;
            byte_len++;
            if (in_waiters > 0) {
                notifyAll();
            }
        }

        synchronized void putBoolean(boolean b) {
            while (boolean_len == boolean_store.length) {
                out_waiters++;
                try {
                    wait();
                } catch (Exception e) {
                    // nothing
                }
                out_waiters--;
            }
            int i = boolean_in + boolean_len;
            if (i >= boolean_store.length) {
                i -= boolean_store.length;
            }
            boolean_store[i] = b;
            boolean_len++;
            if (in_waiters > 0) {
                notifyAll();
            }
        }

        synchronized void putChar(char b) {
            while (char_len == char_store.length) {
                out_waiters++;
                try {
                    wait();
                } catch (Exception e) {
                    // nothing
                }
                out_waiters--;
            }
            int i = char_in + char_len;
            if (i >= char_store.length) {
                i -= char_store.length;
            }
            char_store[i] = b;
            char_len++;
            if (in_waiters > 0) {
                notifyAll();
            }
        }

        synchronized void putShort(short b) {
            while (short_len == short_store.length) {
                out_waiters++;
                try {
                    wait();
                } catch (Exception e) {
                    // nothing
                }
                out_waiters--;
            }
            int i = short_in + short_len;
            if (i >= short_store.length) {
                i -= short_store.length;
            }
            short_store[i] = b;
            short_len++;
            if (in_waiters > 0) {
                notifyAll();
            }
        }

        synchronized void putInt(int b) {
            while (int_len == int_store.length) {
                out_waiters++;
                try {
                    wait();
                } catch (Exception e) {
                    // nothing
                }
                out_waiters--;
            }
            int i = int_in + int_len;
            if (i >= int_store.length) {
                i -= int_store.length;
            }
            int_store[i] = b;
            int_len++;
            if (in_waiters > 0) {
                notifyAll();
            }
        }

        synchronized void putFloat(float b) {
            while (float_len == float_store.length) {
                out_waiters++;
                try {
                    wait();
                } catch (Exception e) {
                    // nothing
                }
                out_waiters--;
            }
            int i = float_in + float_len;
            if (i >= float_store.length) {
                i -= float_store.length;
            }
            float_store[i] = b;
            float_len++;
            if (in_waiters > 0) {
                notifyAll();
            }
        }

        synchronized void putLong(long b) {
            while (long_len == long_store.length) {
                out_waiters++;
                try {
                    wait();
                } catch (Exception e) {
                    // nothing
                }
                out_waiters--;
            }
            int i = long_in + long_len;
            if (i >= long_store.length) {
                i -= long_store.length;
            }
            long_store[i] = b;
            long_len++;
            if (in_waiters > 0) {
                notifyAll();
            }
        }

        synchronized void putDouble(double b) {
            while (double_len == double_store.length) {
                out_waiters++;
                try {
                    wait();
                } catch (Exception e) {
                    // nothing
                }
                out_waiters--;
            }
            int i = double_in + double_len;
            if (i >= double_store.length) {
                i -= double_store.length;
            }
            double_store[i] = b;
            double_len++;
            if (in_waiters > 0) {
                notifyAll();
            }
        }

        synchronized int get(byte[] a, int off, int len) {
            int cnt = 0;
            while (len != 0 && !closed) {
                while (byte_len == 0) {
                    in_waiters++;
                    try {
                        wait();
                    } catch (Exception e) {
                        // nothing
                    }
                    in_waiters--;
                }
                int available = byte_len;
                int sz = len;
                if (sz > available) {
                    sz = available;
                }
                if (byte_in + sz <= byte_store.length) {
                    System.arraycopy(byte_store, byte_in, a, off, sz);
                } else {
                    int sz1 = byte_store.length - byte_in;
                    System.arraycopy(byte_store, byte_in, a, off, sz1);
                    System.arraycopy(byte_store, 0, a, off + sz1, sz - sz1);
                }
                len -= sz;
                off += sz;
                byte_in += sz;
                cnt += sz;
                if (byte_in >= byte_store.length) {
                    byte_in -= byte_store.length;
                }
                byte_len -= sz;
                if (sz != 0 && out_waiters != 0) {
                    notifyAll();
                }
            }

            if (closed && cnt == 0) {
                return -1;
            }

            return cnt;
        }

        synchronized void getSlice(byte[] a, int off, int len) {
            // System.out.println("getSlice-byte " + len);
            while (len != 0) {
                while (byte_len == 0) {
                    in_waiters++;
                    try {
                        wait();
                    } catch (Exception e) {
                        // nothing
                    }
                    in_waiters--;
                }
                int available = byte_len;
                int sz = len;
                if (sz > available) {
                    sz = available;
                }
                if (byte_in + sz <= byte_store.length) {
                    System.arraycopy(byte_store, byte_in, a, off, sz);
                } else {
                    int sz1 = byte_store.length - byte_in;
                    System.arraycopy(byte_store, byte_in, a, off, sz1);
                    System.arraycopy(byte_store, 0, a, off + sz1, sz - sz1);
                }
                len -= sz;
                off += sz;
                byte_in += sz;
                if (byte_in >= byte_store.length) {
                    byte_in -= byte_store.length;
                }
                byte_len -= sz;
                if (sz != 0 && out_waiters != 0) {
                    notifyAll();
                }
            }
        }

        synchronized void getSlice(boolean[] a, int off, int len) {
            // System.out.println("getSlice-boolean " + len);
            while (len != 0) {
                while (boolean_len == 0) {
                    in_waiters++;
                    try {
                        wait();
                    } catch (Exception e) {
                        // nothing
                    }
                    in_waiters--;
                }
                int available = boolean_len;
                int sz = len;
                if (sz > available) {
                    sz = available;
                }
                if (boolean_in + sz <= boolean_store.length) {
                    System.arraycopy(boolean_store, boolean_in, a, off, sz);
                } else {
                    int sz1 = boolean_store.length - boolean_in;
                    System.arraycopy(boolean_store, boolean_in, a, off, sz1);
                    System.arraycopy(boolean_store, 0, a, off + sz1, sz - sz1);
                }
                len -= sz;
                off += sz;
                boolean_in += sz;
                if (boolean_in >= boolean_store.length) {
                    boolean_in -= boolean_store.length;
                }
                boolean_len -= sz;
                if (sz != 0 && out_waiters != 0) {
                    notifyAll();
                }
            }
        }

        synchronized void getSlice(char[] a, int off, int len) {
            // System.out.println("getSlice-char " + len);
            while (len != 0) {
                while (char_len == 0) {
                    in_waiters++;
                    try {
                        wait();
                    } catch (Exception e) {
                        // nothing
                    }
                    in_waiters--;
                }
                int available = char_len;
                int sz = len;
                if (sz > available) {
                    sz = available;
                }
                if (char_in + sz <= char_store.length) {
                    System.arraycopy(char_store, char_in, a, off, sz);
                } else {
                    int sz1 = char_store.length - char_in;
                    System.arraycopy(char_store, char_in, a, off, sz1);
                    System.arraycopy(char_store, 0, a, off + sz1, sz - sz1);
                }
                len -= sz;
                off += sz;
                char_in += sz;
                if (char_in >= char_store.length) {
                    char_in -= char_store.length;
                }
                char_len -= sz;
                if (sz != 0 && out_waiters != 0) {
                    notifyAll();
                }
            }
        }

        synchronized void getSlice(short[] a, int off, int len) {
            // System.out.println("getSlice-short " + len);
            while (len != 0) {
                while (short_len == 0) {
                    in_waiters++;
                    try {
                        wait();
                    } catch (Exception e) {
                        // nothing
                    }
                    in_waiters--;
                }
                int available = short_len;
                int sz = len;
                if (sz > available) {
                    sz = available;
                }
                if (short_in + sz <= short_store.length) {
                    System.arraycopy(short_store, short_in, a, off, sz);
                } else {
                    int sz1 = short_store.length - short_in;
                    System.arraycopy(short_store, short_in, a, off, sz1);
                    System.arraycopy(short_store, 0, a, off + sz1, sz - sz1);
                }
                len -= sz;
                off += sz;
                short_in += sz;
                if (short_in >= short_store.length) {
                    short_in -= short_store.length;
                }
                short_len -= sz;
                if (sz != 0 && out_waiters != 0) {
                    notifyAll();
                }
            }
        }

        synchronized void getSlice(int[] a, int off, int len) {
            // System.out.println("getSlice-int " + len);
            while (len != 0) {
                while (int_len == 0) {
                    in_waiters++;
                    try {
                        wait();
                    } catch (Exception e) {
                        // nothing
                    }
                    in_waiters--;
                }
                int available = int_len;
                int sz = len;
                if (sz > available) {
                    sz = available;
                }
                if (int_in + sz <= int_store.length) {
                    System.arraycopy(int_store, int_in, a, off, sz);
                } else {
                    int sz1 = int_store.length - int_in;
                    System.arraycopy(int_store, int_in, a, off, sz1);
                    System.arraycopy(int_store, 0, a, off + sz1, sz - sz1);
                }
                len -= sz;
                off += sz;
                int_in += sz;
                if (int_in >= int_store.length) {
                    int_in -= int_store.length;
                }
                int_len -= sz;
                if (sz != 0 && out_waiters != 0) {
                    notifyAll();
                }
            }
        }

        synchronized void getSlice(float[] a, int off, int len) {
            // System.out.println("getSlice-float " + len);
            while (len != 0) {
                while (float_len == 0) {
                    in_waiters++;
                    try {
                        wait();
                    } catch (Exception e) {
                        // nothing
                    }
                    in_waiters--;
                }
                int available = float_len;
                int sz = len;
                if (sz > available) {
                    sz = available;
                }
                if (float_in + sz <= float_store.length) {
                    System.arraycopy(float_store, float_in, a, off, sz);
                } else {
                    int sz1 = float_store.length - float_in;
                    System.arraycopy(float_store, float_in, a, off, sz1);
                    System.arraycopy(float_store, 0, a, off + sz1, sz - sz1);
                }
                len -= sz;
                off += sz;
                float_in += sz;
                if (float_in >= float_store.length) {
                    float_in -= float_store.length;
                }
                float_len -= sz;
                if (sz != 0 && out_waiters != 0) {
                    notifyAll();
                }
            }
        }

        synchronized void getSlice(long[] a, int off, int len) {
            // System.out.println("getSlice-long " + len);
            while (len != 0) {
                while (long_len == 0) {
                    in_waiters++;
                    try {
                        wait();
                    } catch (Exception e) {
                        // nothing
                    }
                    in_waiters--;
                }
                int available = long_len;
                int sz = len;
                if (sz > available) {
                    sz = available;
                }
                if (long_in + sz <= long_store.length) {
                    System.arraycopy(long_store, long_in, a, off, sz);
                } else {
                    int sz1 = long_store.length - long_in;
                    System.arraycopy(long_store, long_in, a, off, sz1);
                    System.arraycopy(long_store, 0, a, off + sz1, sz - sz1);
                }
                len -= sz;
                off += sz;
                long_in += sz;
                if (long_in >= long_store.length) {
                    long_in -= long_store.length;
                }
                long_len -= sz;
                if (sz != 0 && out_waiters != 0) {
                    notifyAll();
                }
            }
        }

        synchronized void getSlice(double[] a, int off, int len) {
            // System.out.println("getSlice-double " + len);
            while (len != 0) {
                while (double_len == 0) {
                    in_waiters++;
                    try {
                        wait();
                    } catch (Exception e) {
                        // nothing
                    }
                    in_waiters--;
                }
                int available = double_len;
                int sz = len;
                if (sz > available) {
                    sz = available;
                }
                if (double_in + sz <= double_store.length) {
                    System.arraycopy(double_store, double_in, a, off, sz);
                } else {
                    int sz1 = double_store.length - double_in;
                    System.arraycopy(double_store, double_in, a, off, sz1);
                    System.arraycopy(double_store, 0, a, off + sz1, sz - sz1);
                }
                len -= sz;
                off += sz;
                double_in += sz;
                if (double_in >= double_store.length) {
                    double_in -= double_store.length;
                }
                double_len -= sz;
                if (sz != 0 && out_waiters != 0) {
                    notifyAll();
                }
            }
        }

        synchronized void put(byte[] a, int off, int len) {
            while (len != 0) {
                while (byte_len == byte_store.length) {
                    out_waiters++;
                    try {
                        wait();
                    } catch (Exception e) {
                        // nothing
                    }
                    out_waiters--;
                }
                int available = byte_store.length - byte_len;
                int sz = len;
                if (sz > available) {
                    sz = available;
                }
                int ob = byte_in + byte_len;
                if (ob >= byte_store.length) {
                    ob -= byte_store.length;
                }
                if (ob + sz <= byte_store.length) {
                    System.arraycopy(a, off, byte_store, ob, sz);
                } else {
                    int sz1 = byte_store.length - ob;
                    System.arraycopy(a, off, byte_store, ob, sz1);
                    System.arraycopy(a, off + sz1, byte_store, 0, sz - sz1);
                }
                len -= sz;
                off += sz;
                byte_len += sz;
                if (sz != 0 && in_waiters != 0) {
                    notifyAll();
                }
            }
        }

        synchronized void putSlice(byte[] a, int off, int len) {
            // System.out.println("putSlice-byte " + len);
            while (len != 0) {
                while (byte_len == byte_store.length) {
                    out_waiters++;
                    try {
                        wait();
                    } catch (Exception e) {
                        // nothing
                    }
                    out_waiters--;
                }
                int available = byte_store.length - byte_len;
                int sz = len;
                if (sz > available) {
                    sz = available;
                }
                int ob = byte_in + byte_len;
                if (ob >= byte_store.length) {
                    ob -= byte_store.length;
                }
                if (ob + sz <= byte_store.length) {
                    System.arraycopy(a, off, byte_store, ob, sz);
                } else {
                    int sz1 = byte_store.length - ob;
                    System.arraycopy(a, off, byte_store, ob, sz1);
                    System.arraycopy(a, off + sz1, byte_store, 0, sz - sz1);
                }
                len -= sz;
                off += sz;
                byte_len += sz;
                if (sz != 0 && in_waiters != 0) {
                    notifyAll();
                }
            }
        }

        synchronized void putSlice(boolean[] a, int off, int len) {
            // System.out.println("putSlice-boolean " + len);
            while (len != 0) {
                while (boolean_len == boolean_store.length) {
                    out_waiters++;
                    try {
                        wait();
                    } catch (Exception e) {
                        // nothing
                    }
                    out_waiters--;
                }
                int available = boolean_store.length - boolean_len;
                int sz = len;
                if (sz > available) {
                    sz = available;
                }
                int ob = boolean_in + boolean_len;
                if (ob >= boolean_store.length) {
                    ob -= boolean_store.length;
                }
                if (ob + sz <= boolean_store.length) {
                    System.arraycopy(a, off, boolean_store, ob, sz);
                } else {
                    int sz1 = boolean_store.length - ob;
                    System.arraycopy(a, off, boolean_store, ob, sz1);
                    System.arraycopy(a, off + sz1, boolean_store, 0, sz - sz1);
                }
                len -= sz;
                off += sz;
                boolean_len += sz;
                if (sz != 0 && in_waiters != 0) {
                    notifyAll();
                }
            }
        }

        synchronized void putSlice(char[] a, int off, int len) {
            // System.out.println("putSlice-char " + len);
            while (len != 0) {
                while (char_len == char_store.length) {
                    out_waiters++;
                    try {
                        wait();
                    } catch (Exception e) {
                        // nothing
                    }
                    out_waiters--;
                }
                int available = char_store.length - char_len;
                int sz = len;
                if (sz > available) {
                    sz = available;
                }
                int ob = char_in + char_len;
                if (ob >= char_store.length) {
                    ob -= char_store.length;
                }
                if (ob + sz <= char_store.length) {
                    System.arraycopy(a, off, char_store, ob, sz);
                } else {
                    int sz1 = char_store.length - ob;
                    System.arraycopy(a, off, char_store, ob, sz1);
                    System.arraycopy(a, off + sz1, char_store, 0, sz - sz1);
                }
                len -= sz;
                off += sz;
                char_len += sz;
                if (sz != 0 && in_waiters != 0) {
                    notifyAll();
                }
            }
        }

        synchronized void putSlice(short[] a, int off, int len) {
            // System.out.println("putSlice-short " + len);
            while (len != 0) {
                while (short_len == short_store.length) {
                    out_waiters++;
                    try {
                        wait();
                    } catch (Exception e) {
                        // nothing
                    }
                    out_waiters--;
                }
                int available = short_store.length - short_len;
                int sz = len;
                if (sz > available) {
                    sz = available;
                }
                int ob = short_in + short_len;
                if (ob >= short_store.length) {
                    ob -= short_store.length;
                }
                if (ob + sz <= short_store.length) {
                    System.arraycopy(a, off, short_store, ob, sz);
                } else {
                    int sz1 = short_store.length - ob;
                    System.arraycopy(a, off, short_store, ob, sz1);
                    System.arraycopy(a, off + sz1, short_store, 0, sz - sz1);
                }
                len -= sz;
                off += sz;
                short_len += sz;
                if (sz != 0 && in_waiters != 0) {
                    notifyAll();
                }
            }
        }

        synchronized void putSlice(int[] a, int off, int len) {
            // System.out.println("putSlice-int " + len);
            while (len != 0) {
                while (int_len == int_store.length) {
                    out_waiters++;
                    try {
                        wait();
                    } catch (Exception e) {
                        // nothing
                    }
                    out_waiters--;
                }
                int available = int_store.length - int_len;
                int sz = len;
                if (sz > available) {
                    sz = available;
                }
                int ob = int_in + int_len;
                if (ob >= int_store.length) {
                    ob -= int_store.length;
                }
                if (ob + sz <= int_store.length) {
                    System.arraycopy(a, off, int_store, ob, sz);
                } else {
                    int sz1 = int_store.length - ob;
                    System.arraycopy(a, off, int_store, ob, sz1);
                    System.arraycopy(a, off + sz1, int_store, 0, sz - sz1);
                }
                len -= sz;
                off += sz;
                int_len += sz;
                if (sz != 0 && in_waiters != 0) {
                    notifyAll();
                }
            }
        }

        synchronized void putSlice(float[] a, int off, int len) {
            // System.out.println("putSlice-float " + len);
            while (len != 0) {
                while (float_len == float_store.length) {
                    out_waiters++;
                    try {
                        wait();
                    } catch (Exception e) {
                        // nothing
                    }
                    out_waiters--;
                }
                int available = float_store.length - float_len;
                int sz = len;
                if (sz > available) {
                    sz = available;
                }
                int ob = float_in + float_len;
                if (ob >= float_store.length) {
                    ob -= float_store.length;
                }
                if (ob + sz <= float_store.length) {
                    System.arraycopy(a, off, float_store, ob, sz);
                } else {
                    int sz1 = float_store.length - ob;
                    System.arraycopy(a, off, float_store, ob, sz1);
                    System.arraycopy(a, off + sz1, float_store, 0, sz - sz1);
                }
                len -= sz;
                off += sz;
                float_len += sz;
                if (sz != 0 && in_waiters != 0) {
                    notifyAll();
                }
            }
        }

        synchronized void putSlice(long[] a, int off, int len) {
            // System.out.println("putSlice-long " + len);
            while (len != 0) {
                while (long_len == long_store.length) {
                    out_waiters++;
                    try {
                        wait();
                    } catch (Exception e) {
                        // nothing
                    }
                    out_waiters--;
                }
                int available = long_store.length - long_len;
                int sz = len;
                if (sz > available) {
                    sz = available;
                }
                int ob = long_in + long_len;
                if (ob >= long_store.length) {
                    ob -= long_store.length;
                }
                if (ob + sz <= long_store.length) {
                    System.arraycopy(a, off, long_store, ob, sz);
                } else {
                    int sz1 = long_store.length - ob;
                    System.arraycopy(a, off, long_store, ob, sz1);
                    System.arraycopy(a, off + sz1, long_store, 0, sz - sz1);
                }
                len -= sz;
                off += sz;
                long_len += sz;
                if (sz != 0 && in_waiters != 0) {
                    notifyAll();
                }
            }
        }

        synchronized void putSlice(double[] a, int off, int len) {
            // System.out.println("putSlice-double " + len);
            while (len != 0) {
                while (double_len == double_store.length) {
                    out_waiters++;
                    try {
                        wait();
                    } catch (Exception e) {
                        // nothing
                    }
                    out_waiters--;
                }
                int available = double_store.length - double_len;
                int sz = len;
                if (sz > available) {
                    sz = available;
                }
                int ob = double_in + double_len;
                if (ob >= double_store.length) {
                    ob -= double_store.length;
                }
                if (ob + sz <= double_store.length) {
                    System.arraycopy(a, off, double_store, ob, sz);
                } else {
                    int sz1 = double_store.length - ob;
                    System.arraycopy(a, off, double_store, ob, sz1);
                    System.arraycopy(a, off + sz1, double_store, 0, sz - sz1);
                }
                len -= sz;
                off += sz;
                double_len += sz;
                if (sz != 0 && in_waiters != 0) {
                    notifyAll();
                }
            }
        }

    }

    static class StoreArrayInputStream extends DataInputStream {
        Store buf;

        public StoreArrayInputStream(Store buf) {
            this.buf = buf;
        }

        @Override
        public int bufferSize() {
            return Store.BUFSIZ;
        }

        @Override
        public void reset() {
            buf.reset();
        }

        @Override
        public byte readByte() {
            return buf.getByte();
        }

        @Override
        public boolean readBoolean() {
            return buf.getBoolean();
        }

        @Override
        public char readChar() {
            return buf.getChar();
        }

        @Override
        public short readShort() {
            return buf.getShort();
        }

        @Override
        public int readInt() {
            return buf.getInt();
        }

        @Override
        public long readLong() {
            return buf.getLong();
        }

        @Override
        public float readFloat() {
            return buf.getFloat();
        }

        @Override
        public double readDouble() {
            return buf.getDouble();
        }

        @Override
        public void readArray(boolean[] a, int off, int len) {
            buf.getSlice(a, off, len);
        }

        @Override
        public void readArray(byte[] a, int off, int len) {
            buf.getSlice(a, off, len);
        }

        @Override
        public void readArray(short[] a, int off, int len) {
            buf.getSlice(a, off, len);
        }

        @Override
        public void readArray(char[] a, int off, int len) {
            buf.getSlice(a, off, len);
        }

        @Override
        public void readArray(int[] a, int off, int len) {
            buf.getSlice(a, off, len);
        }

        @Override
        public void readArray(long[] a, int off, int len) {
            buf.getSlice(a, off, len);
        }

        @Override
        public void readArray(float[] a, int off, int len) {
            buf.getSlice(a, off, len);
        }

        @Override
        public void readArray(double[] a, int off, int len) {
            buf.getSlice(a, off, len);
        }

        @Override
        public int read() {
            return buf.get();
        }

        @Override
        public int read(byte[] b) {
            return buf.get(b, 0, b.length);
        }

        @Override
        public int read(byte[] b, int off, int len) {
            return buf.get(b, off, len);
        }

        @Override
        public void readByteBuffer(ByteBuffer b) {
            byte[] f = new byte[b.limit() - b.position()];
            readArray(f, 0, f.length);
            b.put(f);
        }

        @Override
        public long bytesRead() {
            return 0L;
        }

        @Override
        public void resetBytesRead() {
            // not implemented
        }

        @Override
        public int available() {
            return 0;
        }

        @Override
        public void close() {
            // nothing here
        }
    }

    static class StoreArrayOutputStream extends DataOutputStream {

        Store buf;

        public StoreArrayOutputStream(Store buf) {
            this.buf = buf;
        }

        @Override
        public int bufferSize() {
            return Store.BUFSIZ;
        }

        @Override
        public void writeByte(byte b) {
            buf.putByte(b);
        }

        @Override
        public void writeBoolean(boolean b) {
            buf.putBoolean(b);
        }

        @Override
        public void writeChar(char b) {
            buf.putChar(b);
        }

        @Override
        public void writeShort(short b) {
            buf.putShort(b);
        }

        @Override
        public void writeInt(int b) {
            buf.putInt(b);
        }

        @Override
        public void writeFloat(float b) {
            buf.putFloat(b);
        }

        @Override
        public void writeLong(long b) {
            buf.putLong(b);
        }

        @Override
        public void writeDouble(double b) {
            buf.putDouble(b);
        }

        @Override
        public void write(int b) {
            buf.put(b);
        }

        @Override
        public void write(byte[] b) {
            buf.put(b, 0, b.length);
        }

        @Override
        public void write(byte[] b, int off, int len) {
            buf.put(b, off, len);
        }

        @Override
        public void writeArray(boolean[] a, int off, int len) {
            buf.putSlice(a, off, len);
        }

        @Override
        public void writeArray(byte[] a, int off, int len) {
            buf.putSlice(a, off, len);
        }

        @Override
        public void writeArray(short[] a, int off, int len) {
            buf.putSlice(a, off, len);
        }

        @Override
        public void writeArray(char[] a, int off, int len) {
            buf.putSlice(a, off, len);
        }

        @Override
        public void writeArray(int[] a, int off, int len) {
            buf.putSlice(a, off, len);
        }

        @Override
        public void writeArray(long[] a, int off, int len) {
            buf.putSlice(a, off, len);
        }

        @Override
        public void writeArray(float[] a, int off, int len) {
            buf.putSlice(a, off, len);
        }

        @Override
        public void writeArray(double[] a, int off, int len) {
            buf.putSlice(a, off, len);
        }

        @Override
        public void writeByteBuffer(ByteBuffer b) {
            byte[] f = new byte[b.limit() - b.position()];
            b.get(f);
            writeArray(f, 0, f.length);
        }

        @Override
        public void flush() {
            // nothing to flush
        }

        @Override
        public boolean finished() {
            return true;
        }

        @Override
        public void finish() {
            // nothing to finish
        }

        @Override
        public void close() {
            buf.close();
        }

        @Override
        public long bytesWritten() {
            return 0;
        }

        @Override
        public void resetBytesWritten() {
            // not implemented
        }
    }

    /**
     * Creates and returns a deep copy of the specified object.
     * 
     * @param o the object to be copied
     * @return the copy.
     */
    public static Serializable deepCopy(Serializable o) {
        Store store = new Store();
        StoreArrayInputStream input = new StoreArrayInputStream(store);
        StoreArrayOutputStream output = new StoreArrayOutputStream(store);

        final Object oc = o;

        try {

            final SerializationInput ser_input = SerializationFactory.createSerializationInput(serialization, input, null);
            final SerializationOutput ser_output = SerializationFactory.createSerializationOutput(serialization, output, null);

            Thread writer = new Thread("DeepCopy writer") {
                @Override
                public void run() {
                    // System.out.println("Writer started ...");
                    try {
                        ser_output.writeObject(oc);
                        ser_output.close();
                    } catch (Exception e) {
                        // Should not happen
                        // System.out.println("Got exception: " + e);
                        // e.printStackTrace();
                        throw new Error("Got exception: ", e);
                    }
                }
            };
            writer.start();

            return (Serializable) ser_input.readObject();
        } catch (Exception e) {
            // Should not happen
            throw new Error("Got exception: ", e);
        }
    }

    /**
     * A little testing ...
     * 
     * @param args ignored.
     */
    public static void main(String[] args) {
        Hashtable<String, Hashtable<?, ?>> h = new Hashtable<>();
        String[] strings = new String[10];

        for (int i = 0; i < strings.length; i++) {
            strings[i] = "" + i;
            h.put(strings[i], h);
        }

        Hashtable<?, ?> hcp = (Hashtable<?, ?>) deepCopy(h);

        for (int i = 0; i < strings.length; i++) {
            Hashtable<?, ?> o = (Hashtable<?, ?>) hcp.get(strings[i]);
            if (o != hcp) {
                System.out.println("Error " + i);
                System.exit(1);
            }
        }
        System.out.println("OK");
    }
}
