package ibis.ipl.impl.mx;


import java.io.IOException;
import java.nio.ByteBuffer;

public class MxReliableWriteChannel extends MxWriteChannel {

	MxReliableWriteChannel(MxAddress target) throws IOException {
		super(target);
	}

	public void doSend(ByteBuffer buffer) {
		JavaMx.jmx_sendSynchronous(buffer, buffer.remaining(), link, matchData, handle); 
	}

}