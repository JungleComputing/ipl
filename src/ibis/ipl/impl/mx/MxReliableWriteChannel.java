package ibis.ipl.impl.mx;


import java.io.IOException;
import java.nio.ByteBuffer;

public class MxReliableWriteChannel extends MxWriteChannel {
	
	MxReliableWriteChannel(MxChannelFactory factory, MxAddress target, int filter) throws IOException {
		super(factory, target, filter);
	}


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