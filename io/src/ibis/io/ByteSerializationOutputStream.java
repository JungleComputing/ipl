/* $Id$ */

package ibis.io;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * The <code>ByteSerializationOutputStream</code> class can be used when 
 * only byte serialization is needed.
 * It provides an implementation for the <code>SerializationOutput</code>
 * interface, built on methods in <code>OutputStream</code>.
 * However, the only data that can be sent are bytes and byte arrays.
 * All other methods throw an exception.
 * It also provides a base class for "data" serialization and "ibis"
 * serialization.
 */
public class ByteSerializationOutputStream implements SerializationOutput {

    /** The underlying stream. */
    DataOutputStream out;
    
    /**
     * Constructor.
     *
     * @param s the underlying <code>OutputStream</code>
     * @exception java.io.IOException is thrown when an IO error occurs.
     */
    public ByteSerializationOutputStream(DataOutputStream s)
            throws IOException {
        out = s;
    }

    /**
     * Constructor, may be used when this class is sub-classed.
     */
    protected ByteSerializationOutputStream() throws IOException {
        out = null;
    }

    public String serializationImplName() {
        return "byte";
    }

    public boolean reInitOnNewConnection() {
        return false;
    }

    public void writeBoolean(boolean value) throws IOException {
        throw new IOException("Illegal data type written");
    }

    public void writeByte(byte value) throws IOException {
        out.write(value);
    }

    public void writeChar(char value) throws IOException {
        throw new IOException("Illegal data type written");
    }

    public void writeShort(short value) throws IOException {
        throw new IOException("Illegal data type written");
    }

    public void writeInt(int value) throws IOException {
        throw new IOException("Illegal data type written");
    }

    public void writeLong(long value) throws IOException {
        throw new IOException("Illegal data type written");
    }

    public void writeFloat(float value) throws IOException {
        throw new IOException("Illegal data type written");
    }

    public void writeDouble(double value) throws IOException {
        throw new IOException("Illegal data type written");
    }

    public void writeArray(boolean[] ref, int off, int len) throws IOException {
        throw new IOException("Illegal data type written");
    }

    public void writeArray(byte[] ref, int off, int len) throws IOException {

        /*
         * Call write() and read() here. It is supported.
         * RFHH
         */
        if (off == 0 && len == ref.length) {
            out.write(ref);
        } else {
            throw new IOException("Illegal data type written");
        }
    }

    public void writeArray(short[] ref, int off, int len) throws IOException {
        throw new IOException("Illegal data type written");
    }

    public void writeArray(char[] ref, int off, int len) throws IOException {
        throw new IOException("Illegal data type written");
    }

    public void writeArray(int[] ref, int off, int len) throws IOException {
        throw new IOException("Illegal data type written");
    }

    public void writeArray(long[] ref, int off, int len) throws IOException {
        throw new IOException("Illegal data type written");
    }

    public void writeArray(float[] ref, int off, int len) throws IOException {
        throw new IOException("Illegal data type written");
    }

    public void writeArray(double[] ref, int off, int len) throws IOException {
        throw new IOException("Illegal data type written");
    }

    public void writeArray(Object[] ref, int off, int len) throws IOException {
        throw new IOException("Illegal data type written");
    }

    public void writeArray(boolean[] ref) throws IOException {
        writeArray(ref, 0, ref.length);
    }

    public void writeArray(byte[] ref) throws IOException {
        writeArray(ref, 0, ref.length);
    }

    public void writeArray(short[] ref) throws IOException {
        writeArray(ref, 0, ref.length);
    }

    public void writeArray(char[] ref) throws IOException {
        writeArray(ref, 0, ref.length);
    }

    public void writeArray(int[] ref) throws IOException {
        writeArray(ref, 0, ref.length);
    }

    public void writeArray(long[] ref) throws IOException {
        writeArray(ref, 0, ref.length);
    }

    public void writeArray(float[] ref) throws IOException {
        writeArray(ref, 0, ref.length);
    }

    public void writeArray(double[] ref) throws IOException {
        writeArray(ref, 0, ref.length);
    }

    public void writeArray(Object[] ref) throws IOException {
        writeArray(ref, 0, ref.length);
    }

    public void statistics() {
        // no statistics
    }

    public void reset() throws IOException {
        // does nothing.
    }

    public void reset(boolean cleartypes) throws IOException {
        // does nothing.
    }

    public void flush() throws IOException {
        out.flush();
        // No need to call out.finish() here, since
        // ByteSerializationOutputStream does not support writes that
        // require this.
    }

    public void close() throws IOException {
        flush();
    }

    public void realClose() throws IOException {
        close();
        out.close();
    }

    public void setReplacer(Replacer replacer) throws IOException {
        throw new IOException("no replacer allowed in byte serialization");
    }

    public void writeObject(Object obj) throws IOException {
        throw new IOException("Illegal data type written");
    }

    public void writeString(String obj) throws IOException {
        throw new IOException("Illegal data type written");
    }

    public void writeByteBuffer(ByteBuffer value) throws IOException {
	out.writeByteBuffer(value);
    }
}
