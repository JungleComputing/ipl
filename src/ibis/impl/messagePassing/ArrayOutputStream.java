package ibis.ipl.impl.messagePassing;

import java.io.OutputStream;

import java.io.IOException;

/**
 *
 * Extends OutputStream with write of array of primitives and writeSingleInt
 */

final public class ArrayOutputStream
	extends ibis.io.ArrayOutputStream {

    private ByteOutputStream out;
    private boolean[] touched = new boolean[PRIMITIVE_TYPES];

    public ArrayOutputStream(ByteOutputStream out) {
	if (Ibis.DEBUG) {
	    System.err.println(">>>>>>>>>>>>>>>>>>>>>>> Create a messagePassing.ArrayOutputStream on top of " + out);
	}
	this.out = out;
    }


    final public void flushBuffers() throws IOException {
        indices_short[TYPE_BYTE]    = (short) byte_index;
	indices_short[TYPE_CHAR]    = (short) char_index;
	indices_short[TYPE_SHORT]   = (short) short_index;
	indices_short[TYPE_INT]     = (short) int_index;
	indices_short[TYPE_LONG]    = (short) long_index;
	indices_short[TYPE_FLOAT]   = (short) float_index;
	indices_short[TYPE_DOUBLE]  = (short) double_index;

	//    indices_short[PRIMITIVE_TYPES] = (short)  (eof ? 1 : 0);

	out.writeShortArray(indices_short, 0, PRIMITIVE_TYPES);

	if (byte_index > 0) {
	    out.writeByteArray(byte_buffer, 0, byte_index);
	    touched[TYPE_BYTE] = true;
	}
	if (char_index > 0) {
	    out.writeCharArray(char_buffer, 0, char_index);
	    touched[TYPE_CHAR] = true;
	}
	if (short_index > 0) {
	    out.writeShortArray(short_buffer, 0, short_index);
	    touched[TYPE_SHORT] = true;
	}
	if (int_index > 0) {
       	    out.writeIntArray(int_buffer, 0, int_index);
	    touched[TYPE_INT] = true;
	}
	if (long_index > 0) {
	    out.writeLongArray(long_buffer, 0, long_index);
	    touched[TYPE_LONG] = true;
	}
	if (float_index > 0) {
	    out.writeFloatArray(float_buffer, 0, float_index);
	    touched[TYPE_FLOAT] = true;
	}
	if (double_index > 0) {
	    out.writeDoubleArray(double_buffer, 0, double_index);
	    touched[TYPE_DOUBLE] = true;
	}

	reset_indices();
    }

    /**
     * @inheritDoc
     */
    final public void doWriteArray(Object ref, int offset, int len, int type)
	    throws IOException {
	
	switch (type)	{
	case TYPE_BOOLEAN:
	    out.writeBooleanArray( (boolean[])ref, offset, len);
	    break;
	case TYPE_BYTE:
	    out.writeByteArray( (byte[])ref, offset, len);
	    break;
	case TYPE_CHAR:
	    out.writeCharArray( (char[])ref, offset, len);
	    break;
	case TYPE_SHORT:
	    out.writeShortArray( (short[])ref, offset, len);
	    break;
	case TYPE_INT:
	    out.writeIntArray( (int[])ref, offset, len);
	    break;
	case TYPE_LONG:
	    out.writeLongArray( (long[])ref, offset, len);
	    break;
	case TYPE_FLOAT:
	    out.writeFloatArray( (float[])ref, offset, len);
	    break;
	case TYPE_DOUBLE:
	    out.writeDoubleArray( (double[])ref, offset, len);
	    break;
	}
    }

    final public void flush() throws IOException {
	super.flush();
	out.flush();
// System.err.println(this + ": flush; out " + out + "; out.completed " + out.completed());
	if (! out.completed()) {
	    indices_short = new short[PRIMITIVE_TYPES];
	    if (touched[TYPE_BYTE]) {
		byte_buffer   = new byte[BYTE_BUFFER_SIZE];
	    }
	    if (touched[TYPE_CHAR]) {
		char_buffer   = new char[CHAR_BUFFER_SIZE];
	    }
	    if (touched[TYPE_SHORT]) {
		short_buffer  = new short[SHORT_BUFFER_SIZE];
	    }
	    if (touched[TYPE_INT]) {
		int_buffer    = new int[INT_BUFFER_SIZE];
	    }
	    if (touched[TYPE_LONG]) {
		long_buffer   = new long[LONG_BUFFER_SIZE];
	    }
	    if (touched[TYPE_FLOAT]) {
		float_buffer  = new float[FLOAT_BUFFER_SIZE];
	    }
	    if (touched[TYPE_DOUBLE]) {
		double_buffer = new double[DOUBLE_BUFFER_SIZE];
	    }
	}

	for (int i = 0; i < PRIMITIVE_TYPES; i++) {
	    touched[i] = false;
	}
    }

    final public void finish() throws IOException {
	out.finish();
    }

    final public void close() throws IOException {
	flush();
	out.close();
    }

    public final int bytesWritten() { 
	return out.getCount();
    }


    public final void resetBytesWritten() {
	out.resetCount();
    }
}
