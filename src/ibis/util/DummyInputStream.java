package ibis.util;

import java.io.IOException;
import java.io.InputStream;

public class DummyInputStream extends InputStream {

	InputStream in;

	public DummyInputStream(InputStream in) {
		this.in = in;
	}

	public int read() throws IOException {
//		System.err.println("dummy.read");
		return in.read();
	}

	public int read(byte[] b) throws IOException {
		int res = in.read(b);
//		System.err.println("dummy.read array of len " + b.length + " result was " + res + " bytes");
		return res;
	}

	public int read(byte[] b, int off, int len) throws IOException {
		int res = in.read(b, off, len);
//		System.err.println("dummy.read array of len " + len + " result was " + res + " bytes");
		return res;
	}

	public long skip(long n) throws IOException {
		return in.skip(n);
	}

	public int available() throws IOException {
		return in.available();
	}

	public void close() throws IOException {
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
}
