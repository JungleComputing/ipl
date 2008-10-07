/* $Id$ */

package ibis.io.jme;

import java.io.IOException;
import java.io.OutputStream;

/**
 * An <code>OutputStream</code> that can be placed on top of any existing
 * <code>java.io.OutputStream</code>. It adds statistics and prevents
 * a <code>close</code> from propagating to the streams below. You need
 * to use {@link #realClose()} for that.
 */
public class DummyOutputStream extends OutputStream {

    private static final boolean SUPPORT_STATS = true;

    private OutputStream out;

    private long count = 0;

    public DummyOutputStream(OutputStream out) {
        this.out = out;
    }

    public void write(int b) throws IOException {
        out.write(b);

        if (SUPPORT_STATS) {
            count++;
        }
    }

    public void write(byte[] b) throws IOException {
        out.write(b);

        if (SUPPORT_STATS) {
            count += b.length;
        }
    }

    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);

        if (SUPPORT_STATS) {
            count += len;
        }
    }

    public void flush() throws IOException {
        out.flush();
    }

    /**
     * Dummy close to prevent propagating the close to the underlying
     * streams.
     */
    public void close() {
        /* Don't propagate the close, otherwise we close the underlying
         * socket, and that is not what we want here.
         */
    }

    /**
     * Closes the underlying streams as well.
     */
    public void realClose() throws IOException {
        out.close();
    }

    /**
     * Resets the "number of bytes written" counter.
     */
    public void resetCount() {
        count = 0;
    }

    /**
     * Returns the number of bytes written to this stream since the last
     * call to {@link #resetCount} or the beginning of its existence.
     */
    public long getCount() {
        return count;
    }
}
