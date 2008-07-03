package ibis.ipl.impl.mx;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;

import org.apache.log4j.Logger;

public abstract class MxWriteChannel implements WriteChannel {

	private static Logger logger = Logger.getLogger(MxWriteChannel.class);
	
	protected MxChannelFactory factory;
	protected int link;
	protected int handle;
	protected boolean sending = false;
	protected boolean closed = false;
	protected MxAddress target;
	protected int msgSize;
	protected long matchData = Matching.NONE;

	//protected ByteOrder order = ByteOrder.BIG_ENDIAN;

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

	public void close() {
		synchronized(this) {
			if(closed) {
				return;
			}
			// send a CLOSE signal to the reader
			long closeMatchData = Matching.setProtocol(matchData, Matching.PROTOCOL_DISCONNECT);
			int closeHandle = JavaMx.handles.getHandle();
			//FIXME buffer allocation
			ByteBuffer bb = ByteBuffer.allocateDirect(0);
			JavaMx.send(bb, 0, 0, factory.endpointId, link, closeHandle, closeMatchData);
			try {
				int msgSize = JavaMx.wait(factory.endpointId, closeHandle, 1000);
			} catch (MxException e1) {
				//stop trying to receive the message
				logger.warn("Error sending the close signal.");
			}
			JavaMx.handles.releaseHandle(closeHandle);
			
			
			// wait for the pending messages to finish
			if(sending) {
				try {
					if(isFinished() == false) {
						JavaMx.cancel(factory.endpointId, handle); //TODO really stop current send operation, or wait for it?
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
			closed = true;
		}
		JavaMx.handles.releaseHandle(handle);
		JavaMx.disconnect(link);
		JavaMx.links.releaseLink(link);
	}

	
	/* (non-Javadoc)
	 * @see ibis.ipl.impl.mx.WriteChannel#write(java.nio.ByteBuffer)
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
		
		// Nope, we don't do this, can cause problems with offering buffers to several writechannels concurrently
		// Without this, we can do that safely
		// buffer.position(buffer.limit());
	}
	
	/* (non-Javadoc)
	 * @see ibis.ipl.impl.mx.WriteChannel#write(java.nio.ByteBuffer[])
	 */
	public synchronized void write(SendBuffer buffer) throws IOException {
		if(closed) {
			throw new ClosedChannelException();
		}
		if(sending) {
			finish();
			//sending will be false now
		}
		sending = true;
		doSend(buffer);
		msgSize = (int)(buffer.remaining()); // cast will go well as long as the messages are smaller than 2 GB
	}
	
	/**
	 * @param buffer
	 */
	protected abstract void doSend(SendBuffer buffer);

	protected abstract void doSend(ByteBuffer buffer);

	/* (non-Javadoc)
	 * @see ibis.ipl.impl.mx.WriteChannel#finish()
	 */
	public synchronized void finish() throws IOException {
		if(!sending) {
			// well, we are finished in that case!
			return;
		}

		int msgSize;
		try {
			/*if (logger.isDebugEnabled()) {
				logger.debug("finishing message...");
			}*/
			msgSize = JavaMx.wait(factory.endpointId, handle);
			/*if (logger.isDebugEnabled()) {
				logger.debug("message of " + msgSize + " bytes sent!");
			}*/
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

	/* (non-Javadoc)
	 * @see ibis.ipl.impl.mx.WriteChannel#poll()
	 */
	public synchronized boolean isFinished() throws IOException {
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