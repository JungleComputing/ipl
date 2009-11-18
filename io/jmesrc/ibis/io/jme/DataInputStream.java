/* $Id$ */

package ibis.io.jme;

import java.io.IOException;

/**
 * A general data input stream.
 * Provides for methods to read data from an underlying implementation.
 * Calls to read functions may block until data is available.
 */
public abstract class DataInputStream extends java.io.InputStream
        implements DataInput {

    public abstract void close() throws IOException;

    /**
     * Returns the number of bytes read from the stream 
     * since the last reset of this counter.
     * @return The number of bytes read.
     */
    public abstract long bytesRead();

    /**
     * Resets the counter for the number of bytes read.
     */
    public abstract void resetBytesRead();

    /**
     * Reads a boolean value from the stream.
     * @return	The boolean read.
     */
    public abstract boolean readBoolean() throws IOException;

    public int readUnsignedShort() throws IOException {
        return readShort() & 0177777;
    }

    public int readUnsignedByte() throws IOException {
        return readByte() & 0377;
    }

    public void readArray(boolean[] source) throws IOException {
        readArray(source, 0, source.length);
    }

    public void readArray(byte[] source) throws IOException {
        readArray(source, 0, source.length);
    }

    public void readArray(char[] source) throws IOException {
        readArray(source, 0, source.length);
    }

    public void readArray(short[] source) throws IOException {
        readArray(source, 0, source.length);
    }

    public void readArray(int[] source) throws IOException {
        readArray(source, 0, source.length);
    }

    public void readArray(long[] source) throws IOException {
        readArray(source, 0, source.length);
    }

    public void readArray(float[] source) throws IOException {
        readArray(source, 0, source.length);
    }

    public void readArray(double[] source) throws IOException {
        readArray(source, 0, source.length);
    }
    
    public abstract int bufferSize();
}
