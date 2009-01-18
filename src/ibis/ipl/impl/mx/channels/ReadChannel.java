package ibis.ipl.impl.mx.channels;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import org.apache.log4j.Logger;

public class ReadChannel {

	private static Logger logger = Logger.getLogger(ReadChannel.class);

	private ChannelManager manager;
	private int endpointNumber;
	private long matchData;
	private int myHandle = 0;
	private MxAddress source;
	private Object owner = null;
	
	private InputStream myStream = null;

	// the state variables
	private boolean senderClosed = false;
	private boolean open = true;
	private boolean reading = false; // true when a post request is done and isn't finished yet.
	private boolean finishing = false; // true when the finish call is performed.
	private boolean posting = false; // true when the post call is performed.
	
	// the "do" variables
	private boolean requestPosted = false; //true when a request is posted to the MX library
	private ByteBuffer userBuf = null;
	private int initialPosition;
	private ByteBuffer overflowBuf;

	protected ReadChannel(ChannelManager manager, MxAddress source,
			int endpointNumber, long matchData) {
		overflowBuf = ByteBuffer.allocateDirect(Config.BUFSIZE);
		overflowBuf.flip();
		this.manager = manager;
		this.source = source;
		this.endpointNumber = endpointNumber;
		this.matchData = matchData;
		myHandle = JavaMx.handles.getHandle();
		
		if (logger.isDebugEnabled()) {
			logger.debug("ReadChannel <-- " + source.toString() + " : " + 
					Integer.toHexString(Matching.getPort(matchData)) 
					+  " created.");
		}
	}

	public int read(ByteBuffer dst) throws IOException {
		post(dst);
		return finish();
	}

