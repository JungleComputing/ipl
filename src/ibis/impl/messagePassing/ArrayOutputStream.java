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

    public final boolean finished() {
	return out.completed();
    }

    final public void doFlush() throws IOException {
	out.flush();
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
