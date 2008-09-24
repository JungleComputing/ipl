/* $Id$ */

package ibis.io.jme;

import java.io.IOException;
import java.io.InputStream;

/**
 * An <code>InputStream</code> that can be placed on top of any existing
 * <code>java.io.InputStream</code>. It adds statistics and prevents
 * a <code>close</code> from propagating to the streams below. You need
 * to use {@link #realClose()} for that.
 */

public class DummyInputStream extends InputStream {

    static final boolean SUPPORT_STATS = true;

    InputStream in;

    long count = 0;

    public DummyInputStream(InputStream in) {
        this.in = in;
    }

    public int read() throws IOException {
        // System.err.println("dummy.read");
        if (SUPPORT_STATS) {
            count++;
        }
        return in.read();
    }

    public int read(byte[] b) throws IOException {
        int res = in.read(b);
        // System.err.println("dummy.read array of len " + b.length
        //         + " result was " + res + " bytes");
        if (SUPPORT_STATS) {
            if (res >= 0) {
                count += res;
            }
        }
        return res;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int res = in.read(b, off, len);
        // System.err.println("dummy.read array of len " + len 
        //         + " result was " + res + " bytes");
        if (SUPPORT_STATS) {
            if (res >= 0) {
                count += res;
            }
        }
        return res;
    }

    public long skip(long n) throws IOException {
        return in.skip(n);
    }

    public int available() throws IOException {
        return in.available();
    }

    /**
     * Dummy close to prevent propagating the close to the underlying
     * streams.
     */
    public void close() {
        /* ignore */
    }

    /**
     * Closes the underlying streams as well.
     */
    public void realClose() throws IOException {
        in.close();
    }

    public void mark(int readlimit) {
        in.mark(readlimit);
    }

    public void reset() throws IOException {
        in.reset();
    }

    public boolean markSupported() {
        return in.markSupported();
    }

    /**
     * Resets the "number of bytes read" counter.
     */ 
    public void resetCount() {
        count = 0;
    }

    /**
     * Returns the number of bytes read from this stream since the last 
     * call to {@link #resetCount} or the beginning of its existence. 
     */
    public long getCount() {
        return count;
    }
}
