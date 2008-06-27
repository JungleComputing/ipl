package ibis.ipl.impl.mx;

import ibis.ipl.ConnectionClosedException;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

public class MxReadChannel {
	private static Logger logger = Logger.getLogger(MxReadChannel.class);
	
	protected int handle;
	protected int endpointId;
	protected boolean closed, senderClosed, receiverClosed, receiving;
	protected MxChannelFactory factory;
	protected long matchData = Matching.NONE;
	
	/**
	 * @param factory
	 */
	MxReadChannel(MxChannelFactory factory) {
		this.factory = factory;
		handle = JavaMx.handles.getHandle();
		this.endpointId = factory.endpointId;
		closed = receiving = senderClosed = receiverClosed = false;
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
			while(receiving) {
				if (logger.isDebugEnabled()) {
					logger.debug("close() while...");
				}
				JavaMx.cancel(factory.endpointId, handle); //really stop current reception, or wait for it?
				/*if (logger.isDebugEnabled()) {
					logger.debug("close() is waiting...");
				}*/
				notifyAll();
				try {
					if (logger.isDebugEnabled()) {
						logger.debug("close() is waiting...");
					}
					wait();
				} catch (InterruptedException e) {
					// Ignore
				}
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
	
	protected synchronized void receiverClose() {
		receiverClosed = true;
	}
	
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

			while(receiving) {
				if (logger.isDebugEnabled()) {
					logger.debug("senderClose() while...");
				}
				JavaMx.cancel(factory.endpointId, handle); //really stop current reception, or wait for it?
				/*if (logger.isDebugEnabled()) {
					logger.debug("close() is waiting...");
				}*/
				notifyAll();
				try {
					if (logger.isDebugEnabled()) {
						logger.debug("senderClose() is waiting...");
					}
					wait();
				} catch (InterruptedException e) {
					// Ignore
				}
			}
			// nobody is receiving anymore
			if(closed) {
				//somebody else already detected that the channel is empty, we are done
				if (logger.isDebugEnabled()) {
					logger.debug("leaving close() 2...");
				}
				notifyAll();
				return true;
			}
			if(doPoll() == -1) {  //FIXME doPoll() broken?
			// 	no message in channel
				close();
				if (logger.isDebugEnabled()) {
					logger.debug("leaving close() 4...");
				}
				notifyAll();
				return true;
			}
			notifyAll();
		}
		if (logger.isDebugEnabled()) {
			logger.debug("leaving senderClose() 1...");
		}
		if(receiverClosed) {
			realClose();
			return true;
		}
		return false;		
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
				throw new ConnectionClosedException();
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
						return 0;
					}
					if(senderClosed) {
						// sender closed
						if (doPoll() == -1) {
							// no message in channel
							realClose();
							receiving = false;
							notifyAll();
							return 0;
						} // else: go pick the message 
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
			}

			// set the position to after the first empty position of the buffer (standard NIO Channel behaviour) 
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

	public synchronized int poll() {
		if(closed) {
			/*if (logger.isDebugEnabled()) {
				logger.debug("polling on closed channel");
			}*/
			
			//TODO Exception
			return 0;
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
	
	public synchronized int poll(long timeout) {
		if(closed) {
			/*if (logger.isDebugEnabled()) {
				logger.debug("polling on closed channel");
			}*/
			return 0;
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
