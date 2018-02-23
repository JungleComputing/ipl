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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * The <code>SunSerializationInputStream</code> class is the "glue" between
 * <code>SerializationInputStream</code> and <code>ObjectInputStream</code>.
 * It provides implementations for the abstract methods in
 * <code>SerializationInputStream</code>, built on methods in
 * <code>ObjectInputStream</code>.
 */
public final class SunSerializationInputStream
        extends java.io.ObjectInputStream implements SerializationInput {

    private InputStream in;

    /**
     * Constructor. Calls constructor of superclass.
     *
     * @param s the underlying <code>DataInputStream</code>
     * @exception IOException when an IO error occurs.
     */
    public SunSerializationInputStream(InputStream s) throws IOException {
        super(new DummyInputStream(s));
        this.in = s;
    }

    public SunSerializationInputStream(DataInputStream s) throws IOException {
        super(new DummyInputStream(s));
        this.in = s;
    }

    /**
     * Returns the name of the current serialization implementation: "sun".
     *
     * @return the name of the current serialization implementation.
     */
    public String serializationImplName() {
        return "sun";
    }

    public boolean reInitOnNewConnection() {
        return true;
    }

    /**
     * Dummy reset. For Ibis, we want to be able to remove the object table in
     * a SerializationInputStream.
     * With Sun serialization, this is accomplished by sending a RESET to it.
     * For Ibis serialization, we cannot do this because we can only send
     * a RESET when a handle is expected.
     */
    public void clear() {
        // Not needed for Sun serialization.
    }

    /**
     * No statistics are printed for the Sun serialization version.
     */
    public void statistics() {
        // No statistics for Sun serialization.
    }

    /**
     * Read a slice of an array of booleans. A consequence of the Ibis
     * <code>ReadMessage</code> interface is that a copy has to be made here,
     * because <code>ObjectInputStream</code> has no mechanism to read an array
     * "in place".
     *
     * @param ref the array to be read
     * @param off offset in the array from where reading starts
     * @param len the number of elements to be read
     *
     * @exception IOException when something is wrong.
     */
    public void readArray(boolean[] ref, int off, int len) throws IOException {
        try {
            boolean[] temp = (boolean[]) readObject();
            if (temp.length != len) {
                throw new ArrayIndexOutOfBoundsException(
                        "Received sub array has wrong len");
            }
            System.arraycopy(temp, 0, ref, off, len);
        } catch (ClassNotFoundException f) {
            throw new SerializationError("class 'boolean[]' not found", f);
        }
    }

    /**
     * Read a slice of an array of bytes.
     * See {@link #readArray(boolean[], int, int)} for a description.
     */
    public void readArray(byte[] ref, int off, int len) throws IOException {
        /*
         * Calling write() and read() here turns out to be much, much faster.
         * So, we go ahead and implement a fast path just for byte[].
         * RFHH
         */
        /*
        int rd = 0;
        do {
            rd += read(ref, off + rd, len - rd);
        } while (rd < len);

        No, not good. It is written in such a way that it can be read back
        with readObject(). (Ceriel)
        */
        try {
            byte[] temp = (byte[]) readObject();
            if (temp.length != len) {
                throw new ArrayIndexOutOfBoundsException(
                        "Received sub array has wrong len");
            }
            System.arraycopy(temp, 0, ref, off, len);
        } catch (ClassNotFoundException f) {
            throw new SerializationError("class 'byte[]' not found", f);
        }
    }
    
    public void readByteBuffer(ByteBuffer b) throws IOException {
	int len = b.limit() - b.position();
	byte[] temp;
	try {
            temp = (byte[]) readObject();
            if (temp.length != len) {
                throw new ArrayIndexOutOfBoundsException(
                        "Received sub array has wrong len");
            }
	} catch (ClassNotFoundException f) {
            throw new SerializationError("class 'byte[]' not found", f);
        }
	b.put(temp);
    }

    /**
     * Read a slice of an array of chars.
     * See {@link #readArray(boolean[], int, int)} for a description.
     */
    public void readArray(char[] ref, int off, int len) throws IOException {
        try {
            char[] temp = (char[]) readObject();
            if (temp.length != len) {
                throw new ArrayIndexOutOfBoundsException(
                        "Received sub array has wrong len");
            }
            System.arraycopy(temp, 0, ref, off, len);
        } catch (ClassNotFoundException f) {
            throw new SerializationError("class 'char[]' not found", f);
        }
    }

    /**
     * Read a slice of an array of shorts.
     * See {@link #readArray(boolean[], int, int)} for a description.
     */
    public void readArray(short[] ref, int off, int len) throws IOException {
        try {
            short[] temp = (short[]) readObject();
            if (temp.length != len) {
                throw new ArrayIndexOutOfBoundsException(
                        "Received sub array has wrong len");
            }
            System.arraycopy(temp, 0, ref, off, len);
        } catch (ClassNotFoundException f) {
            throw new SerializationError("class 'short[]' not found", f);
        }
    }

    /**
     * Read a slice of an array of ints.
     * See {@link #readArray(boolean[], int, int)} for a description.
     */
    public void readArray(int[] ref, int off, int len) throws IOException {
        try {
            int[] temp = (int[]) readObject();
            if (temp.length != len) {
                throw new ArrayIndexOutOfBoundsException(
                        "Received sub array has wrong len");
            }
            System.arraycopy(temp, 0, ref, off, len);
        } catch (ClassNotFoundException f) {
            throw new SerializationError("class 'int[]' not found", f);
        }
    }

    /**
     * Read a slice of an array of longs.
     * See {@link #readArray(boolean[], int, int)} for a description.
     */
    public void readArray(long[] ref, int off, int len) throws IOException {
        try {
            long[] temp = (long[]) readObject();
            if (temp.length != len) {
                throw new ArrayIndexOutOfBoundsException(
                        "Received sub array has wrong len");
            }
            System.arraycopy(temp, 0, ref, off, len);
        } catch (ClassNotFoundException f) {
            throw new SerializationError("class 'long[]' not found", f);
        }
    }

    /**
     * Read a slice of an array of floats.
     * See {@link #readArray(boolean[], int, int)} for a description.
     */
    public void readArray(float[] ref, int off, int len) throws IOException {
        try {
            float[] temp = (float[]) readObject();
            if (temp.length != len) {
                throw new ArrayIndexOutOfBoundsException(
                        "Received sub array has wrong len");
            }
            System.arraycopy(temp, 0, ref, off, len);
        } catch (ClassNotFoundException f) {
            throw new SerializationError("class 'float[]' not found", f);
        }
    }

    /**
     * Read a slice of an array of doubles.
     * See {@link #readArray(boolean[], int, int)} for a description.
     */
    public void readArray(double[] ref, int off, int len) throws IOException {
        try {
            double[] temp = (double[]) readObject();
            if (temp.length != len) {
                throw new ArrayIndexOutOfBoundsException(
                        "Received sub array has wrong len");
            }
            System.arraycopy(temp, 0, ref, off, len);
        } catch (ClassNotFoundException f) {
            throw new SerializationError("class 'double[]' not found", f);
        }
    }

    /**
     * Read a slice of an array of Objects.
     * See {@link #readArray(boolean[], int, int)} for a description.
     */
    public void readArray(Object[] ref, int off, int len) throws IOException,
            ClassNotFoundException {
        Object[] temp = (Object[]) readObject();
        if (temp.length != len) {
            throw new ArrayIndexOutOfBoundsException(
                    "Received sub array has wrong len");
        }
        System.arraycopy(temp, 0, ref, off, len);
    }

    public void readArray(boolean[] ref) throws IOException {
        readArray(ref, 0, ref.length);
    }

    public void readArray(byte[] ref) throws IOException {
        readArray(ref, 0, ref.length);
    }

    public void readArray(short[] ref) throws IOException {
        readArray(ref, 0, ref.length);
    }

    public void readArray(char[] ref) throws IOException {
        readArray(ref, 0, ref.length);
    }

    public void readArray(int[] ref) throws IOException {
        readArray(ref, 0, ref.length);
    }

    public void readArray(long[] ref) throws IOException {
        readArray(ref, 0, ref.length);
    }

    public void readArray(float[] ref) throws IOException {
        readArray(ref, 0, ref.length);
    }

    public void readArray(double[] ref) throws IOException {
        readArray(ref, 0, ref.length);
    }

    public void readArray(Object[] ref)
            throws IOException, ClassNotFoundException {
        readArray(ref, 0, ref.length);
    }

    public String readString() throws IOException {
        try {
            return (String) readObject();
        } catch (ClassNotFoundException e) {
            throw new SerializationError("class 'String' not found", e);
        }
    }

    public void realClose() throws IOException {
        close();
        in.close();
    }
}
