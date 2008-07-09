package ibis.ipl.impl.mx;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.log4j.Logger;

import ibis.io.DataOutputStream;

public class MxSimpleDataOutputStream extends MxUnbufferedDataOutputStream implements Config {
	//FIXME not threadsafe

	private static Logger logger = Logger.getLogger(MxSimpleDataOutputStream.class);
	
	private MxWriteChannel channel;
	
	public MxSimpleDataOutputStream(MxWriteChannel channel, ByteOrder order) {
		super(order);
		this.channel = channel;
	}
	

	@Override
	protected void doClose() throws IOException {
		channel.close();
	}


	@Override
	protected void doFinish() throws IOException {
		channel.finish();
		buffer.clear();
	}


	@Override
	protected boolean doFinished() throws IOException {
		if (channel.isFinished()) {
			//finished
			buffer.clear();
			return true;
		}
		return false;
	}


	@Override
	protected long doWrite() throws IOException {
		buffer.flip();
		channel.write(buffer);
		return buffer.remaining();
	}
}
