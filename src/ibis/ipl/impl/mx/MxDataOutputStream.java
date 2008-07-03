package ibis.ipl.impl.mx;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.log4j.Logger;

import ibis.io.DataOutputStream;

public abstract class MxDataOutputStream extends DataOutputStream implements Config {
	//FIXME not threadsafe

	private static Logger logger = Logger.getLogger(MxDataOutputStream.class);
	
	private ByteBuffer buffer;
	private boolean sending = false;
	private boolean closed = false;
	private long count = 0;
	
	protected MxDataOutputStream(ByteOrder order) {
		buffer = ByteBuffer.allocateDirect(BYTE_BUFFER_SIZE).order(order);
	}

	@Override
	public int bufferSize() {
		return buffer.capacity();
	}

	@Override
	public long bytesWritten() {
		return count;
	}

	@Override
	public synchronized void close() throws IOException {
		// Also closes the associated channel
		if (logger.isDebugEnabled()) {
			logger.debug("closing...");
		}
		if(closed) {
			return;
		}
		flush();
		if(sending) {
			finish();
		}
		closed = true;
		doClose();
		if (logger.isDebugEnabled()) {
			logger.debug("closed!");
		}
	}

	protected abstract void doClose() throws IOException;

	@Override
	public synchronized void flush() throws IOException {
    	if(closed) {
			throw new IOException("Stream is closed");
		}
		if(buffer.position() == 0) {
			// buffer is empty, don't send it
			return;
		}
		// post send for this buffer
		if(!sending) { //prevent sending the same buffer twice
			sending = true;
			buffer.flip();
			long size = buffer.remaining();
			if(size == 0) {
				//empty buffer, don't send, and we are ready
				buffer.clear();
				sending = false;
				return;
			}
			/*if (logger.isDebugEnabled()) {
				logger.debug("sending message...");
			}*/
			doWrite(buffer); 
			// TODO throws IOexceptions, catch them?
			count += size;
		}
	}
	
	protected abstract void doWrite(ByteBuffer buffer) throws IOException;

	@Override
	public synchronized void finish() throws IOException {
    	if(closed) {
			throw new IOException("Stream is closed");
		}
		if(sending) {
			doFinish();
			buffer.clear();
			sending = false;
		}
	}

	protected abstract void doFinish() throws IOException;

	@Override
	public synchronized boolean finished() throws IOException {
    	if(closed) {
			throw new IOException("Stream is closed");
		}
		if(!sending) {
			// we are not sending, so we are finished
			return true;
		}
		boolean finished = doFinished();
		if(finished) {
			sending = false;
			buffer.clear();
		}
		return finished;
	}

	protected abstract boolean doFinished() throws IOException;

	@Override
	public void resetBytesWritten() {
		count = 0;
	}

	@Override
	public void write(int arg0) throws IOException {
		writeByte((byte) arg0);
	}

	public void writeBoolean(boolean value) throws IOException {
        if (value) {
            writeByte((byte) 1);
        } else {
            writeByte((byte) 0);
        }
	}

	public void writeByte(byte value) throws IOException {
        /*if (logger.isDebugEnabled()) {
            logger.debug("writeByte(" + value + ")");
        }*/
        if(closed) {
        	throw new IOException("Stream is closed");
        }
        try {
            buffer.put(value);
        } catch (BufferOverflowException e) {
            // buffer was full, send
            flush();
            finish();
            // and try again
            buffer.put(value);
        }
	}

	public void writeChar(char value) throws IOException {
        /*if (logger.isDebugEnabled()) {
            logger.debug("writeChar(" + value + ")");
        }*/
        if(closed) {
        	throw new IOException("Stream is closed");
        }
        try {
            buffer.putChar(value);
        } catch (BufferOverflowException e) {
            // buffer was full, send
            flush();
            finish();
            // and try again
            buffer.putChar(value);
        }
	}

	public void writeDouble(double value) throws IOException {
        /*if (logger.isDebugEnabled()) {
            logger.debug("writeDouble(" + value + ")");
        }*/
        if(closed) {
        	throw new IOException("Stream is closed");
        }
        try {
            buffer.putDouble(value);
        } catch (BufferOverflowException e) {
            // buffer was full, send
            flush();
            finish();
            // and try again
            buffer.putDouble(value);
        }
	}

	public void writeFloat(float value) throws IOException {
        /*if (logger.isDebugEnabled()) {
            logger.debug("writeFloat(" + value + ")");
        }*/
        if(closed) {
        	throw new IOException("Stream is closed");
        }
        try {
            buffer.putFloat(value);
        } catch (BufferOverflowException e) {
            // buffer was full, send
            flush();
            finish();
            // and try again
            buffer.putFloat(value);
        }
	}

	public void writeInt(int value) throws IOException {
        /*if (logger.isDebugEnabled()) {
            logger.debug("writeInt(" + value + ")");
        }*/
        if(closed) {
        	throw new IOException("Stream is closed");
        }
        try {
            buffer.putInt(value);
        } catch (BufferOverflowException e) {
            // buffer was full, send
            flush();
            finish();
            // and try again
            buffer.putInt(value);
        }
	}

	public void writeLong(long value) throws IOException {
        if(closed) {
        	throw new IOException("Stream is closed");
        }
        /*if (logger.isDebugEnabled()) {
            logger.debug("writeLong(" + value + ")");
        }*/
        try {
            buffer.putLong(value);
        } catch (BufferOverflowException e) {
            // buffer was full, send
            flush();
            finish();
            // and try again
            buffer.putLong(value);
        }
	}

	public void writeShort(short value) throws IOException {
        if(closed) {
        	throw new IOException("Stream is closed");
        }
        /*if (logger.isDebugEnabled()) {
            logger.debug("writeShort(" + value + ")");
        }*/
        try {
            buffer.putShort(value);
        } catch (BufferOverflowException e) {
            // buffer was full, send
            flush();
            finish();
            // and try again
            buffer.putShort(value);
        }
	}
	
	public void writeArray(boolean[] source, int offset, int length)
			throws IOException {
        for (int i = offset; i < (offset + length); i++) {
            writeBoolean(source[i]);
        }
	}

	public void writeArray(byte[] source, int offset, int length)
			throws IOException {
        for (int i = offset; i < (offset + length); i++) {
            writeByte(source[i]);
        }
	}

	public void writeArray(char[] source, int offset, int length)
			throws IOException {
        for (int i = offset; i < (offset + length); i++) {
            writeChar(source[i]);
        }
	}

	public void writeArray(short[] source, int offset, int length)
			throws IOException {
        for (int i = offset; i < (offset + length); i++) {
            writeShort(source[i]);
        }
	}

	public void writeArray(int[] source, int offset, int length)
			throws IOException {
        for (int i = offset; i < (offset + length); i++) {
            writeInt(source[i]);
        }
	}

	public void writeArray(long[] source, int offset, int length)
			throws IOException {
        for (int i = offset; i < (offset + length); i++) {
            writeLong(source[i]);
        }
	}

	public void writeArray(float[] source, int offset, int length)
			throws IOException {
        for (int i = offset; i < (offset + length); i++) {
            writeFloat(source[i]);
        }
	}

	public void writeArray(double[] source, int offset, int length)
			throws IOException {
        for (int i = offset; i < (offset + length); i++) {
            writeDouble(source[i]);
        }
	}	
}
