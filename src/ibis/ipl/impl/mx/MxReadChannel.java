package ibis.ipl.impl.mx;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import org.apache.log4j.Logger;

public class MxReadChannel implements ReadChannel {
	private static Logger logger = Logger.getLogger(MxReadChannel.class);
	
	protected int handle;
	protected int endpointId;
	protected boolean closed, realClosed, senderClosed, receiving;
	protected MxChannelFactory factory;
	protected long matchData = Matching.NONE;
	private MxAddress sender = null;
	
	/**
	 * @param factory
	 */
	MxReadChannel(MxChannelFactory factory) {
		this.factory = factory;
		handle = JavaMx.handles.getHandle();
		this.endpointId = factory.endpointId;
		closed = realClosed = receiving = senderClosed = false;
	}

	/* (non-Javadoc)
	 * @see ibis.ipl.impl.mx.ReadChannel#close()
	 */
	public synchronized void close() {
		logger.debug("close()");
		if(closed) {
			return;
		}
		closed = true;
		if(!senderClosed) {
			// inform the senders about closing this channel
			factory.sendCloseMessage(this);
		}
		while(receiving) {
			//JavaMx.wakeup(endpointId);
			/*if (logger.isDebugEnabled()) {
				logger.debug("close() is waiting...");
			}*/
			try {
				wait();
			} catch (InterruptedException e) {
				// ignore
			}
		}
			
		realClose();
		notifyAll(); //TODO do we need this here? In any case, it is not wrong...
	}
		
	
	private synchronized void realClose() {
		logger.debug("realClose()");
		if(realClosed) {
			return;
		}
		closed = realClosed = true;
		JavaMx.handles.releaseHandle(handle);
		//logger.debug("closed!");

	}
		
	/**
	 * @return true when the connection is closed, false when the connection is not closed completely and there may be still is some data left in the channel
	 */
	protected synchronized void senderClose() {
		logger.debug("senderClose()");
		if (closed) {
			return;
		}
		if(senderClosed) {
			//sender already closed, but receiver is not
			return;
		}
		senderClosed = true;
		while(receiving) {
			// someone is receiving, let him detect whether the channel is already closed completely 
			//FIXME this 'cancel' can cause a SIGSEGV
			//JavaMx.cancel(factory.endpointId, handle); //really stop current reception, or wait for it?
			try {
				wait();
			} catch (InterruptedException e) {
				// ignore
			}	
		} 
		// nobody is waiting for a message, check whether there is no message on its way yourself
		if(doPoll() == -1) {
			// no message in channel, so we can close it
			realClose();
			notifyAll();
			return;
		} else {
			notifyAll();
			return;
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		// release native resources when the channel is not closed completely
		if(!realClosed) {
			logger.debug("Collecting native garbage (Channel not closed correctly).");
			realClose();
		}
		super.finalize();
	}

	
	/* (non-Javadoc)
	 * @see ibis.ipl.impl.mx.ReadChannel#read(java.nio.ByteBuffer, long)
	 */
	public int read(ByteBuffer buffer) throws IOException {
		//logger.debug("read()");
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
				// TODO ignore messages of 0 bytes
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
				while(msgSize < 0) {
					synchronized(this) {
						if(senderClosed || closed) {
							if(JavaMx.cancel(factory.endpointId, handle) == true) {
								receiving = false;
								notifyAll();
								return -1;
							} else {
								// We were too late, so we still have to complete the receive request
							}
						}
					}
					msgSize = JavaMx.wait(endpointId, handle, 100);
				}
			}

			// set the position to after the first empty position of the buffer (standard NIO Channel behavior)
			if(msgSize > 0) {
				buffer.position(buffer.position() + msgSize);
			}
			logger.debug("message of " + msgSize + " bytes arrived.");
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
		logger.debug("isOpen()");
		return !closed;
	}

	/* (non-Javadoc)
	 * @see ibis.ipl.impl.mx.ReadChannel#poll()
	 */
	public synchronized int poll() throws ClosedChannelException {
		//logger.debug("poll()");
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
				throw new ClosedChannelException(); //FIXME just return an EOF?
			}
			return result;
		}
		return doPoll();
	}
	
	/* (non-Javadoc)
	 * @see ibis.ipl.impl.mx.ReadChannel#poll(long)
	 */
	public int poll(long timeout) throws ClosedChannelException {
		//logger.debug("poll(timeout)");
		synchronized(this) {
			//logger.debug("poll(timeout) got access");
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
					throw new ClosedChannelException(); //FIXME, no exception, but EOF?
				}
				return result;
			}
			receiving = true;
		}
		int result = -1;
		if(timeout == 0) {
			//logger.debug("poll(timeout): no-timeout loop");
			while (result < 0) {
				/* FIXME when timeout is infinite and the channel closes, we will be blocked forever here
				 * Fix this by 'polling' for a closed channel or calling mx_wakeup when closing (last one can have many side-effects!)
				 */
				// this is the first suggestion
				if(closed) {
					synchronized(this) {
						receiving = false;
						notifyAll();
					}
					return result;
				}
				;
				result = JavaMx.probe(endpointId, 100, matchData, Matching.MASK_ALL); //-1 when no message available
			}
			synchronized(this) {
				receiving = false;
				notifyAll();
			}
			return result;
		} else {
			result = JavaMx.probe(endpointId, timeout, matchData, Matching.MASK_ALL); //-1 when no message available
			synchronized(this) {
				receiving = false;
				notifyAll();
			}
			return result;
		}
	}
	
	private int doPoll() {
		//logger.debug("doPoll()");
		return JavaMx.iprobe(endpointId, matchData, Matching.MASK_ALL); //-1 when no message available
	}

	/**
	 * @return the sender
	 */
	protected MxAddress getSender() {
		return sender;
	}

	/**
	 * @param sender the sender to set
	 */
	protected void setSender(MxAddress sender) {
		this.sender = sender;
	}
}
