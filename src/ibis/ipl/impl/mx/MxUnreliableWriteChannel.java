package ibis.ipl.impl.mx;


import java.io.IOException;
import java.nio.ByteBuffer;

public class MxUnreliableWriteChannel extends MxWriteChannel {

	MxUnreliableWriteChannel(MxAddress target) throws IOException {
		super(target);
	}

	public void doSend(ByteBuffer buffer) {
		JavaMx.jmx_send(buffer, buffer.remaining(), link, matchData, handle); 
	}

}