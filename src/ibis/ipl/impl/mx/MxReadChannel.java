package ibis.ipl.impl.mx;

import ibis.ipl.ConnectionClosedException;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MxReadChannel extends Matching {
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
		if(closed) {
			return;
		}
		closed = true;
		while(receiving) {
			JavaMx.cancel(factory.endpointId, handle); //really stop current reception, or wait for it?
		}
		JavaMx.handles.releaseHandle(handle);
	}
	
	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}

	
	public int read(ByteBuffer buffer) throws IOException {
		int msgSize;
		
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
			JavaMx.recv(buffer, buffer.position(), buffer.remaining(), endpointId, handle, matchData);
			msgSize = JavaMx.wait(factory.endpointId, handle);
			buffer.position(buffer.limit());
		} catch (MxException e) {
			receiving = false;
			notifyAll();
			throw(e);
		}
		receiving = false;
		notifyAll();
		
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
		return = JavaMx.iprobe(endpointId, matchData, MASK_NONE); //-1 when no message available
	}
}
