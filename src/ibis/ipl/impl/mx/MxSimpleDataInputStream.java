package ibis.ipl.impl.mx;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.log4j.Logger;

public class MxSimpleDataInputStream extends MxDataInputStream {

	private static Logger logger = Logger.getLogger(MxSimpleDataInputStream.class);
	
	protected ReadChannel channel;

	
	/**
	 * @param channel the data source
	 */
	public MxSimpleDataInputStream(ReadChannel channel, ByteOrder order) {
		super(order);
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
