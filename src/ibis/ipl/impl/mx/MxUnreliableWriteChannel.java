package ibis.ipl.impl.mx;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import org.apache.log4j.Logger;

public class MxUnreliableWriteChannel extends MxWriteChannel {
	private static Logger logger = Logger.getLogger(MxReliableWriteChannel.class);
	
	MxUnreliableWriteChannel(MxChannelFactory factory, MxAddress target, int filter) throws IOException {
		super(factory, target, filter);
	}

	public void doSend(ByteBuffer buffer) {
		/*if (logger.isDebugEnabled()) {
			logger.debug("sending message...");
		}*/
		/*if (logger.isDebugEnabled()) {
			String data = "message contents: <<";
			ShortBuffer b = buffer.asShortBuffer();
			while(b.hasRemaining()) {
				data += Short.toString(b.get()) + " ";
			}	
			logger.debug(data + ">>");
		}*/
		JavaMx.send(buffer, buffer.position(), buffer.remaining(), factory.endpointId, link, handle, matchData);
		// FIXME buffer cannot be reused yet (ibuffered must be checked first)
		// At the moment, this is not a bug!!
	}

	/* (non-Javadoc)
	 * @see ibis.ipl.impl.mx.MxWriteChannel#doSend(java.nio.ByteBuffer[])
	 */
	@Override
	protected void doSend(SendBuffer buffer) {
		// TODO Auto-generated method stub
		
	}

}