package ibis.ipl.impl.mx.channels;

import ibis.io.BufferedArrayInputStream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import org.apache.log4j.Logger;

public class InputStream extends java.io.InputStream {
	
	private static final Logger logger = Logger.getLogger(InputStream.class);

	private ByteBuffer frontBuffer;
	private ByteBuffer backBuffer;
	private ReadChannel channel;
	private boolean closed = false;

	protected InputStream(ReadChannel channel) throws IOException {
		this.channel = channel;
		if (channel == null || !channel.isOpen()) {
			closed = true;
			return;
		}
		frontBuffer = ByteBuffer.allocateDirect(Config.BUFSIZE);
		frontBuffer.flip();
		backBuffer = ByteBuffer.allocateDirect(Config.BUFSIZE);
		backBuffer.clear();
		channel.post(backBuffer);
	}

	@Override
	public int available() throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("available()");
		}
		return frontBuffer.remaining() + channel.available();
	}

	@Override
	public synchronized void close() throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("close()");
		}
		if (closed) {
			return;
		}
		closed = true;
		channel.close();
	}

	@Override
	public synchronized void mark(int arg0) {
		if (logger.isDebugEnabled()) {
			logger.debug("mark()");
		}
		// empty implementation
	}

	@Override
	public boolean markSupported() {
		if (logger.isDebugEnabled()) {
			logger.debug("markSupported()");
		}
		return false;
	}

	@Override
	public int read() throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("read()");
		}
		synchronized (this) {
			if (closed) {
				throw new IOException("Stream is closed");
			}
		}
		while (!frontBuffer.hasRemaining()) {
			try {
				swapBuffers();
			} catch (IOException e) {
				if (closed) {
					return -1;
				}
			}
		}
		return frontBuffer.get();
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("read(byte[], " + off + ", " + len + ")");
		}
		synchronized (this) {
			if (closed) {
				return -1;
			}
		}
		int length = len;
		int offset = off;
		while(!frontBuffer.hasRemaining()) {
			try {
				swapBuffers();
			} catch (IOException e) {
				if (closed) {
					return -1;
				}
			}
		}
		
		int remaining = frontBuffer.remaining();		
		if (length > remaining) {
			frontBuffer.get(b, offset, remaining);
			return remaining;
		} else {
			frontBuffer.get(b, offset, length);
			return length;
		}
	}

	@Override
	public int read(byte[] b) throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("read(byte[])");
		}
		return read(b, 0, b.length);
	}

	@Override
	public synchronized void reset() throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("reset()");
		}
		throw new IOException("reset() not supported");
	}

	@Override
	public long skip(long len) throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("skip()");
		}
		if (len <= 0) {
			return 0;
		}
		synchronized (this) {
			if (closed) {
				return 0;
			}
		}
		long remaining = len;
		while (remaining > frontBuffer.remaining()) {
			remaining -= frontBuffer.remaining();
			frontBuffer.position(frontBuffer.limit());
			try {
				swapBuffers();
			} catch (IOException e) {
				if (closed) {
					return len - remaining;
				}
			}
		}
		frontBuffer.position(frontBuffer.position() + (int) remaining);
		return len;
	}

	private void fillBuffer() throws IOException {
		// System.out.println("Before fillBuffer: " + frontBuffer.remaining() + "
		// bytes in frontBuffer");
		frontBuffer.compact();
		try {
			channel.read(frontBuffer);
		} catch (ClosedChannelException e) {
			close();
			throw (e);
		} catch (IOException e) {
			if (!channel.isOpen()) {
				close();
				throw new ClosedChannelException();
			}
		}
		frontBuffer.flip();
		// System.out.println("After fillBuffer: " + frontBuffer.remaining() + "
		// bytes in frontBuffer");
	}

	private void swapBuffers() throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("swapBuffers()");
		}
		/*
		frontBuffer.clear();  
		channel.read(frontBuffer);
		frontBuffer.flip();
		*/
		
		// nu dit:		
		frontBuffer.clear();
		channel.finish();
		channel.post(frontBuffer);
		ByteBuffer temp = frontBuffer;
		frontBuffer = backBuffer;
		backBuffer = temp;
		frontBuffer.flip();
		if (logger.isDebugEnabled()) {
			logger.debug("swapBuffers: now " + frontBuffer.remaining() + " bytes in frontBuffer.");
		}
	}
	
}
