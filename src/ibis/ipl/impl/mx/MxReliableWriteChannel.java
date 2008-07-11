package ibis.ipl.impl.mx;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import org.apache.log4j.Logger;

public class MxReliableWriteChannel extends MxWriteChannel {

	private static Logger logger = Logger.getLogger(MxReliableWriteChannel.class);
	
	MxReliableWriteChannel(MxChannelFactory factory, MxAddress target, int filter) throws IOException {
		super(factory, target, filter);
	}

	//public void doSend(ByteBuffer buffer) {
		/*if (logger.isDebugEnabled()) {
			String data = "message contents: <<";
			ShortBuffer b = buffer.asShortBuffer();
			while(b.hasRemaining()) {
				data += Short.toString(b.get()) + " ";
			}	
			logger.debug(data + ">>");
		}*/
//		JavaMx.sendSynchronous(buffer, buffer.position(), buffer.remaining(), factory.endpointId, link, handle, matchData);
		// buffer cannot be reused yet!
		// At the moment, this is not a bug!!
//	}

	/* (non-Javadoc)
	 * @see ibis.ipl.impl.mx.MxWriteChannel#doSend(java.nio.ByteBuffer[])
	 */
	@Override
	protected void doSend(SendBuffer buffer, int handle) {
		ByteBuffer[] bufs = buffer.byteBuffers;
		JavaMx.sendSynchronous(
				bufs[SendBuffer.HEADER], bufs[SendBuffer.HEADER].remaining(),
				bufs[SendBuffer.LONGS], bufs[SendBuffer.LONGS].remaining(),
				bufs[SendBuffer.DOUBLES], bufs[SendBuffer.DOUBLES].remaining(),
				bufs[SendBuffer.INTS], bufs[SendBuffer.INTS].remaining(),
				bufs[SendBuffer.FLOATS], bufs[SendBuffer.FLOATS].remaining(),
				bufs[SendBuffer.SHORTS], bufs[SendBuffer.SHORTS].remaining(),
				bufs[SendBuffer.CHARS], bufs[SendBuffer.CHARS].remaining(),
				bufs[SendBuffer.BYTES], bufs[SendBuffer.BYTES].remaining(),
				bufs[SendBuffer.PADDING], bufs[SendBuffer.PADDING].remaining(),
				factory.endpointId, link, handle, matchData
				);
		
	}

}