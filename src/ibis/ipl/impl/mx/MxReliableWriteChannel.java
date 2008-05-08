package ibis.ipl.impl.mx;


import java.io.IOException;
import java.nio.ByteBuffer;

public class MxReliableWriteChannel extends MxWriteChannel {

	MxReliableWriteChannel(MxChannelFactory factory, MxAddress target, int filter) throws IOException {
		super(factory, target, filter);
	}

	public void doSend(ByteBuffer buffer) {
		JavaMx.sendSynchronous(buffer, buffer.remaining(), factory.handlerId, link, handle, matchData); 
	}

}