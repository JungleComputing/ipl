/* $Id$ */

package ibis.io;

import java.io.EOFException;
import java.io.IOException;

/**
 * The <code>ByteSerializationInputStream</code> class can be used when 
 * only byte serialization is needed.
 * It provides an implementation for the <code>SerializationInput</code>
 * interface, built on methods in <code>InputStream</code>.
 * However, the only data that can be read are bytes and byte arrays.
 * All other methods throw an exception.
 * It also provides a base class for "data" serialization and "ibis"
 * serialization.
 */
public class ByteSerializationInputStream extends SerializationBase
        implements SerializationInput {

    /** The underlying stream. */
    DataInputStream in;

    /**
     * Constructor, may be used when this class is sub-classed.
     */
    protected ByteSerializationInputStream() throws IOException {
        in = null;
    }

    /**
     * Constructor.
     *
     * @param s the underlying <code>InputStream</code>
     * @exception IOException is thrown on an IO error.
     */
    public ByteSerializationInputStream(DataInputStream s) throws IOException {
        in = s;
    }

    public int available() throws IOException {
        return in.available();
    }

    public String serializationImplName() {
        return "byte";
    }

    public boolean reInitOnNewConnection() {
        return false;
    }

    public void clear() {
        // Nothing for byte serialization.
    }

    public void statistics() {
        // no statistics for byte serialization.
    }

    public byte readByte() throws IOException {
        int b = in.read();

        if (b == -1) {
            throw new EOFException("end of file reached");
        }
        return (byte) b;
    }

    public int readUnsignedByte() throws IOException {
        return readByte() & 0377;
    }

    public boolean readBoolean() throws IOException {
        throw new IOException("Illegal data type read");
    }

    public char readChar() throws IOException {
        throw new IOException("Illegal data type read");
    }

    public short readShort() throws IOException {
        throw new IOException("Illegal data type read");
    }

    public int readUnsignedShort() throws IOException {
        return readShort() & 0177777;
    }

    public int readInt() throws IOException {
        throw new IOException("Illegal data type read");
    }

    public long readLong() throws IOException {
        throw new IOException("Illegal data type read");
    }

    public float readFloat() throws IOException {
        throw new IOException("Illegal data type read");
    }

    public double readDouble() throws IOException {
        throw new IOException("Illegal data type read");
    }

    public String readString() throws IOException {
        throw new IOException("Illegal data type read");
    }

    public Object readObject() throws IOException, ClassNotFoundException {
        throw new IOException("Illegal data type read");
    }

    public void readArray(byte[] ref, int off, int len) throws IOException {
        /*
         * Call read() and read() here. It is supported.
         * RFHH
         */
        if (off == 0 && ref.length == len) {
            int rd = 0;
            do {
                rd += in.read(ref, rd, len - rd);
            } while (rd < len);
            return;
        }
        throw new IOException("Illegal data type read");
    }

    public void readArray(boolean[] ref, int off, int len) throws IOException {
        throw new IOException("Illegal data type read");
    }

    public void readArray(char[] ref, int off, int len) throws IOException {
        throw new IOException("Illegal data type read");
    }

    public void readArray(short[] ref, int off, int len) throws IOException {
        throw new IOException("Illegal data type read");
    }

    public void readArray(int[] ref, int off, int len) throws IOException {
        throw new IOException("Illegal data type read");
    }

    public void readArray(long[] ref, int off, int len) throws IOException {
        throw new IOException("Illegal data type read");
    }

    public void readArray(float[] ref, int off, int len) throws IOException {
        throw new IOException("Illegal data type read");
    }

    public void readArray(double[] ref, int off, int len) throws IOException {
        throw new IOException("Illegal data type read");
    }

    public void readArray(Object[] ref, int off, int len)
            throws IOException, ClassNotFoundException {
        throw new IOException("Illegal data type read");
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

    public void close() throws IOException {
        // nothing
    }

    public void realClose() throws IOException {
        close();
        in.close();
    }

}
