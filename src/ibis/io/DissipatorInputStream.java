package ibis.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * A inputstream which uses a <code>IbisDissipator</code> as the
 * underlying implementation.
 */
public final class DissipatorInputStream extends InputStream {

    /**
     * The underlying dissipator.
     */
    private IbisDissipator in;

    /**
     * Constructor.
     * @param in	the underlying dissipator.
     */
    public DissipatorInputStream(IbisDissipator in) {
	super();
	this.in = in;
    }

    /**
     * {@inheritDoc}
     */
    public int available() throws IOException {
	return in.available();
    }

    /**
     * {@inheritDoc}
     */
    public int read() throws IOException {
	return (int)in.readByte();
    }

    /**
     * {@inheritDoc}
     */
    public int read(byte[] b) throws IOException {
	in.readArray(b, 0, b.length);
	return b.length;
    }

    /**
     * {@inheritDoc}
     */
    public int read(byte[] b, int off, int len) throws IOException {
	in.readArray(b, off, len);
	return b.length;
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws IOException {
	in.close();
    }
}
