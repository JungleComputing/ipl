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
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * The <code>SunSerializationOutputStream</code> class is the "glue" between
 * <code>SerializationOutputStream</code> and <code>ObjectOutputStream</code>.
 * It provides implementations for the abstract methods in
 * <code>SerializationOutputStream</code>, build on methods in
 * <code>ObjectOutputStream</code>. Sun serialization only requires an
 * implementation of the <code>java.io.OutputStream</code> methods in the
 * underlying {@link DataOutputStream}.
 */
public final class SunSerializationOutputStream extends java.io.ObjectOutputStream implements SerializationOutput {

    private Replacer replacer;

    private OutputStream out;

    /**
     * Constructor. Calls constructor of superclass with the
     * <code>DataOutputStream</code> parameter (which is also a
     * <code>java.io.OutputStream</code>) and flushes to send the serialization
     * header out.
     *
     * @param out the <code>OutputStream</code>
     * @exception java.io.IOException is thrown when an IO error occurs.
     */
    public SunSerializationOutputStream(OutputStream out) throws IOException {
        super(new DummyOutputStream(out));

        this.out = out;
        flush();
    }

    public SunSerializationOutputStream(DataOutputStream out) throws IOException {
        super(new DummyOutputStream(out));
        this.out = out;
        flush();
    }

    @Override
    public void reset(boolean cleartypes) throws IOException {
        // Sun serialization always clears the type table on a reset.
        reset();
    }

    /**
     * Returns the name of the current serialization implementation: "sun".
     *
     * @return the name of the current serialization implementation.
     */
    @Override
    public String serializationImplName() {
        return "sun";
    }

    @Override
    public boolean reInitOnNewConnection() {
        return true;
    }

    /**
     * Write a slice of an array of booleans. Warning: duplicates are NOT detected
     * when these calls are used! If the slice consists of the complete array, the
     * complete array is written using <code>writeUnshared</code>. Otherwise a copy
     * is made into an array of length <code>len</code> and that copy is written.
     * This is a bit unfortunate, but a consequence of the Ibis
     * <code>WriteMessage</code> interface.
     *
     * @param ref the array to be written
     * @param off offset in the array from where writing starts
     * @param len the number of elements to be written
     *
     * @exception java.io.IOException is thrown on an IO error.
     */
    @Override
    public void writeArray(boolean[] ref, int off, int len) throws IOException {
        if (off == 0 && len == ref.length) {
            // Use writeUnshared, so that no cycle detection is used ...
            writeUnshared(ref);
        } else {
            boolean[] temp = new boolean[len];
            System.arraycopy(ref, off, temp, 0, len);
            writeObject(temp);
        }
    }

    /**
     * Write a slice of an array of bytes. See
     * {@link #writeArray(boolean[], int, int)} for a description.
     */
    @Override
    public void writeArray(byte[] ref, int off, int len) throws IOException {
        /*
         * Calling write() and read() here turns out to be much, much faster. So, we go
         * ahead and implement a fast path just for byte[]. RFHH
         */
        // write(ref, off, len);
        // No, we should be able to read the result back with readObject!
        // (Ceriel)
        if (off == 0 && len == ref.length) {
            writeUnshared(ref);
        } else {
            byte[] temp = new byte[len];
            System.arraycopy(ref, off, temp, 0, len);
            writeObject(temp);
        }
    }

    /**
     * Write a slice of an array of shorts. See
     * {@link #writeArray(boolean[], int, int)} for a description.
     */
    @Override
    public void writeArray(short[] ref, int off, int len) throws IOException {
        if (off == 0 && len == ref.length) {
            writeUnshared(ref);
        } else {
            short[] temp = new short[len];
            System.arraycopy(ref, off, temp, 0, len);
            writeObject(temp);
        }
    }

    /**
     * Write a slice of an array of chars. See
     * {@link #writeArray(boolean[], int, int)} for a description.
     */
    @Override
    public void writeArray(char[] ref, int off, int len) throws IOException {
        if (off == 0 && len == ref.length) {
            writeUnshared(ref);
        } else {
            char[] temp = new char[len];
            System.arraycopy(ref, off, temp, 0, len);
            writeObject(temp);
        }
    }

    /**
     * Write a slice of an array of ints. See
     * {@link #writeArray(boolean[], int, int)} for a description.
     */
    @Override
    public void writeArray(int[] ref, int off, int len) throws IOException {
        if (off == 0 && len == ref.length) {
            writeUnshared(ref);
        } else {
            int[] temp = new int[len];
            System.arraycopy(ref, off, temp, 0, len);
            writeObject(temp);
        }
    }

