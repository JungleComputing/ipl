package ibis.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * A inputstream which uses a Dissipator as the underlying implementation.
 */
public final class DissipatorInputStream extends InputStream {

	private IbisDissipator in;

	public DissipatorInputStream(IbisDissipator in) {
		super();
		this.in = in;
	}

	public int available() throws IOException {
		return in.available();
	}
	
	public int read() throws IOException {
		return (int)in.readByte();
	}

	public int read(byte[] b) throws IOException {
		in.readArray(b, 0, b.length);
		return b.length;
	}

	public int read(byte[] b, int off, int len) throws IOException {
		in.readArray(b, off, len);
		return b.length;
	}

	public void close() throws IOException {
		in.close();
	}


}
	
