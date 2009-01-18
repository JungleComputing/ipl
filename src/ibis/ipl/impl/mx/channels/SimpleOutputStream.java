package ibis.ipl.impl.mx.channels;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import org.apache.log4j.Logger;

public class SimpleOutputStream extends OutputStream {

	private static final Logger logger = Logger.getLogger(SimpleOutputStream.class);
	
	private WriteChannel channel;

	protected SimpleOutputStream(WriteChannel channel) {
		super();
		this.channel = channel;
		if (channel == null || !channel.isOpen()) {
			closed = true;
			return;
		}
	}

	void swapBuffers() throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("swapBuffers()");
		}
		/*
		 * frontBuffer.flip(); channel.flush(); channel.write(frontBuffer);
		 * frontBuffer.clear();
		 */
		// nu dit:
		if(!channel.isOpen()) {
			close();
			throw new ClosedChannelException();
		}
		frontBuffer.flip();
		channel.flush();
		channel.post(frontBuffer);
		ByteBuffer temp = frontBuffer;
		frontBuffer = backBuffer;
		backBuffer = temp;
		frontBuffer.clear();
	}
	
	void doFlush() throws IOException {
		// System.out.println("Flushing " + buffer.remaining() + " bytes");
		if(!channel.isOpen()) {
			close();
			throw new ClosedChannelException();
		}
		frontBuffer.flip();
		
		channel.post(frontBuffer);
		channel.flush();
		frontBuffer.clear();
		backBuffer.clear();
	}
	
	void doClose() throws IOException {
		channel.close();
	}
}