	public void post(ByteBuffer dst) throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("post()");
		}
		synchronized (this) {
			while (reading && open) {
				try {
					wait();
				} catch (InterruptedException e) {
					// ignore
				}
			}
			if (!open) {
				throw new ClosedChannelException();
			}
			reading = true;
			posting = true;
		}
			
		try {
			doPost(dst);
		} catch (IOException e) {
			throw e;
		}
		synchronized(this) {
			posting = false;
			notifyAll();
		}
	}

	public int finish() throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("finish()");
		}
		
		synchronized(this) {
			while (finishing && open) {
				try {
					wait();
				} catch (InterruptedException e) {
					// ignore
				}
			}
			
			
			if (!open) {
				throw new ClosedChannelException();
			}
			
			if(!reading) {
				if (logger.isDebugEnabled()) {
					logger.debug("ILLEGAL STATE");
				}
				throw new IOException("Illegal state for finishing a request");
			}
			finishing = true;			
		}
		int result = -1;
		try {
			result = doFinish();
		} catch (MxException e) {
			// TODO Auto-generated catch block
		}
		
		synchronized(this) {
			finishing = false;
			reading = false;
			notifyAll();
		}
		if(logger.isDebugEnabled()) {
			logger.debug("ReadChannel read a message");
		}
	
		return result;
	}

	private void doPost(ByteBuffer dst) throws IOException {
		userBuf = dst;
		initialPosition = userBuf.position();
		
		if (overflowBuf.hasRemaining()) {
			if (overflowBuf.remaining() >= userBuf.remaining()) {
				int oldlimit = overflowBuf.limit();
				overflowBuf.limit(overflowBuf.position() + userBuf.remaining());
				userBuf.put(overflowBuf);
				overflowBuf.limit(oldlimit);
				if (logger.isDebugEnabled()) {
					logger.debug("request posted: got data from buffer");
				}
				return;
			} else {
				userBuf.put(overflowBuf);
				if (logger.isDebugEnabled()) {
					logger.debug("request posted: got data from buffer");
				}
			}
		
			overflowBuf.clear();
			if (JavaMx.iprobe(endpointNumber, matchData, Matching.MASK_ALL) < 0) {
				// no message in channel
				return;
			}
		}
	
		overflowBuf.clear();

		// no message in channel
		if (senderClosed) {
			// sender also closed already, so we handle this as an EOF, unless we already read some data from the buffer;
			if (logger.isDebugEnabled()) {
				logger.debug("request posted: sender closed and no new MX message");
			}
			return;
		}
		
		// post a recv request
		if(userBuf.position() != initialPosition) {
			// we already read some data from the overflowBuffer;
			return;
		}
		
		try {
			if (userBuf.remaining() > Config.BUFSIZE) {
				JavaMx.recv(userBuf, userBuf.position(), userBuf
						.remaining(), endpointNumber, myHandle, matchData);
			} else {
				JavaMx.recv(userBuf, userBuf.position(), userBuf
						.remaining(), overflowBuf, overflowBuf.position(),
						overflowBuf.remaining(), endpointNumber, myHandle,
						matchData);
			}
		} catch (MxException e) {
			//FIXME handle them
			throw e;
		}
		requestPosted = true;
		
		if (logger.isDebugEnabled()) {
			logger.debug("request posted: MX_recv posted");
		}		
		
		/*
		overflowBuf.clear();
		if (JavaMx.iprobe(endpointNumber, matchData, Matching.MASK_ALL) < 0) {
			// no message in channel
			if (senderClosed) {
				// sender also closed already, so we handle this as an EOF, unless we already read some data from the buffer;
				if (logger.isDebugEnabled()) {
					logger.debug("request posted: sender closed and no new MX message");
				}
				return;
			}
			
			// post a recv request
			if(userBuf.position() != initialPosition) {
				// we already read some data from the overflowBuffer;
				return;
			}
			
			try {
				if (userBuf.remaining() > Config.BUFSIZE) {
					JavaMx.recv(userBuf, userBuf.position(), userBuf
							.remaining(), endpointNumber, myHandle, matchData);
				} else {
					JavaMx.recv(userBuf, userBuf.position(), userBuf
							.remaining(), overflowBuf, overflowBuf.position(),
							overflowBuf.remaining(), endpointNumber, myHandle,
							matchData);
				}
			} catch (MxException e) {
				//FIXME handle them
				throw e;
			}
			requestPosted = true;
			
			if (logger.isDebugEnabled()) {
				logger.debug("request posted: MX_recv posted");
			}	
		} else {
			// there is some data available
			maximizeTransfer();
		}
		*/
	}
		
	private int doFinish() throws MxException {
		if(requestPosted) {
			try {
				finishRequest();
			} catch (MxException e) {
				//TODO
				throw e;
			} catch (ClosedChannelException e) {
				//TODO
				return -1;
			}
		}
		
		if(userBuf.hasRemaining() && JavaMx.iprobe(endpointNumber, matchData, Matching.MASK_ALL) >= 0) {
			try {
				maximizeTransfer();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		int result = userBuf.position() - initialPosition;
		userBuf = null;
		overflowBuf.flip();
		return result;
	}

	private void maximizeTransfer() throws MxException {
		do {
			if(logger.isDebugEnabled()) {
				logger.debug("maximizeloop");
			}
			try {
				int bytes = -1;
				if (userBuf.remaining() > Config.BUFSIZE) {
					JavaMx.recv(userBuf, userBuf.position(), userBuf
							.remaining(), endpointNumber, myHandle, matchData);
					while (bytes < 0) {
						bytes = JavaMx.test(endpointNumber, myHandle);
					}
					/*if(bytes < 0) {
						throw new Error("JavaMx.test() failed: Cannot happen here, message should be available");
					}*/
					userBuf.position(userBuf.position() + bytes);
				} else {
					JavaMx.recv(userBuf, userBuf.position(), userBuf
							.remaining(), overflowBuf, overflowBuf.position(),
							overflowBuf.remaining(), endpointNumber, myHandle,
							matchData);
					while (bytes < 0) {
						bytes = JavaMx.test(endpointNumber, myHandle); // test is sometimes not successful here
					}
					/*if(bytes < 0) {
						throw new Error("JavaMx.test() failed: Cannot happen here, message should be available");
					}*/
					if(bytes <= userBuf.remaining()) {
						userBuf.position(userBuf.position() + bytes);
					} else {
						overflowBuf.position(bytes - userBuf.remaining());
						userBuf.position(userBuf.limit());
						return;
					}
				}
			} catch (MxException e) {
				//FIXME handle them
				throw e;
			}
		} while(userBuf.hasRemaining() && JavaMx.iprobe(endpointNumber, matchData, Matching.MASK_ALL) >= 0);
	}

	private void finishRequest() throws MxException, ClosedChannelException {
		int bytes = -1;
		try {
			bytes = JavaMx.wait(endpointNumber, myHandle, 1000);
			while (bytes < 0) {
				if (senderClosed) {
					if (JavaMx.cancel(endpointNumber, myHandle)) {
						// We cannot finish this receive
						requestPosted = false;
						doClose();
						throw new ClosedChannelException();
					} // else we can finish the posted receive
				}
				bytes = JavaMx.wait(endpointNumber, myHandle, 1000); // wait a second
			}
			requestPosted = false;
			if(bytes <= userBuf.remaining()) {
				userBuf.position(userBuf.position() + bytes);
			} else {
				overflowBuf.position(bytes - userBuf.remaining());
				userBuf.position(userBuf.limit());
			}
		} catch (MxException e) {
			//TODO
			requestPosted = false;
			throw e;
		}
	}
	
	
	
	public int available() {
		if(reading) {
			return userBuf.position() - initialPosition;
		} else {
			return overflowBuf.remaining(); 
		}
	}

	public boolean isOpen() {
		return open;
	}

	public synchronized void close() throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("close()");
		}
		if(!open) {
			throw new ClosedChannelException();
		}
		if(!senderClosed) {
			manager.sendCloseMessage(this);
			synchronized(this) {
				try {
					wait(1000); //FIXME why wait? there must be a better way to do this.
				} catch (InterruptedException e) {
					// ignore
				}
			}
			if(!senderClosed) {
				// hard close
				senderClosed = true;
				//cancel and quit?
			}
		}
		//TODO checking
		if(requestPosted) {	
			try {
				finishRequest();
			} catch (IOException e) {
				//ignore it here
			}
		}
		if (open) {
			// purge all messages that are still in transit
			userBuf = ByteBuffer.allocateDirect(Config.BUFSIZE);
			while (JavaMx.iprobe(endpointNumber, matchData, Matching.MASK_ALL) != -1) {
				//receive a message
				//FIXME avoid buffer allocation here
				userBuf.clear();
				overflowBuf.clear();				
				maximizeTransfer();
			}	
		}
		doClose();
	}

	private synchronized void doClose() {
		if(!open) {
			notifyAll();
			return;
		}
		open = false;
		manager.readChannelCloses(Matching.getChannel(matchData));
		JavaMx.handles.releaseHandle(myHandle);
		notifyAll();
	}

	protected synchronized void senderClosedConnection() {
		if(!open || senderClosed) {
			return;
		}
		senderClosed = true;
		notifyAll();
	}

	protected MxAddress getSource() {
		return source;
	}

	protected int getPort() {
		return Matching.getPort(matchData);
	}

	public InputStream getInputStream() throws IOException {
		if (myStream == null) {
			myStream = new InputStream(this);
		}
		return myStream;
	}

	/**
	 * @return the owner
	 */
	public Object getOwner() {
		return owner;
	}

	/**
	 * @param owner the owner to set
	 */
	public void setOwner(Object owner) {
		this.owner = owner;
	}

	protected boolean containsData() throws IOException {
		// only true for own outputstream.
		return myStream == null ? false : myStream.available() > 0;
	}

	protected synchronized void isSelected(int msgSize) throws IOException {
		while (posting) {
			try {
				wait();
			} catch (InterruptedException e) {
				// ignore
			}
		}	
		
		if(!requestPosted) {
			throw new IOException("channel is selected when not possible");
		}
		requestPosted = false;
		if(msgSize <= userBuf.remaining()) {
			userBuf.position(userBuf.position() + msgSize);
		} else {
			overflowBuf.position(msgSize - userBuf.remaining());
			userBuf.position(userBuf.limit());
		}
	}
}
