package ibis.util;

import java.io.IOException;
import java.io.OutputStream;

public class DummyOutputStream extends OutputStream {

	static final boolean SUPPORT_STATS = true;

	OutputStream out;
	long count = 0;

	public DummyOutputStream(OutputStream out) {
		this.out = out;
	}

	public void write(int b) throws IOException {
		out.write(b);

		if(SUPPORT_STATS) {
			count++;
		}
	}

	public void write(byte[] b) throws IOException {
		out.write(b);

		if(SUPPORT_STATS) {
			count += b.length;
		}
	}

	public void write(byte[] b, int off, int len) throws IOException {
		out.write(b, off, len);

		if(SUPPORT_STATS) {
			count += len;
		}
	}

	public void flush() throws IOException {
		out.flush();
	}

	public void close() throws IOException {
		/* don't propagate the close, otherwise we close the underlying socket,
		   and that is not what we want here. */
	}

	public void realClose() throws IOException {
		out.close();
	}

	public void resetCount() {
		count = 0;
	}

	public long getCount() {
		return count;
	}
}
