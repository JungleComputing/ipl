package ibis.ipl.impl.mx;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import org.apache.log4j.Logger;

public abstract class MxWriteChannel implements WriteChannel, Config {

	private static Logger logger = Logger.getLogger(MxWriteChannel.class);
		
	private SendBuffer[] buffers = new SendBuffer[FLUSH_QUEUE_SIZE];
	private int[] handles = new int[FLUSH_QUEUE_SIZE];
	private int first, queuedBuffers;
	
	protected MxChannelFactory factory;
	protected int link;
	protected boolean closed = false;
	protected MxAddress target;
	protected long matchData = Matching.NONE;
	private int endpointId;

	protected MxWriteChannel(MxChannelFactory factory, MxAddress target, int filter) throws IOException {
		this.factory = factory;
		this.link = JavaMx.links.getLink();
		endpointId = factory.endpointId;
		if(JavaMx.connect(endpointId, link, target.nicId, target.endpointId, filter) == false) {
			throw new IOException("Could not connect to target");
		}	
		for (int i = 0; i < FLUSH_QUEUE_SIZE; i++) {
			handles[i] = JavaMx.handles.getHandle();
		}
		first = queuedBuffers = 0;
	}

	public synchronized void close() {
		if(closed) {
			return;
		}
		factory.sendDisconnectMessage(this);
		try {
			flush();
		} catch (IOException e) {
			// TODO ignore, just close when flushing goes wrong?
		}
		closed = true;
		JavaMx.disconnect(link);
		JavaMx.links.releaseLink(link);
		for (int i = 0; i < FLUSH_QUEUE_SIZE; i++) {
			JavaMx.handles.releaseHandle(handles[i]);
		}
	}

	protected synchronized void receiverClosed() {
		if(closed) {
			return;
		}
		closed = true;
		if(queuedBuffers > 0) {
			// FIXME some buffers are in transit, or not flushed, yet, clean the mess
			while(queuedBuffers > 0) {
				JavaMx.cancel(endpointId, handles[first]);
				first = (first+1)%FLUSH_QUEUE_SIZE;
				queuedBuffers--;
			}
		}
		JavaMx.disconnect(link);
		JavaMx.links.releaseLink(link);
		for (int i = 0; i < FLUSH_QUEUE_SIZE; i++) {
			JavaMx.handles.releaseHandle(handles[i]);
		}
	}
	
	/* (non-Javadoc)
	 * @see ibis.ipl.impl.mx.WriteChannel#write(java.nio.ByteBuffer)
	 */
	public void write(ByteBuffer buffer) throws IOException {
		//TODO function is not in use?
		SendBuffer sb = SendBuffer.get();
		buffer.mark();
		try {
			sb.bytes.put(buffer);
		} catch (BufferOverflowException e) {
			throw new IOException("ByteBuffer too large to send");
		}
		write(sb);
		//TODO recycle sb in some way?
		buffer.reset();
	}

	/* (non-Javadoc)
	 * @see ibis.ipl.impl.mx.WriteChannel#write(java.nio.ByteBuffer[])
	 */
	public synchronized void write(SendBuffer buffer) throws IOException {
		if(closed) {
			throw new ClosedChannelException();
		}
		if (queuedBuffers == FLUSH_QUEUE_SIZE) {
			// crap, the queue is full! Wait for the head to finish()
			logger.debug("queue full while sending, waiting for the head message to be transfered succesfully");
			flushBuffer();
		} 
		int queuePos = (first+queuedBuffers)%FLUSH_QUEUE_SIZE;
		buffers[queuePos] = buffer;
		logger.debug("sending buffer with handle no: " + queuePos);
		doSend(buffer, handles[queuePos]);
		queuedBuffers++;

		logger.debug("message added to queue");
	}
	
	/**
	 * @param buffer
	 */
	protected abstract void doSend(SendBuffer buffer, int handle);

	/**
	 * flushed a the head buffer from the buffer queue
	 * @throws IOException
	 */
	private void flushBuffer() throws IOException {
		int msgSize = -1;
		while (msgSize < 0) {
			synchronized(this) {
				if(queuedBuffers == 0) {
					throw new IOException("no buffers in transit");
				}
				if(closed) {
					throw new ClosedChannelException();
				}				
			}
			try {
				logger.debug("waiting for handle " + first);
				msgSize = JavaMx.wait(endpointId, handles[first]);
			} catch (MxException e) {
				// 	TODO Maybe handle this some of them in the future
				throw(e); 
			}
		}
		if(msgSize != buffers[first].remaining()) {
			// message truncated
			throw new MxException("Message truncated from "+ buffers[first].remaining() + " to " + msgSize + "bytes");
		}
		first = (first+1)%FLUSH_QUEUE_SIZE;
		queuedBuffers--;	
	}
	
	/* (non-Javadoc)
	 * @see ibis.ipl.impl.mx.WriteChannel#finish()
	 */
	public synchronized void flush() throws IOException {
		if(closed) {
			throw new ClosedChannelException();
		}
		while(queuedBuffers > 0) {			
			flushBuffer();
		}
	}

	/* (non-Javadoc)
	 * @see ibis.ipl.impl.mx.WriteChannel#isFinished()
	 */
	public synchronized boolean isFinished() throws IOException {
		if(closed) {
			throw new ClosedChannelException();
		}
		return queuedBuffers == 0;
		//TODO when the queue is not empty, test for buffer completion for all buffers in the queue
	}

	protected synchronized boolean isSending() {
		return queuedBuffers != 0;
		//TODO unused method?
	}
	
	@Override
	protected void finalize() throws Throwable {
		logger.debug("finalizer called");
		if(!closed) {
			// TODO close(); maybe?
			JavaMx.disconnect(link);
			JavaMx.links.releaseLink(link);
			for (int i = 0; i < FLUSH_QUEUE_SIZE; i++) {
				JavaMx.handles.releaseHandle(handles[i]);
			}
		}
		super.finalize();
	}
}