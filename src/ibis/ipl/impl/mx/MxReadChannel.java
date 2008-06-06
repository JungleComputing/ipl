package ibis.ipl.impl.mx;

import ibis.ipl.ConnectionClosedException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import org.apache.log4j.Logger;

public class MxReadChannel extends Matching {
	private static Logger logger = Logger.getLogger(MxReadChannel.class);
	
	protected int handle;
	protected int endpointId;
	protected boolean closed, receiving;
	protected MxChannelFactory factory;
	
	/**
	 * @param factory
	 */
	MxReadChannel(MxChannelFactory factory) {
		this.factory = factory;
		handle = JavaMx.handles.getHandle();
		this.endpointId = factory.endpointId;
		closed = receiving = false;
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
				JavaMx.cancel(factory.endpointId, handle); //really stop current reception, or wait for it?
				try {
					wait();
				} catch (InterruptedException e) {
					// Ignore
				}
				if (logger.isDebugEnabled()) {
					logger.debug("closing");
				}
			}
			notifyAll();
		}
		JavaMx.handles.releaseHandle(handle);
	}
	
	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}

	
	public int read(ByteBuffer buffer) throws IOException {
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
			if (logger.isDebugEnabled()) {
				logger.debug("Receiving...");
			}
			while(msgSize <= 0) {
				// ignore messages of 0 bytes
				synchronized(this) {
					if(!closed) {
						JavaMx.recv(buffer, buffer.position(), buffer.remaining(), endpointId, handle, matchData);
						if (logger.isDebugEnabled()) {
							logger.debug("recv() posted");
						}
					} else {
						// connection closed
						if (logger.isDebugEnabled()) {
							logger.debug("read() notices that the connection got closed, aborting");
						}
						receiving = false;
						throw new ConnectionClosedException();
					}
				}
				// Don't want to do this synchronized (blocking call) 
				msgSize = JavaMx.wait(endpointId, handle);
			}

			// set the position to after the first empty position of the buffer (standard NIO Channel behaviour) 
			buffer.position(buffer.position() + msgSize);

			
			if (logger.isDebugEnabled()) {
				logger.debug("message of " + msgSize + " bytes arrived.");
			}
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
			return 0;
		}
		if(receiving) {
			return 0;
		}
		return JavaMx.iprobe(endpointId, matchData, MASK_NONE); //-1 when no message available
	}
}
