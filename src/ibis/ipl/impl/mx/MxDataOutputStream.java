package ibis.ipl.impl.mx;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import ibis.io.DataOutputStream;

public abstract class MxDataOutputStream extends DataOutputStream {

	private static Logger logger = Logger.getLogger(MxDataOutputStream.class);
	private boolean sending = false;
	protected boolean closed = false;
	private long count = 0;

	public MxDataOutputStream() {
		super();
	}

	@Override
	public long bytesWritten() {
		return count;
	}

	@Override
	public synchronized void close() throws IOException {
		// Also closes the associated channel
		if(closed) {
			return;
		}
		flush();
		if(sending) {
			finish();
		}
		closed = true;
		doClose();
		//logger.debug("closed");
	}

	protected abstract void doClose() throws IOException;

	@Override
	public synchronized void flush() throws IOException {
		if(closed) {
			throw new IOException("Stream is closed");
		}
		if(sending) {
			//FIXME remove this
			System.out.println("MxDOS: flush() called when still sending");
			finish();
			//sending will become false
			//TODO should this result in an exception?
		}
		// post send for this buffer
		
		if(isEmpty()) {
			// buffer is empty, don't send it
			return;
		}
		sending = true;
		count += doWrite(); 
		// TODO throws IOexceptions, catch them?
		// we are already sending a buffer!
		
	}

	protected abstract long doWrite() throws IOException;
	protected abstract boolean isEmpty();

	@Override
	public synchronized void finish() throws IOException {
		if(closed) {
			throw new IOException("Stream is closed");
		}
		if(sending) {
			doFinish();
			sending = false;
		}
	}

	protected abstract void doFinish() throws IOException;

	@Override
	public synchronized boolean finished() throws IOException {
		if(closed) {
			throw new IOException("Stream is closed");
		}
		if(!sending) {
			// we are not sending, so we are finished
			return true;
		}
		boolean finished = doFinished();
		if(finished) {
			sending = false;
		}
		return finished;
	}

	protected abstract boolean doFinished() throws IOException;

	@Override
	public void resetBytesWritten() {
		count = 0;
	}

}