package ibis.ipl.impl.mx;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import org.apache.log4j.Logger;

public class MxLocalChannel implements ReadChannel, WriteChannel {

	private static Logger logger = Logger.getLogger(MxLocalChannel.class);

	private boolean closed = false;
	
	//The channel capacity
	private ByteBuffer buffer;
	
	MxLocalChannel() {
		logger.debug("Local channel created");
		buffer = ByteBuffer.allocate(Config.BYTE_BUFFER_SIZE); //We don't need a direct buffer here
	}
	
	/* Common: */
	
	public synchronized void close() {
		logger.debug("Local channel closed");
		closed = true;
	}

	/* ReadChannel methods: */
	
	public synchronized boolean isOpen() {
		return !closed;
	}

	public synchronized int poll() throws ClosedChannelException {
		int result = buffer.position();
		if(closed && result == 0) {
			return -1;
			//TODO EXCEPTION?
		}
		return result = buffer.position();
	}

	public synchronized int poll(long timeout) throws ClosedChannelException {
		long deadline = System.currentTimeMillis() + timeout;
		long time;
		int result = buffer.position();
		while (result == 0) {
			if(closed) {
				return -1;
				//TODO Exception?
			}
			time = System.currentTimeMillis();
			if(timeout != 0 && time > deadline) {
				return 0; //TODO or 0?
			}
			try {
				wait(deadline - time);
			} catch (InterruptedException e) {
				// ignore
			}
			result = buffer.position();
		}
		return result;
	}

	public synchronized int read(ByteBuffer dest, long timeout) throws IOException {
		logger.debug("read");
		int size = poll(timeout);
		if (size > 0) {
			//TODO check for buffer sizes?
			buffer.flip();
			dest.put(buffer);
			
			// empty buffer and notify blocked write methods
			buffer.clear();
			notifyAll();
			return size;
		}
		return size;
	}

	/* WriteChannel methods */
	
	public  void finish() throws IOException {
		//empty implementation
	}

	public boolean isFinished() throws IOException {
		//empty implementation
		return true;
	}

	public synchronized void write(ByteBuffer src) throws IOException {
		logger.debug("write");
		while(buffer.remaining() < src.remaining()) {
			try {
				wait();
			} catch (InterruptedException e) {
				// ignore
			}
		}
		buffer.put(src);
	}

	public synchronized void write(SendBuffer src) throws IOException {
		logger.debug("write SendBuffer");
		while(buffer.remaining() < src.remaining()) {
			try {
				wait();
			} catch (InterruptedException e) {
				// ignore
			}
		}
		for(ByteBuffer buf : src.byteBuffers) {
			buffer.put(buf);
		}
	}



}