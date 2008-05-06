package ibis.ipl.impl.mx;

import java.io.IOException;
import java.nio.ByteBuffer;

import ibis.io.DataOutputStream;

public class MxDataOutputStream extends DataOutputStream {

	static final int BUFFER_SIZE = 8192;
	private ByteBuffer buffer;
	private MxWriteChannel channel;
	private boolean finished = true;
	
	private long count = 0;
	
	public MxDataOutputStream(MxWriteChannel channel) {
		buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
		this.channel = channel;
	}

	@Override
	public int bufferSize() {
		return buffer.capacity();
	}

	@Override
	public long bytesWritten() {
		// TODO Auto-generated method stub
		return count;
	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void flush() throws IOException {
		// TODO Auto-generated method stub
		// post send for this buffer
		if(finished) {
			buffer.flip();
			finished = false;
			if(channel.send(buffer) == false) {
				//TODO: error
			}
		}		
	}
	
	@Override
	public void finish() throws IOException {
		// TODO Auto-generated method stub
		if(!finished) {
			//TODO: wait for send operation to complete
			int bytesWritten = channel.finish();
			//TODO: check the number of bytes written
			finished = true;
		}
	}

	@Override
	public boolean finished() throws IOException {
		return finished;
	}

	@Override
	public void resetBytesWritten() {
		count = 0;
	}

	@Override
	public void write(int arg0) throws IOException {
		// TODO Auto-generated method stub
		
	}

	public void writeArray(boolean[] source, int offset, int length)
			throws IOException {
		// TODO Auto-generated method stub

	}

	public void writeArray(byte[] source, int offset, int length)
			throws IOException {
		// TODO Auto-generated method stub

	}

	public void writeArray(char[] source, int offset, int length)
			throws IOException {
		// TODO Auto-generated method stub

	}

	public void writeArray(short[] source, int offset, int length)
			throws IOException {
		// TODO Auto-generated method stub

	}

	public void writeArray(int[] source, int offset, int length)
			throws IOException {
		// TODO Auto-generated method stub

	}

	public void writeArray(long[] source, int offset, int length)
			throws IOException {
		// TODO Auto-generated method stub

	}

	public void writeArray(float[] source, int offset, int length)
			throws IOException {
		// TODO Auto-generated method stub

	}

	public void writeArray(double[] source, int offset, int length)
			throws IOException {
		// TODO Auto-generated method stub

	}

	public void writeBoolean(boolean value) throws IOException {
		// TODO Auto-generated method stub

	}

	public void writeByte(byte value) throws IOException {
		// TODO Auto-generated method stub

	}

	public void writeChar(char value) throws IOException {
		// TODO Auto-generated method stub

	}

	public void writeDouble(double value) throws IOException {
		// TODO Auto-generated method stub

	}

	public void writeFloat(float value) throws IOException {
		// TODO Auto-generated method stub

	}

	public void writeInt(int value) throws IOException {
		// TODO Auto-generated method stub

	}

	public void writeLong(long value) throws IOException {
		// TODO Auto-generated method stub

	}

	public void writeShort(short value) throws IOException {
		// TODO Auto-generated method stub

	}

}
