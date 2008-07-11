package ibis.ipl.impl.mx;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.log4j.Logger;

public class MxBufferedDataInputStreamImpl extends MxBufferedDataInputStream {
	/* Actually, this class is the same as MxSimpleDataInputStream */
	
	private static Logger logger = Logger.getLogger(MxBufferedDataInputStreamImpl.class);
	
	protected ReadChannel channel;

	
	/**
	 * @param channel the data source
	 */
	public MxBufferedDataInputStreamImpl(ReadChannel channel) {
		super();
		this.channel = channel;		
	}


	@Override
	protected void doClose() throws IOException {
		channel.close();
	}

	protected int doReceive(ByteBuffer buffer) throws IOException {
		return channel.read(buffer, 0);	
	}

	@Override
	protected int doAvailable() throws IOException {
		//FIXME empty messages
		return channel.poll();
	}
	
	protected int doWaitUntilAvailable(long timeout) throws IOException {
		//FIXME empty messages
		if(timeout <= 0) {
			return channel.poll(0);
		} else {
			return channel.poll(timeout);
		}
	}
	
}
