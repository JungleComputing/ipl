package ibis.ipl.impl.mx;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import org.apache.log4j.Logger;

public class MxReadChannel implements ReadChannel {
	private static Logger logger = Logger.getLogger(MxReadChannel.class);
	
	protected int handle;
	protected int endpointId;
	protected boolean closed, senderClosed, receiving;
	protected MxChannelFactory factory;
	protected long matchData = Matching.NONE;
	
	/**
	 * @param factory
	 */
	MxReadChannel(MxChannelFactory factory) {
		this.factory = factory;
		handle = JavaMx.handles.getHandle();
		this.endpointId = factory.endpointId;
		closed = receiving = senderClosed = false;
	}

	/* (non-Javadoc)
	 * @see ibis.ipl.impl.mx.ReadChannel#close()
	 */
	public void close() {
		//logger.debug("closing");
		synchronized(this) {
			if(closed) {
				return;
			}
			closed = true;
			if(!senderClosed) {
				// inform the senders about closing this channel
				if(receiving) {
					// FIXME can result in a SIGSEGV, in libmyriexpress
					JavaMx.cancel(factory.endpointId, handle); //really stop current reception, or wait for it?
					//JavaMx.wakeup(endpointId);
					/*if (logger.isDebugEnabled()) {
						logger.debug("close() is waiting...");
					}*/
				}
				factory.sendCloseMessage(this);
			}
			notifyAll(); //TODO do we need this here? In any case, it is not wrong...
		}
	}
		
	
	private synchronized void realClose() {
		closed = true;
		JavaMx.handles.releaseHandle(handle);
		//logger.debug("closed!");

	}
		
	/**
	 * @return true when the connection is closed, false when the connection is not closed completely and there may be still is some data left in the channel
	 */
	protected synchronized boolean senderClose() {
		//logger.debug("Sender closing");
		if (closed) {
			return true;
		}
		if(senderClosed) {
			//sender already closed, but receiver is not
			return false;
		}
		if(receiving) {
			// someone is receiving, let him detect whether the channel is already closed completetely 
			JavaMx.cancel(factory.endpointId, handle); //really stop current reception, or wait for it?
			notifyAll(); //TODO do we need this here? In any case, it is not wrong...
			return false;	
		} else { 
			// nobody is waiting for a message, check whether there is no message on its way yourself
			if(doPoll() == -1) {
				// no message in channel, so we can close it
				realClose();
				notifyAll();
				return true;
			} else {
				notifyAll();
				return false;
			}
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		// release native resources when the channel is not closed completely
		if(!closed) {
			logger.debug("Collecting native garbage (Channel not closed correctly).");
			closed = true;
			realClose();
		}
		super.finalize();
	}

	
	/* (non-Javadoc)
	 * @see ibis.ipl.impl.mx.ReadChannel#read(java.nio.ByteBuffer, long)
	 */
	public int read(ByteBuffer buffer, long timeout) throws IOException {
		int msgSize = -1;
		
		synchronized(this) {
			while(receiving == true) {
				try {
					wait();
				} catch (InterruptedException e) {
					// ignore
				}
			}
			if(closed) {
				throw new ClosedChannelException();
			}
			receiving = true;
		}
		try {
			/*if (logger.isDebugEnabled()) {
				logger.debug("Receiving...");
			}*/
			while(msgSize <= 0) {
				// ignore messages of 0 bytes
				synchronized(this) {
					if(closed) {
						//logger.debug("read() notices that the connection got closed, aborting");
						receiving = false;
						notifyAll();
						return -1;
					}
					if(senderClosed) {
						// sender closed
						if (doPoll() == -1) {
							// no message in channel
							receiving = false;
							realClose();
							notifyAll();
							return -1;
						} // else: pick up the message 
					}
					JavaMx.recv(buffer, buffer.position(), buffer.remaining(), endpointId, handle, matchData);
				}
				if(timeout <= 0) {
					//FIXME causes an SIGSEGV when closing a the channel in some circumstances: needs investigation
					msgSize = JavaMx.wait(endpointId, handle);
				} else {
					msgSize = JavaMx.wait(endpointId, handle, timeout);
				}
			}

			// set the position to after the first empty position of the buffer (standard NIO Channel behavior)
			if(msgSize > 0) {
				buffer.position(buffer.position() + msgSize);
			}
			//logger.debug("message of " + msgSize + " bytes arrived.");
		} catch (MxException e) {
			synchronized(this) {
				receiving = false;
				notifyAll();
			}
			throw(e);
		}
		
		synchronized(this) {
			receiving = false;
			notifyAll();
		}
		return msgSize;
	}

	/* (non-Javadoc)
	 * @see ibis.ipl.impl.mx.ReadChannel#isOpen()
	 */
	public boolean isOpen() {
		return !closed;
	}

	/* (non-Javadoc)
	 * @see ibis.ipl.impl.mx.ReadChannel#poll()
	 */
	public synchronized int poll() throws ClosedChannelException {
		if(closed) {
			throw new ClosedChannelException();
		}
		if(receiving) {		
			return 0;
		}
		if(senderClosed) {
			int result = doPoll();
			if (result == -1) {
				// channel is empty, so close it
				realClose();
			}
			return result;
		}
		return doPoll();
	}
	
	/* (non-Javadoc)
	 * @see ibis.ipl.impl.mx.ReadChannel#poll(long)
	 */
	public synchronized int poll(long timeout) throws ClosedChannelException {
		if(closed) {
			throw new ClosedChannelException();
		}
		if(receiving) {
			return 0;
		}
		if(senderClosed) {
			int result = doPoll();
			if (result == -1) {
				// channel is empty, so close it
				realClose();
			}
			return result;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("polling");
		}
		return JavaMx.probe(endpointId, timeout, matchData, Matching.MASK_ALL); //-1 when no message available
	}
	
	private int doPoll() {
		return JavaMx.iprobe(endpointId, matchData, Matching.MASK_ALL); //-1 when no message available
	}
}
