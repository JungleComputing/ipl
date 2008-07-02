package ibis.ipl.impl.mx;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.log4j.Logger;

import ibis.io.DataOutputStream;

public class MxScatteringDataOutputStream extends DataOutputStream implements Config {
	// Based loosely on the NioAccumulator
	//FIXME not threadsafe
	
	static final int INITIAL_CONNECTIONS_SIZE = 8;
	private static Logger logger = Logger.getLogger(MxScatteringDataOutputStream.class);
	
	private ByteBuffer buffer;
	//private MxWriteChannel channel;
	private MxWriteChannel[] connections = new MxWriteChannel[INITIAL_CONNECTIONS_SIZE];
	int nrOfConnections = 0;
	private boolean sending = false;
	private boolean closed = false;
	private boolean closing = false;
	private long count;
	
	public MxScatteringDataOutputStream() {
		buffer = ByteBuffer.allocateDirect(BYTE_BUFFER_SIZE).order(ByteOrder.nativeOrder());
		count = 0;
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
		closing = true;
		flush();
		if(!closed) {
			for(int i = 0; i< nrOfConnections; i++) {
				logger.debug("close " + i);
				connections[i].close();				
			}
		}
		if(sending) {
			if(finished()) {
				closed = true;
			}
		} else {
			//FIXME
			closed = true;
		}
		
		if (logger.isDebugEnabled()) {
			logger.debug("closed!");
		}
	}

	@Override
	public synchronized void flush() throws IOException {
		logger.debug("flushing");
    	if(closed) {
			throw new IOException("Stream is closed");
		}
		if(buffer.position() == 0) {
			// buffer is empty, don't send it
			logger.debug("flushed nothing");
			return;
		}
		
		// post send for this buffer
		if(!sending) { //prevent sending the same buffer twice
			sending = true;
			buffer.flip().mark();
			long size = buffer.remaining();
			if(size == 0) {
				//empty buffer, don't send, and we are ready
				buffer.clear();
				sending = false;
				logger.debug("flushed nothing 2");
				return;
			}
			/*if (logger.isDebugEnabled()) {
				logger.debug("sending message...");
			}*/
			for(int i = 0; i< nrOfConnections; i++) {
				//FIXME catch exceptions
				
				logger.debug("flush " + i);
				buffer.reset();
				connections[i].write(buffer);
			}
			count += size;
		}
		logger.debug("flushed");
	}
	
	@Override
	public synchronized void finish() throws IOException {
		logger.debug("finishing");
    	if(closed) {
			throw new IOException("Stream is closed");
		}
		if(sending) {
			for(int i = 0; i< nrOfConnections; i++) {
				logger.debug("finish " + i);
				connections[i].finish();
				//FIXME catch exceptions
			}
			buffer.clear();
			sending = false;
		}
		if(closing) {
			closed = true;
		}
	}

	@Override
	public synchronized boolean finished() throws IOException {
		logger.debug("finished");
    	if(closed) {
			throw new IOException("Stream is closed");
		}
		if(!sending) {
			// we are not sending, so we are finished
			return true;
		}
		for(int i = 0; i< nrOfConnections; i++) {
			logger.debug("poll " + i);
			if( !connections[i].poll()) { //FIXME catch exceptions
				return false;
			}
		}

		// we are finished!
		sending = false;
		buffer.clear();
		if(closing) {
			closed = true;
		}
		return true;
	}

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
        if(closing) {
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
        if(closing) {
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
        if(closing) {
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
        if(closing) {
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
        if(closing) {
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
        if(closing) {
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
        if(closing) {
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

	protected synchronized void add(MxWriteChannel connection) {
		if(sending) {
			try {
				finish();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(1);
			}
		}
		if (nrOfConnections == connections.length) {
            MxWriteChannel[] newConnections = new MxWriteChannel[connections.length * 2];
            for (int i = 0; i < connections.length; i++) {
                newConnections[i] = connections[i];
            }
            connections = newConnections;
        }
        connections[nrOfConnections] = connection;
        logger.debug("Connection added at position " + nrOfConnections);
        nrOfConnections++;	
	}

	protected synchronized void remove(MxWriteChannel connection) throws IOException {
		logger.debug("remove");
		if(sending) {
			finish();
		}
		if(nrOfConnections == 0) {
			throw new IOException("no connection to remove");
		}
        for (int i = 0; i < nrOfConnections; i++) {
            if (connections[i] == connection) {
                nrOfConnections--;
                connections[i] = connections[nrOfConnections];
                connections[nrOfConnections] = null;
                return;
            }
        }
        throw new IOException("tried to remove non existing connections");
    }
}
