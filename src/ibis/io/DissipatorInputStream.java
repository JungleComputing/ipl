/* $Id$ */

package ibis.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * A inputstream which uses a <code>Dissipator</code> as the
 * underlying implementation.
 */
public final class DissipatorInputStream extends InputStream {

    /** The underlying dissipator. */
    private Dissipator in;

    /**
     * Constructor.
     * @param in	the underlying dissipator.
     */
    public DissipatorInputStream(Dissipator in) {
        super();
        this.in = in;
    }

    public int available() throws IOException {
        return in.available();
    }

    public int read() throws IOException {
        return in.readByte();
    }

    public int read(byte[] b) throws IOException {
        in.readArray(b, 0, b.length);
        return b.length;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        in.readArray(b, off, len);
        return len;
    }

    public void close() throws IOException {
        in.close();
    }
}