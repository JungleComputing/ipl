package ibis.ipl.impl.messagePassing;

import java.io.InputStream;
import ibis.ipl.IbisIOException;

/**
 *
 * Extends InputStream with read of array of primitives
 */

final public class ArrayInputStream
	extends ibis.io.ArrayInputStream {

    private ByteInputStream in;

    public ArrayInputStream(ByteInputStream in) {
	this.in = in;
    }


    public void readArray(boolean[] a, int off, int len) throws IbisIOException {
	in.read(a, off, len);
    }

    public void readArray(byte[] a, int off, int len) throws IbisIOException {
	in.read(a, off, len);
    }

    public void readArray(short[] a, int off, int len) throws IbisIOException {
	in.read(a, off, len);
    }

    public void readArray(char[] a, int off, int len) throws IbisIOException {
	in.read(a, off, len);
    }

    public void readArray(int[] a, int off, int len) throws IbisIOException {
	in.read(a, off, len);
// System.err.println("Read array " + a + " of " + len + " ints, " + len * SIZEOF_INT + " bytes");
// for (int i = 0; i < len; i++) {
    // System.err.print(a[i] + " ");
// }
    }

    public void readArray(long[] a, int off, int len) throws IbisIOException {
	in.read(a, off, len);
    }

    public void readArray(float[] a, int off, int len) throws IbisIOException {
	in.read(a, off, len);
    }

    public void readArray(double[] a, int off, int len) throws IbisIOException {
	in.read(a, off, len);
    }

    public int available() throws IbisIOException {
	throw new IbisIOException("ibis.ipl.impl.messagePassing.available() not implemented");
    }

}
