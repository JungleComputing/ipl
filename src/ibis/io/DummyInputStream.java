package ibis.io;

import java.io.IOException;
import java.io.InputStream;

public class DummyInputStream extends InputStream {

    static final boolean SUPPORT_STATS = true;

    InputStream in;
    long count = 0;

    public DummyInputStream(InputStream in) {
	this.in = in;
    }

    public int read() throws IOException {
	//		System.err.println("dummy.read");
	if (SUPPORT_STATS) {
	    count++;
	}
	return in.read();
    }

    public int read(byte[] b) throws IOException {
	int res = in.read(b);
	//		System.err.println("dummy.read array of len " + b.length + " result was " + res + " bytes");
	if (SUPPORT_STATS) {
	    if (res >= 0) count += res;
	}
	return res;
    }

    public int read(byte[] b, int off, int len) throws IOException {
	int res = in.read(b, off, len);
	//		System.err.println("dummy.read array of len " + len + " result was " + res + " bytes");
	if (SUPPORT_STATS) {
	    if (res >= 0) count += res;
	}
	return res;
    }

    public long skip(long n) throws IOException {
	return in.skip(n);
    }

    public int available() throws IOException {
	return in.available();
    }

    public void close() {
	/* ignore */
    }

    public void realClose() throws IOException {
	in.close();
    }

    public void mark(int readlimit)  {
	in.mark(readlimit);
    }

    public void reset() throws IOException {
	in.reset();
    }

    public boolean markSupported() {
	return in.markSupported();
    }

    public void resetCount() {
	count = 0;
    }

    public long getCount() {
	return count;
    }
}
