package ibis.ipl.impl.mx;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import org.apache.log4j.Logger;

public abstract class MxWriteChannel {

	private static Logger logger = Logger.getLogger(MxWriteChannel.class);
	
	protected MxChannelFactory factory;
	protected int link;
	protected int handle;
	protected boolean sending = false;
	protected boolean closed = false;
	protected MxAddress target;
	protected int msgSize;
	protected long matchData = Matching.NONE;

	protected MxWriteChannel(MxChannelFactory factory, MxAddress target, int filter) throws IOException {
		this.factory = factory;
		this.link = JavaMx.links.getLink();
		//TODO timeouts?
		if(JavaMx.connect(factory.endpointId, link, target.nicId, target.endpointId, filter) == false) {
			throw new IOException("Could not connect to target");
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Connected to " + target.toString());
		}
		this.handle = JavaMx.handles.getHandle();

	}

	void close() {
		synchronized(this) {
			// send a CLOSE signal to the reader
			closed = true;
			long closeMatchData = Matching.setProtocol(matchData, Matching.PROTOCOL_DISCONNECT);
			int closeHandle = JavaMx.handles.getHandle();
			//FIXME buffer allocation
			ByteBuffer bb = ByteBuffer.allocateDirect(0);
			JavaMx.send(bb, 0, 0, factory.endpointId, link, closeHandle, closeMatchData);
			try {
				int msgSize = JavaMx.wait(factory.endpointId, closeHandle);
			} catch (MxException e1) {
				//stop trying to receive the message
				logger.warn("Error sending the close signal.");
			}
			JavaMx.handles.releaseHandle(closeHandle);
			
			
			// wait for the pending messages to finish
			if(sending) {
				try {
					if(poll() == false) {
						JavaMx.cancel(factory.endpointId, handle); //really stop current send operation, or wait for it?
						/*try {
							wait();
						} catch (InterruptedException e) {
							// ignore
						}*/	
					}
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if(closed) {
				return;
			}
			closed = true;
		}
		/* TODO remove this?
		while(sending) {
			JavaMx.cancel(factory.endpointId, handle); //really stop current send operation, or wait for it?
		}
		*/

		JavaMx.handles.releaseHandle(handle);
		JavaMx.links.releaseLink(link);
	}

	
	/**
	 * @param buffer The data buffer that is sent. The data between the position and the limit will be sent. Upon completion the position will be equal to the limit.
	 * @throws IOException An exception when sending went wrong. When an exception occurs, the message is not sent and the buffer is unharmed.
	 */
	public synchronized void write(ByteBuffer buffer) throws IOException {
		if(closed) {
			throw new ClosedChannelException();
		}
		if(sending) {
			finish();
			//sending will be false now
		}	
		sending = true;
		doSend(buffer);
		msgSize = buffer.remaining();
		buffer.position(buffer.limit());
	}
	
	public synchronized void write(ByteBuffer[] buffers) throws IOException {
		if(closed) {
			throw new ClosedChannelException();
		}
		if(sending) {
			finish();
			//sending will be false now
		}
		sending = true;
		doSend(buffers);
		msgSize = 0;
		for(ByteBuffer b: buffers) {
			msgSize += b.remaining();
			b.position(b.limit());
		}
	}
	
	/**
	 * @param buffers
	 */
	protected abstract void doSend(ByteBuffer[] buffers);

	protected abstract void doSend(ByteBuffer buffer);

	public synchronized void finish() throws IOException {
		if(!sending) {
			// well, we are finished in that case!
			return;
		}

		int msgSize;
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("finishing message...");
			}
			msgSize = JavaMx.wait(factory.endpointId, handle);
			if (logger.isDebugEnabled()) {
				logger.debug("message of " + msgSize + " bytes sent!");
			}
		} catch (MxException e) {
			// TODO Maybe handle this some of them in the future
			throw(e); 
		}
		if(msgSize == -1) {
			throw new MxException("error waiting for the message completion");
		}
		sending = false;
		notifyAll();
		if(msgSize != this.msgSize) {
			// message truncated
			throw new MxException("Message truncated from "+ this.msgSize + " to " + msgSize + "bytes");
		}
	}

	public synchronized boolean poll() throws IOException {
		if(!sending) {
			// well, we are finished in that case!
			return true;
		}

		int msgSize;
		try {
			msgSize = JavaMx.test(factory.endpointId, handle);
		} catch (MxException e) {
			// TODO Maybe handle this some of them in the future
			throw(e); 
		}
		if(msgSize == -1) {
			// request still pending
			return false;
		}
		sending = false;
		notifyAll();
		return true;
	}

	public synchronized boolean isSending() {
		//TODO unused method?
		return sending;
	}
	
	@Override
	protected void finalize() throws Throwable {
		if(!closed) {
			close();
		}
		super.finalize();
	}
}