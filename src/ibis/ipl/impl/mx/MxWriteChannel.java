package ibis.ipl.impl.mx;

import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class MxWriteChannel extends Matching {

	protected MxChannelFactory factory;
	protected int link;
	protected int handle;
	protected boolean sending = false;
	protected MxAddress target;
	protected long matchData;
	protected int msgSize;

	protected MxWriteChannel(MxChannelFactory factory, MxAddress target, int filter) throws IOException {
		this.factory = factory;
		this.link = JavaMx.links.getLink();
		//TODO timeouts?
		if(JavaMx.connect(factory.endpointId, link, target.nicId, target.endpointId, filter) == false) {
			throw new IOException("Could not connect to target");
		}
		this.handle = JavaMx.handles.getHandle();
		
		//TODO: get a handle
	}

	synchronized void close() {
		//TODO release handles, link and buffer(s)
		
	}

	public synchronized boolean send(ByteBuffer buffer) {
		if(!sending) {
			//send_handle = JavaMx.jmx_send(buffer, buffer.remaining(), link, match);
			doSend(buffer);
			sending = true;
			msgSize = buffer.remaining();
			return true;
		} else {
			// exception?
			return false;
		}
	}
	
	protected abstract void doSend(ByteBuffer buffer);

	public synchronized boolean finish() {
		if(sending) {
			int msgSize;
			try {
				msgSize = JavaMx.wait(factory.endpointId, handle);
			} catch (MxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
			if(msgSize == -1) {
				// TODO do something
			}
			sending = false;
			if(msgSize != this.msgSize) {
				//message truncated. Why?
				// TODO throw exception
				return false;
			}
			return true;
		} else {
			// TODO not sending, throw exception?
			return false;
		}
	}

	public synchronized boolean poll() {
		if(sending) {
			int msgSize;
			try {
				msgSize = JavaMx.test(factory.endpointId, handle);
			} catch (MxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
			if(msgSize == -1) {
				// TODO do something
				return false;
			}
			sending = false;
			return true;
		} else {
			// TODO not sending, throw exception?
			return false;
		}
	}

	public synchronized boolean isSending() {
		return sending;
	}
	
	@Override
	protected void finalize() throws Throwable {
		// TODO Auto-generated method stub
		close();
		super.finalize();
	}
}