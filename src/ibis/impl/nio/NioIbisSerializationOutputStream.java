package ibis.impl.nio;

import java.io.IOException;

import ibis.io.IbisSerializationInputStream;
import ibis.io.IbisSerializationOutputStream;

import ibis.ipl.IbisError;

import ibis.io.Accumulator;

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
    public void writeArray(Object ref, int offset, int len, int type)
							throws IOException {
	switch(type) {
	    case TYPE_BOOLEAN:
		accumulator.writeArray((boolean[]) ref, offset, len);
		break;
	    case TYPE_BYTE:
		accumulator.writeArray((byte[]) ref, offset, len);
		break;
	    case TYPE_CHAR:
		accumulator.writeArray((char[]) ref, offset, len);
		break;
	    case TYPE_SHORT:
		accumulator.writeArray((short[]) ref, offset, len);
		break;
	    case TYPE_INT:
		accumulator.writeArray((int[]) ref, offset, len);
		break;
	    case TYPE_LONG:
		accumulator.writeArray((long[]) ref, offset, len);
		break;
	    case TYPE_FLOAT:
		accumulator.writeArray((float[]) ref, offset, len);
		break;
	    case TYPE_DOUBLE:
		accumulator.writeArray((double[]) ref, offset, len);
		break;
	    default:
		throw new IbisError("unknown array type");
	}
    }

    public void flush() throws IOException {
	accumulator.flush();
    }

    public void writeBoolean(boolean value) throws IOException {
	accumulator.writeBoolean(value);
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
//	accumulator.close();
    }

}
