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
	while (len > 0) {
	    int rd = in.read(a, off, len);
	    len -= rd;
	    off += rd;
	}
    }

    public void readArray(byte[] a, int off, int len) throws IbisIOException {
	while (len > 0) {
	    int rd = in.read(a, off, len);
	    len -= rd;
	    off += rd;
	}
    }

    public void readArray(short[] a, int off, int len) throws IbisIOException {
	while (len > 0) {
	    int rd = in.read(a, off, len);
	    len -= rd;
	    off += rd;
	}
    }

    public void readArray(char[] a, int off, int len) throws IbisIOException {
	while (len > 0) {
	    int rd = in.read(a, off, len);
	    len -= rd;
	    off += rd;
	}
    }

    public void readArray(int[] a, int off, int len) throws IbisIOException {
	while (len > 0) {
	    int rd = in.read(a, off, len);
	    len -= rd;
	    off += rd;
	}
    }

    public void readArray(long[] a, int off, int len) throws IbisIOException {
	while (len > 0) {
	    int rd = in.read(a, off, len);
	    len -= rd;
	    off += rd;
	}
    }

    public void readArray(float[] a, int off, int len) throws IbisIOException {
	while (len > 0) {
	    int rd = in.read(a, off, len);
	    len -= rd;
	    off += rd;
	}
    }

    public void readArray(double[] a, int off, int len) throws IbisIOException {
	while (len > 0) {
	    int rd = in.read(a, off, len);
	    len -= rd;
	    off += rd;
	}
    }

    public int available() throws IbisIOException {
	throw new IbisIOException("ibis.ipl.impl.messagePassing.available() not implemented");
    }

}
