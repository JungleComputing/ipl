package ibis.ipl.impl.messagePassing;

import java.io.OutputStream;
import ibis.ipl.IbisIOException;

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


    final public void writeArray(boolean[] a, int off, int len)
	    throws IbisIOException {
	out.writeBooleanArray(a, off, len);
    }

    final public void writeArray(byte[] a, int off, int len)
	    throws IbisIOException {
	out.writeByteArray(a, off, len);
    }

    final public void writeArray(short[] a, int off, int len)
	    throws IbisIOException {
// System.err.println("Write messagePassing.ArrayOutputStream short[" + off + ":" + len +"]");
	out.writeShortArray(a, off, len);
    }

    final public void writeArray(char[] a, int off, int len)
	    throws IbisIOException {
	out.writeCharArray(a, off, len);
    }

    final public void writeArray(int[] a, int off, int len)
	    throws IbisIOException {
	out.writeIntArray(a, off, len);
    }

    final public void writeArray(long[] a, int off, int len)
	    throws IbisIOException {
	out.writeLongArray(a, off, len);
    }

    final public void writeArray(float[] a, int off, int len)
	    throws IbisIOException {
	out.writeFloatArray(a, off, len);
    }

    final public void writeArray(double[] a, int off, int len)
	    throws IbisIOException {
	out.writeDoubleArray(a, off, len);
    }


    final public void flushBuffers() throws IbisIOException {
        indices_short[TYPE_BYTE]    = (short) byte_index;
	indices_short[TYPE_CHAR]    = (short) char_index;
	indices_short[TYPE_SHORT]   = (short) short_index;
	indices_short[TYPE_INT]     = (short) int_index;
	indices_short[TYPE_LONG]    = (short) long_index;
	indices_short[TYPE_FLOAT]   = (short) float_index;
	indices_short[TYPE_DOUBLE]  = (short) double_index;
	indices_short[TYPE_HANDLE]  = (short) handle_index;

	//    indices_short[PRIMITIVE_TYPES] = (short)  (eof ? 1 : 0);

	writeArray(indices_short, 0, PRIMITIVE_TYPES);

	if (byte_index > 0) {
	    writeArray(byte_buffer, 0, byte_index);
	    touched[TYPE_BYTE] = true;
	}
	if (char_index > 0) {
	    writeArray(char_buffer, 0, char_index);
	    touched[TYPE_CHAR] = true;
	}
	if (short_index > 0) {
	    writeArray(short_buffer, 0, short_index);
	    touched[TYPE_SHORT] = true;
	}
	if (int_index > 0) {
       	    writeArray(int_buffer, 0, int_index);
	    touched[TYPE_INT] = true;
	}
	if (long_index > 0) {
	    writeArray(long_buffer, 0, long_index);
	    touched[TYPE_LONG] = true;
	}
	if (float_index > 0) {
	    writeArray(float_buffer, 0, float_index);
	    touched[TYPE_FLOAT] = true;
	}
	if (double_index > 0) {
	    writeArray(double_buffer, 0, double_index);
	    touched[TYPE_DOUBLE] = true;
	}
	if (handle_index > 0) {
	    writeArray(handle_buffer, 0, handle_index);
	    touched[TYPE_HANDLE] = true;
	}

	reset_indices();
    }


    final public void flush() throws IbisIOException {
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
	    if (touched[TYPE_HANDLE]) {
		handle_buffer = new int[HANDLE_BUFFER_SIZE];
	    }
	}

	for (int i = 0; i < PRIMITIVE_TYPES; i++) {
	    touched[i] = false;
	}
    }

    final public void finish() throws IbisIOException {
	out.finish();
    }

    final public void close() throws IbisIOException {
	out.close();
    }

    public final int bytesWritten() { 
	return -1; // please implement --Rob
    }


    public final void resetBytesWritten() {
	// please implement --Rob
    }
}
