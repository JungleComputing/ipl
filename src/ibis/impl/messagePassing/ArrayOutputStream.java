package ibis.impl.messagePassing;

import java.io.OutputStream;

import java.io.IOException;

/**
 * Implementation of an <code>ibis.io.ArrayOutputStream</code> on top of a
 * <code>ByteOutputStream</code>.
 */
final public class ArrayOutputStream
	extends ibis.io.ArrayOutputStream {

    private final ByteOutputStream out;

    public ArrayOutputStream(ByteOutputStream out) {
	if (Ibis.DEBUG) {
	    System.err.println(">>>>>>>>>>>>>>>>>>>>>>> Create a messagePassing.ArrayOutputStream on top of " + out);
	}
	this.out = out;
    }

    /**
     * @inheritDoc
     */
    final public void writeArray(byte[] ref, int offset, int len)
	    throws IOException {
	out.writeByteArray(ref, offset, len);
    }

    /**
     * @inheritDoc
     */
    final public void writeArray(boolean[] ref, int offset, int len)
	    throws IOException {
	out.writeBooleanArray(ref, offset, len);
    }

    /**
     * @inheritDoc
     */
    final public void writeArray(char[] ref, int offset, int len)
	    throws IOException {
	out.writeCharArray(ref, offset, len);
    }

    /**
     * @inheritDoc
     */
    final public void writeArray(short[] ref, int offset, int len)
	    throws IOException {
	out.writeShortArray(ref, offset, len);
    }

    /**
     * @inheritDoc
     */
    final public void writeArray(int[] ref, int offset, int len)
	    throws IOException {
	out.writeIntArray(ref, offset, len);
    }

    /**
     * @inheritDoc
     */
    final public void writeArray(long[] ref, int offset, int len)
	    throws IOException {
	out.writeLongArray(ref, offset, len);
    }

    /**
     * @inheritDoc
     */
    final public void writeArray(float[] ref, int offset, int len)
	    throws IOException {
	out.writeFloatArray(ref, offset, len);
    }

    /**
     * @inheritDoc
     */
    final public void writeArray(double[] ref, int offset, int len)
	    throws IOException {
	out.writeDoubleArray(ref, offset, len);
    }

    final public void flush() throws IOException {
	out.flush();
    }

    final public boolean finished() {
	return out.completed();
    }

    final public void finish() throws IOException {
	out.finish();
    }

    final public void close() throws IOException {
	flush();
	out.close();
    }

    public final long bytesWritten() { 
	return out.getCount();
    }


    public final void resetBytesWritten() {
	out.resetCount();
    }
}
