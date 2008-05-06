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

	protected MxWriteChannel(MxAddress target) throws IOException {
		this.link = JavaMx.links.getLink();
		if(JavaMx.jmx_connect(target.nic_id, target.endpoint_id, link) == false) {
			throw new IOException("Could not connect to target");
		}
		handle = JavaMx.handles.getHandle();
		
		//TODO: get a handle
	}

	void close() {
		//TODO release handles, link and buffer(s)
		
	}

	public boolean send(ByteBuffer buffer) {
		if(!sending) {
			//send_handle = JavaMx.jmx_send(buffer, buffer.remaining(), link, match);
			doSend(buffer);
			sending = true;
			return true;
		} else {
			// exception?
			return false;
		}
	}
	
	protected abstract void doSend(ByteBuffer buffer);

	public int finish() {
		if(sending) {
			int written = JavaMx.jmx_wait(handle);
			sending = false;
			return written;
		} else {
			// exception?
			return -1;
		}
	}

	public int poll() {
		if(sending) {
			int written = JavaMx.jmx_test(handle);
			if(written >= 0) { 
				sending = false;
			}
			return written;
		} else {
			// exception?
			return -1;
		}
	}

	public boolean isSending() {
		return sending;
	}
	
	@Override
	protected void finalize() throws Throwable {
		// TODO Auto-generated method stub
		close();
		super.finalize();
	}
}