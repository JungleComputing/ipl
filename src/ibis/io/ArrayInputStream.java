package ibis.io;

import java.io.IOException;

/**
 *
 * Extends OutputStream with read of array of primitives and readSingleInt
 */

public abstract class ArrayInputStream
	implements TypeSize {

    public abstract void readArray(boolean[] a, int off, int len) throws IOException;

    public abstract void readArray(byte[] a, int off, int len) throws IOException;

    public abstract void readArray(short[] a, int off, int len) throws IOException;

    public abstract void readArray(char[] a, int off, int len) throws IOException;

    public abstract void readArray(int[] a, int off, int len) throws IOException;

    public abstract void readArray(long[] a, int off, int len) throws IOException;

    public abstract void readArray(float[] a, int off, int len) throws IOException;

    public abstract void readArray(double[] a, int off, int len) throws IOException;

    public abstract int available() throws IOException;

}

