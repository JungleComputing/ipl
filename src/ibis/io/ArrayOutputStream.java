package ibis.io;

import java.io.IOException;

/**
 *
 * Extends OutputStream with write of array of primitives and writeSingleInt
 */

public abstract class ArrayOutputStream implements TypeSize {

    public abstract void writeArray(boolean[] a, int off, int len) throws IOException;

    public abstract void writeArray(byte[] a, int off, int len) throws IOException;
	    
    public abstract void writeArray(short[] a, int off, int len) throws IOException;

    public abstract void writeArray(char[] a, int off, int len) throws IOException;

    public abstract void writeArray(int[] a, int off, int len) throws IOException;

    public abstract void writeArray(long[] a, int off, int len) throws IOException;
    
    public abstract void writeArray(float[] a, int off, int len) throws IOException;

    public abstract void writeArray(double[] a, int off, int len) throws IOException;

    public abstract void flush() throws IOException;

}
