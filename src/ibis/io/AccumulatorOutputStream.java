package ibis.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A outputstream which uses a Accumulator as a underlying implementation.
 */
public final class AccumulatorOutputStream extends OutputStream {

	private IbisAccumulator out;

	public AccumulatorOutputStream(IbisAccumulator out) {
		super();
		this.out = out;
	}

	public void close() throws IOException {
		out.close();
	}

	public void flush() throws IOException {
		out.flush();
	}

	public void write(int b) throws IOException {
		out.writeByte((byte)b);
	}

	public void write(byte[] b) throws IOException {
		out.writeArray(b, 0, b.length);
	}

	public void write(byte[] b, int off, int len) throws IOException {
		out.writeArray(b, off, len);
	}
}
	
