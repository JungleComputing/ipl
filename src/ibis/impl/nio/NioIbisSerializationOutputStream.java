package ibis.impl.nio;

import ibis.io.Accumulator;
import ibis.io.IbisSerializationOutputStream;

import java.io.IOException;

/**
 * Does "normal" Ibis serialization, but with a different output method, 
 * using an accumulator instead of arrays
 */
final class NioIbisSerializationOutputStream 
		    extends IbisSerializationOutputStream implements Config {

    /**
     * Accumulator we send output to
     */
    private Accumulator accumulator;

    NioIbisSerializationOutputStream(Accumulator accumulator) 
							throws IOException {
	super();

	this.accumulator = accumulator;
    }

    /**
     * {@inheritDoc}
     */
    public String serializationImplName() {
	return "nio-ibis";
    }

    /**
     * {@inheritDoc}
     */
    public void writeArrayBoolean(boolean[] ref, int offset, int len)
							throws IOException {
	accumulator.writeArray(ref, offset, len);
    }

    /**
     * {@inheritDoc}
     */
    public void writeArrayByte(byte[] ref, int offset, int len)
							throws IOException {
	accumulator.writeArray(ref, offset, len);
    }

    /**
     * {@inheritDoc}
     */
    public void writeArrayChar(char[] ref, int offset, int len)
							throws IOException {
	accumulator.writeArray(ref, offset, len);
    }

    /**
     * {@inheritDoc}
     */
    public void writeArrayShort(short[] ref, int offset, int len)
							throws IOException {
	accumulator.writeArray(ref, offset, len);
    }

    /**
     * {@inheritDoc}
     */
    public void writeArrayInt(int[] ref, int offset, int len)
							throws IOException {
	accumulator.writeArray(ref, offset, len);
    }

    /**
     * {@inheritDoc}
     */
    public void writeArrayLong(long[] ref, int offset, int len)
							throws IOException {
	accumulator.writeArray(ref, offset, len);
    }

    /**
     * {@inheritDoc}
     */
    public void writeArrayFloat(float[] ref, int offset, int len)
							throws IOException {
	accumulator.writeArray(ref, offset, len);
    }

    /**
     * {@inheritDoc}
     */
    public void writeArrayDouble(double[] ref, int offset, int len)
							throws IOException {
	accumulator.writeArray(ref, offset, len);
    }

    public void flush() throws IOException {
	accumulator.flush();
    }

    public void writeBoolean(boolean value) throws IOException {
	accumulator.writeBoolean(value);
    }

    public void writeByte(byte value) throws IOException {
	accumulator.writeByte(value);
    }

    public void writeByte(int value) throws IOException {
	accumulator.writeByte((byte) (value & 0xFF));
    }

    public void writeChar(char value) throws IOException {
	accumulator.writeChar(value);
    }

    public void writeShort(short value) throws IOException {
	accumulator.writeShort(value);
    }

    public void writeShort(int value) throws IOException {
	accumulator.writeShort((short) value);
    }

    public void writeInt(int value) throws IOException {
	accumulator.writeInt(value);
    }

    public void writeLong(long value) throws IOException {
	accumulator.writeLong(value);
    }

    public void writeFloat(float value) throws IOException {
	accumulator.writeFloat(value);
    }

    public void writeDouble(double value) throws IOException {
	accumulator.writeDouble(value);
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws IOException {
	accumulator.flush();
    }

}
