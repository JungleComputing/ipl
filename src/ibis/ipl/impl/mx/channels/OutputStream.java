package ibis.ipl.impl.mx.channels;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

public abstract class OutputStream extends java.io.OutputStream {

	private static final Logger logger = Logger.getLogger(OutputStream.class);
	
	ByteBuffer frontBuffer;
	ByteBuffer backBuffer;
	boolean closed = false;

	OutputStream() {
		backBuffer = ByteBuffer.allocateDirect(Config.STREAMBUFSIZE);
		frontBuffer = ByteBuffer.allocateDirect(Config.STREAMBUFSIZE);
	}

	@Override
	public void write(int b) throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("write(int)");
		}
		synchronized (this) {
			if (closed) {
				throw new IOException("Stream is closed");
			}
		}
		if (!frontBuffer.hasRemaining()) {
			swapBuffers();
		}
		frontBuffer.put((byte) b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("write(byte[], off, len)");
		}
		synchronized (this) {
			if (closed) {
				throw new IOException("Stream is closed");
			}
		}
		int remaining = frontBuffer.remaining();
		int offset = off;
		int length = len;
		while (length > remaining) {
			frontBuffer.put(b, offset, remaining);
			swapBuffers();
			offset += remaining;
			length -= remaining;
			remaining = frontBuffer.remaining();
		}
		frontBuffer.put(b, offset, length);
	}

	@Override
	public void write(byte[] b) throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("write(byte[])");
		}
		write(b, 0, b.length);
	}

	@Override
	public synchronized void close() {
		if (logger.isDebugEnabled()) {
			logger.debug("close()");
		}
		if (closed) {
			return;
		}
		closed = true;
		try {
			doClose();
		} catch (IOException e) {
			// ignore
		}
	}

	@Override
	public void flush() throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("flush()");
		}
		synchronized (this) {
			if (closed) {
				throw new IOException("Stream is closed");
			}
		}
		doFlush();
	}

	abstract void swapBuffers() throws IOException;
	
	abstract void doFlush() throws IOException;
	
	abstract void doClose() throws IOException;
}