    /**
     * Write a slice of an array of longs. See
     * {@link #writeArray(boolean[], int, int)} for a description.
     */
    @Override
    public void writeArray(long[] ref, int off, int len) throws IOException {
        if (off == 0 && len == ref.length) {
            writeUnshared(ref);
        } else {
            long[] temp = new long[len];
            System.arraycopy(ref, off, temp, 0, len);
            writeObject(temp);
        }
    }

    /**
     * Write a slice of an array of floats. See
     * {@link #writeArray(boolean[], int, int)} for a description.
     */
    @Override
    public void writeArray(float[] ref, int off, int len) throws IOException {
        if (off == 0 && len == ref.length) {
            writeUnshared(ref);
        } else {
            float[] temp = new float[len];
            System.arraycopy(ref, off, temp, 0, len);
            writeObject(temp);
        }
    }

    /**
     * Write a slice of an array of doubles. See
     * {@link #writeArray(boolean[], int, int)} for a description.
     */
    @Override
    public void writeArray(double[] ref, int off, int len) throws IOException {
        if (off == 0 && len == ref.length) {
            writeUnshared(ref);
        } else {
            double[] temp = new double[len];
            System.arraycopy(ref, off, temp, 0, len);
            writeObject(temp);
        }
    }

    /**
     * Write a slice of an array of Objects. See
     * {@link #writeArray(boolean[], int, int)} for a description.
     */
    @Override
    public void writeArray(Object[] ref, int off, int len) throws IOException {
        if (off == 0 && len == ref.length) {
            writeUnshared(ref);
        } else {
            Object[] temp = new Object[len];
            System.arraycopy(ref, off, temp, 0, len);
            writeObject(temp);
        }
    }

    @Override
    public void writeArray(boolean[] ref) throws IOException {
        writeUnshared(ref);
    }

    @Override
    public void writeArray(byte[] ref) throws IOException {
        writeUnshared(ref);
    }

    @Override
    public void writeArray(short[] ref) throws IOException {
        writeUnshared(ref);
    }

    @Override
    public void writeArray(char[] ref) throws IOException {
        writeUnshared(ref);
    }

    @Override
    public void writeArray(int[] ref) throws IOException {
        writeUnshared(ref);
    }

    @Override
    public void writeArray(long[] ref) throws IOException {
        writeUnshared(ref);
    }

    @Override
    public void writeArray(float[] ref) throws IOException {
        writeUnshared(ref);
    }

    @Override
    public void writeArray(double[] ref) throws IOException {
        writeUnshared(ref);
    }

    @Override
    public void writeArray(Object[] ref) throws IOException {
        writeUnshared(ref);
    }

    @Override
    public void writeString(String ref) throws IOException {
        writeObject(ref);
    }

    @Override
    public void writeShort(short r) throws IOException {
        super.writeShort(r);
    }

    @Override
    public void writeChar(char r) throws IOException {
        super.writeChar(r);
    }

    @Override
    public void writeByte(byte r) throws IOException {
        super.writeByte(r);
    }

    /**
     * No statistics are printed for the Sun serialization version.
     */
    @Override
    public void statistics() {
        // no statistics for Sun serialization.
    }

    @Override
    public void realClose() throws IOException {
        close();
        out.close();
    }

    /**
     * Set a replacer. The replacement mechanism can be used to replace an object
     * with another object during serialization. This is used in RMI, for instance,
     * to replace a remote object with a stub. The replacement mechanism provided
     * here is independent of the serialization implementation (Ibis serialization,
     * Sun serialization).
     *
     * @param replacer the replacer object to be associated with this output stream
     *
     * @exception java.io.IOException is thrown when enableReplaceObject throws an
     *                                exception.
     */
    @Override
    public void setReplacer(Replacer replacer) throws IOException {
        try {
            enableReplaceObject(true);
        } catch (Throwable e) {
            // May throw a SecurityException.
            // Don't know how to deal with that.
            throw new IbisIOException("enableReplaceObject threw exception: ", e);
        }
        this.replacer = replacer;
    }

    /**
     * Object replacement for Sun serialization. This method gets called by Sun
     * object serialization when replacement is enabled.
     *
     * @param obj the object to be replaced
     * @return the result of the object replacement
     */
    @Override
    protected Object replaceObject(Object obj) {
        if (obj != null && replacer != null) {
            obj = replacer.replace(obj);
        }
        return obj;
    }

    @Override
    public void writeByteBuffer(ByteBuffer value) throws IOException {
        int len = value.limit() - value.position();
        if (value.hasArray()) {
            writeArray(value.array(), value.arrayOffset(), len);
        } else {
            byte[] temp = new byte[len];
            value.get(temp);
            writeArray(temp);
        }
    }
}
