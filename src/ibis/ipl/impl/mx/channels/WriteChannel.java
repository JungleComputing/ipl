package ibis.ipl.impl.mx.channels;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;

import org.apache.log4j.Logger;

public class WriteChannel implements java.nio.channels.WritableByteChannel {
	private static Logger logger = Logger.getLogger(WriteChannel.class);

	private MxSocket socket;
	private boolean open = true, receiverClosed = false;
	private MxAddress target;
	private int endpointNumber;
	private long matchData;
	private int[] myHandles, msgSize;
	private int nHandles;
	private int nextHandle = 0;
	private int activeHandles = 0;
	private int myLink = 0;

	private boolean sending = false;

	protected WriteChannel(MxSocket socket, int endpointNumber, int link,
			long matchData, MxAddress target) {
		this.socket = socket;
		this.endpointNumber = endpointNumber;
		this.matchData = matchData;
		this.target = target;
		nHandles = Config.BUFFERS;
		myHandles = new int[nHandles];
		msgSize = new int[nHandles];
		for (int i = 0; i < nHandles; i++) {
			myHandles[i] = JavaMx.handles.getHandle();
			msgSize[i] = 0;
		}
		myLink = link;
		if (logger.isDebugEnabled()) {
			logger.debug("WriteChannel --> " + target.toString() + " : " + 
					Integer.toHexString(Matching.getPort(matchData)) 
					+  " created.");
		}
	}

	public OutputStream getOutputStream() {
		return new SimpleOutputStream(this);
	}

	public int post(ByteBuffer src) throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("post()");
		}
		synchronized (this) {
			while (sending) {
				try {
					wait();
				} catch (InterruptedException e) {
					// ignore
				}
			}
			if (!open) {
				throw new ClosedChannelException();
			}
			
			sending = true;
		}
		int msgSize = -1;
		
		try {
			msgSize = doPost(src);
		} catch (IOException e) {
			synchronized (this) {
				sending = false;
				notifyAll();
			}
			throw e;
		}
		
		synchronized (this) {
			sending = false;
			notifyAll();
		}
		return msgSize;
	}

	public void flush() throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("flush()");
		}
		synchronized (this) {
			while (sending) {
				try {
					wait();
				} catch (InterruptedException e) {
					// ignore
				}
			}
			if (!open) {
				throw new ClosedChannelException();
			}
			sending = true;
		}
		
		try {
			doFlush();
		} catch (IOException e) {
			synchronized (this) {
				sending = false;
				notifyAll();
			}
			throw e;
		}
		synchronized (this) {
			sending = false;
			notifyAll();
		}
	}

	public int write(ByteBuffer src) throws IOException {
		/*int result = post(src);
		flush();
		return result;
		*/
		synchronized (this) {
			while (sending) {
				try {
					wait();
				} catch (InterruptedException e) {
					// ignore
				}
			}
			if (!open) {
				throw new ClosedChannelException();
			}
			
			sending = true;
		}
		int result = -1;
		try {
			result = doPost(src);
			doFlush();
		} catch (IOException e) {
			synchronized (this) {
				sending = false;
				notifyAll();
			}
			throw e;
		}
		synchronized (this) {
			sending = false;
			notifyAll();
		}
		return result;
	}

	public boolean isOpen() {
		return open;
	}

	public synchronized void close() throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("close()");
		}
		if (!open) {
			return;
		}
		flush(); // FIXME leads to a SIGSEGV at JavaMX.wait() in doFlush()
		long matchData = Matching.setProtocol(this.matchData,
				Matching.PROTOCOL_DISCONNECT);
		JavaMx.sendSynchronous(null, 0, 0, endpointNumber, myLink, myHandles[0],
				matchData);
		JavaMx.wait(endpointNumber, myHandles[0], 1000); // when this will not
														// succeed, quit anyways
		doClose();
		// logger.debug("WriteChannel closed by user");
	}

	protected synchronized void receiverClosed() {
		if (!open) {
			return;
		}
		receiverClosed = true;
		// TODO cancel message which are in transit?
		doClose();
		// logger.debug("WriteChannel closed by ReadChannel");
	}

	private void doClose() {
		open = false;
		socket.removeWriteChannel(toString());
		JavaMx.disconnect(myLink);
		JavaMx.links.releaseLink(myLink);
		for (int i = 0; i < Config.BUFFERS; i++) {
			myHandles[i] = JavaMx.handles.getHandle();
			JavaMx.handles.releaseHandle(myHandles[i]);
		}
	}

	private int doPost(ByteBuffer src) throws IOException {
		int totalSize = src.remaining();
		while (src.hasRemaining()) {
			if (activeHandles == nHandles) {
				int result = JavaMx.wait(endpointNumber, myHandles[nextHandle],
						1000);
				while (result < 0) {
					synchronized (this) {
						if (!open) {
							// channel closed!!!
							// TODO cancel?
							if (receiverClosed) {
								throw new ClosedChannelException();
							} else {
								throw new AsynchronousCloseException();
							}
						}
					}
					result = JavaMx.wait(endpointNumber, myHandles[nextHandle],
							1000);
				}
				if (result != msgSize[nextHandle]) {
					// error
					close();
					throw new IOException("MsgSize error");
				}
				activeHandles--;
			}

			if (src.remaining() < Config.BUFSIZE) {
				msgSize[nextHandle] = src.remaining();
				JavaMx.send(src, src.position(), msgSize[nextHandle],
						endpointNumber, myLink, myHandles[nextHandle], matchData);
				src.position(src.limit());
			} else {
				msgSize[nextHandle] = Config.BUFSIZE;
				JavaMx.send(src, src.position(), msgSize[nextHandle],
						endpointNumber, myLink, myHandles[nextHandle], matchData);
				src.position(src.position() + Config.BUFSIZE);
			}
			nextHandle = (nextHandle + 1) % nHandles;
			activeHandles++;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Message of " + totalSize + " bytes sent");
		}
		return totalSize;
	}
	
	private void doFlush() throws IOException {
		nextHandle = (nextHandle + nHandles - activeHandles) % nHandles; 
		// modulo function can result in negative values?
		
		while (activeHandles > 0) {
			// wait for remaining messages to finish
			int result = JavaMx.wait(endpointNumber, myHandles[nextHandle], 1000);
			while (result < 0) {
				synchronized (this) {
					if (!open) {
						// channel closed!!!
						// TODO cancel?
						if (receiverClosed) {
							throw new ClosedChannelException();
						} else {
							throw new AsynchronousCloseException();
						}
					}
				}
				result = JavaMx.wait(endpointNumber, myHandles[nextHandle], 1000);
			}
			activeHandles--;
			if (result != msgSize[nextHandle]) {
				// error
				close();
				throw new IOException("MsgSize error");
			}
			nextHandle = (nextHandle + 1) % nHandles;
		}
	}
	
	@Override
	public String toString() {
		return createString(target, Matching.getPort(matchData));
	}

	protected static String createString(MxAddress address, int port) {
		return "WriteChannel:" + address.toString() + "("
				+ Integer.toString(port) + ")";
	}

}
