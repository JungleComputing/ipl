package ibis.ipl.impl.mx;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public class MxReadChannel extends Matching implements ReadableByteChannel {
	int handle;
	boolean closed;
	protected MxChannelFactory factory;
	
	MxReadChannel(MxChannelFactory factory) {
		this.factory = factory;
		handle = JavaMx.handles.getHandle();
		closed = false;
	}

	public void close() {
		//TODO release handles and buffer(s)
		JavaMx.cancel(factory.handlerId, handle);
		
		JavaMx.handles.releaseHandle(handle);
		closed = true;
	}
	
	@Override
	protected void finalize() throws Throwable {
		// TODO Auto-generated method stub
		close();
		super.finalize();
	}

	public synchronized int read(ByteBuffer buffer) throws IOException {
		int msgSize;
		try {
			msgSize = JavaMx.wait(factory.handlerId, handle);
		} catch (MxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}

		if(msgSize == -1) { //error
			// TODO throw exception
			return -1;
		}
		try {
			JavaMx.recv(buffer, buffer.remaining(), factory.handlerId, handle, matchData);
		} catch (MxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
		return msgSize;
	}

	public boolean isOpen() {
		return !closed;
	}
	
	
	
}
