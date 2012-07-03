package ibis.ipl.impl.stacking.cache;

import ibis.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;

public class BufferedDataInputStream extends DataInputStream{
    
    CacheReceivePort port;

    BufferedDataInputStream(CacheReceivePort port) {
        this.port = port;
    }

    @Override
    public void close() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long bytesRead() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void resetBytesRead() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean readBoolean() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int bufferSize() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int read() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public byte readByte() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public char readChar() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public short readShort() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int readInt() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long readLong() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public float readFloat() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double readDouble() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void readArray(boolean[] destination, int offset, int length) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void readArray(byte[] destination, int offset, int length) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void readArray(char[] destination, int offset, int length) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void readArray(short[] destination, int offset, int length) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void readArray(int[] destination, int offset, int length) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void readArray(long[] destination, int offset, int length) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void readArray(float[] destination, int offset, int length) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void readArray(double[] destination, int offset, int length) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void readByteBuffer(ByteBuffer value) throws IOException, ReadOnlyBufferException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
