package ibis.ipl.impl.mx;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

public class MxDataInputStreamImpl extends MxDataInputStream {
	
	private static Logger logger = Logger.getLogger(MxDataInputStreamImpl.class);
	
	protected ReadChannel channel;

	
	/**
	 * @param channel the data source
	 */
	public MxDataInputStreamImpl(ReadChannel channel) {
		super();
		this.channel = channel;		
	}


	@Override
	protected void doClose() throws IOException {
		// FIXME DEBUG: fixing closing channels
		channel.close();
	}

	protected int doReceive(ByteBuffer buffer) throws IOException {
		return channel.read(buffer);	
	}

	@Override
	protected int doAvailable() throws IOException {
		//FIXME empty messages
		//TODO for now, we do not check the channel, but just say we have nothing, unless it is a local channel
		if(channel instanceof MxLocalChannel) {
			return channel.poll();
		}
		return 0;
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
