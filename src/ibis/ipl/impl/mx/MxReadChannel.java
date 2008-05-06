package ibis.ipl.impl.mx;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public class MxReadChannel extends Matching implements ReadableByteChannel {
	int handle;
	boolean closed;
	
	MxReadChannel() {
		handle = JavaMx.handles.getHandle();
		closed = false;
	}

	public void close() {
		//TODO release handles and buffer(s)
		JavaMx.handles.releaseHandle(handle);
		closed = true;
	}
	
	@Override
	protected void finalize() throws Throwable {
		// TODO Auto-generated method stub
		close();
		super.finalize();
	}

	public int read(ByteBuffer buffer) throws IOException {
		JavaMx.jmx_wait(handle);
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean isOpen() {
		return !closed;
	}
	
	
	
}
