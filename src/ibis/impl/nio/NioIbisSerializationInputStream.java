package ibis.impl.nio;

import ibis.io.Dissipator;
import ibis.io.IbisSerializationInputStream;

import java.io.IOException;


/**
 * Does "normal" Ibis serialization, but with a different output method,
 * using (Nio)Buffers instead of arrays
 */
final class NioIbisSerializationInputStream extends IbisSerializationInputStream implements Config { 

    /**
     * Source for data
     */
    final Dissipator dissipator;

    NioIbisSerializationInputStream(Dissipator dissipator) throws IOException {
	super();

	this.dissipator = dissipator;

    }

    public String serializationImplName() {
	return "test-nio-ibis";
    }

    public int available() throws IOException {
	return dissipator.available();
    }

    public void close() throws IOException {
	dissipator.close();
    }

    public boolean readBoolean() throws IOException {
	return dissipator.readBoolean();
    }

    public byte readByte() throws IOException {
	return dissipator.readByte();
    }

    public char readChar() throws IOException {
	return dissipator.readChar();
    }

    public short readShort() throws IOException {
	return dissipator.readShort();
    }

    public int readInt() throws IOException {
	return dissipator.readInt();
    }

    public long readLong() throws IOException {
	return dissipator.readLong();
    }

    public float readFloat() throws IOException {
	return dissipator.readFloat();
    }

    public double readDouble() throws IOException {
	return dissipator.readDouble();
    }

    protected void readBooleanArray(boolean ref[], int off, int len) throws IOException {
	dissipator.readArray(ref, off, len);
    }


    protected void readByteArray(byte ref[], int off, int len) throws IOException {
	dissipator.readArray(ref, off, len);
    }

    protected void readCharArray(char ref[], int off, int len) throws IOException {
	dissipator.readArray(ref, off, len);
    }

    protected void readShortArray(short ref[], int off, int len) throws IOException {
	dissipator.readArray(ref, off, len);
    }

    protected void readIntArray(int ref[], int off, int len) throws IOException {
	dissipator.readArray(ref, off, len);
    }

    protected void readLongArray(long ref[], int off, int len) throws IOException {
	dissipator.readArray(ref, off, len);
    }

    protected void readFloatArray(float ref[], int off, int len) throws IOException {
	dissipator.readArray(ref, off, len);
    }

    protected void readDoubleArray(double ref[], int off, int len) throws IOException {
	dissipator.readArray(ref, off, len);
    }
}
