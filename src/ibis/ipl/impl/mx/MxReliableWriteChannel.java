package ibis.ipl.impl.mx;


import java.io.IOException;
import java.nio.ByteBuffer;

public class MxReliableWriteChannel extends MxWriteChannel {

	MxReliableWriteChannel(MxChannelFactory factory, MxAddress target, int filter) throws IOException {
		super(factory, target, filter);
	}

	public void doSend(ByteBuffer buffer) {
		JavaMx.sendSynchronous(buffer, buffer.position(), buffer.remaining(), factory.endpointId, link, handle, matchData); 
	}

	/* (non-Javadoc)
	 * @see ibis.ipl.impl.mx.MxWriteChannel#doSend(java.nio.ByteBuffer[])
	 */
	@Override
	protected void doSend(ByteBuffer[] buffers) {
		// TODO Auto-generated method stub
		
	}

}