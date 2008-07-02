package ibis.ipl.impl.mx;

import ibis.ipl.ConnectionClosedException;
import ibis.ipl.impl.SendPortIdentifier;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import org.apache.log4j.Logger;

public class MxReadChannel {
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

	public void close() {
		if (logger.isDebugEnabled()) {
			logger.debug("closing");
		}
		synchronized(this) {
			if(closed) {
				return;
			}
			closed = true;
			if(!senderClosed) {
				// inform the senders about closing this channel
				//FIXME doing this here breaks the channelfactory, because it throws a nullpointerexception there
				factory.sendCloseMessage(this);
			}
			if(receiving) {
				if (logger.isDebugEnabled()) {
					logger.debug("close() while...");
				}
				JavaMx.cancel(factory.endpointId, handle); //really stop current reception, or wait for it?
				/*if (logger.isDebugEnabled()) {
					logger.debug("close() is waiting...");
				}*/
				notifyAll(); //TODO do we need this here? In any case, it is not wrong...
			}
			realClose();
		}
	}
		
	
	private synchronized void realClose() {
		closed = true;
		JavaMx.handles.releaseHandle(handle);
		if (logger.isDebugEnabled()) {
			logger.debug("closed!");
		}
	}
		
	/**
	 * @return true when the connection is closed, false when the connection is not closed completely and there may be still is some data left in the channel
	 */
	protected boolean senderClose() {
		if (logger.isDebugEnabled()) {
			logger.debug("Sender closing");
		}
		synchronized(this) {			
			if (closed) {
				return true;
			}
			if(senderClosed) {
				//sender already closed, but receiver is not
				return false;
			}

			if(receiving) {
				// someone is receiving, let him detect whether the channel is already closed completetely 
				if (logger.isDebugEnabled()) {
					logger.debug("senderClose(): channel is wating for a message...");
				}
				JavaMx.cancel(factory.endpointId, handle); //really stop current reception, or wait for it?
				/*if (logger.isDebugEnabled()) {
					logger.debug("close() is waiting...");
				}*/
				notifyAll(); //TODO do we need this here? In any case, it is not wrong...
				if (logger.isDebugEnabled()) {
					logger.debug("leaving senderClose() 1...");
				}
				return false;
				
			} else { 
				// nobody is waiting for a message, check whether there is no message on its way yourself
				if(doPoll() == -1) {
					// no message in channel, so we can close it
					realClose();
					if (logger.isDebugEnabled()) {
						logger.debug("leaving senderClose() 4...");
					}
					notifyAll();
					return true;
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("leaving senderClose() 2...");
					}
					notifyAll();
					return false;
				}
			}
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		// release native resources when the channel is not closed completely
		if(!closed) {
			if (logger.isDebugEnabled()) {
				logger.debug("Collecting native garbage (Channel not closed correctly).");
			}
			closed = true;
			if(receiving) {
				JavaMx.cancel(factory.endpointId, handle);
			}
			realClose();
		}
		super.finalize();
	}

	
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
						if (logger.isDebugEnabled()) {
							logger.debug("read() notices that the connection got closed, aborting");
						}
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
					/*if (logger.isDebugEnabled()) {
						logger.debug("recv() posted");
					}*/ 
				}
				if(timeout <= 0) {
					msgSize = JavaMx.wait(endpointId, handle);
				} else {
					msgSize = JavaMx.wait(endpointId, handle, timeout);
				}
				//FIXME timeouts, message return codes
			}

			// set the position to after the first empty position of the buffer (standard NIO Channel behavior) 
			buffer.position(buffer.position() + msgSize);

			
			/*if (logger.isDebugEnabled()) {
				logger.debug("message of " + msgSize + " bytes arrived.");
			}*/
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
		
		if(msgSize == -1) { //error
			throw new MxException("Message not arrived");
		}
		return msgSize;
	}

	public boolean isOpen() {
		return !closed;
	}

	public synchronized int poll() throws ClosedChannelException {
		if(closed) {
			/*if (logger.isDebugEnabled()) {
				logger.debug("polling on closed channel");
			}*/
			throw new ClosedChannelException();
		}
		if(receiving) {
			/*if (logger.isDebugEnabled()) {
				logger.debug("polling on receiving channel");
			}*/
			
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
	
	public synchronized int poll(long timeout) throws ClosedChannelException {
		if(closed) {
			/*if (logger.isDebugEnabled()) {
				logger.debug("polling on closed channel");
			}*/
			throw new ClosedChannelException();
		}
		if(receiving) {
			/*if (logger.isDebugEnabled()) {
				logger.debug("polling on receiving channel");
			}*/
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
