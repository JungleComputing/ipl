package ibis.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A <code>OutputStream</code> which uses a <code>Accumulator</code> as
 * the underlying implementation.
 */
public final class AccumulatorOutputStream extends OutputStream {

    /**
     * The underlying <code>Accumulator</code>.
     */
    private Accumulator out;

    /**
     * Constructor. Calls parameterless constructor of superclass.
     *
     * @param out	the underlying <code>Accumulator</code>
     */
    public AccumulatorOutputStream(Accumulator out) {
	super();
	this.out = out;
    }

    /**
     * Closes this <code>OutputStream</code>, by closing the underlying
     * <code>Accumulator</code>.
     *
     * @exception IOException when an IO error occurs.
     */
    public void close() throws IOException {
	out.close();
    }

    /**
     * Flushes this <code>OutputStream</code>, by flushing the underlying
     * <code>Accumulator</code>.
     *
     * @exception IOException when an IO error occurs.
     */
    public void flush() throws IOException {
	out.flush();
    }

    /**
     * Writes a single byte to this <code>OutputStream</code>, by
     * writing it to the underlying <code>Accumulator</code>.
     *
     * @exception IOException when an IO error occurs.
     */
    public void write(int b) throws IOException {
	out.writeByte((byte)b);
    }

    /**
     * Writes an array of bytes to this <code>OutputStream</code>, by
     * writing it to the underlying <code>Accumulator</code>.
     *
     * @exception IOException when an IO error occurs.
     */
    public void write(byte[] b) throws IOException {
	out.writeArray(b, 0, b.length);
    }

    /**
     * Writes a slice of an array of bytes to this <code>OutputStream</code>,
     * by writing it to the underlying <code>Accumulator</code>.
     *
     * @exception IOException when an IO error occurs.
     */
    public void write(byte[] b, int off, int len) throws IOException {
	out.writeArray(b, off, len);
    }
}
