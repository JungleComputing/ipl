package ibis.ipl.impl.mx;

import java.io.EOFException;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;

import org.apache.log4j.Logger;

import ibis.io.DataInputStream;

public abstract class MxDataInputStream extends DataInputStream implements Config {
	//FIXME receiving message that are too small for the read operations
	
	private static Logger logger = Logger.getLogger(MxDataInputStream.class);
	
	private long count = 0;
	private boolean closed = false;
	protected ByteBuffer buffer;
	
	public MxDataInputStream(ByteOrder order) {
		buffer = ByteBuffer.allocateDirect(BYTE_BUFFER_SIZE).order(order);
		buffer.flip(); // start with an empty buffer
	}

	protected void receive() throws IOException {
		//FIXME buffer.size() < bytes errors
		//TODO make this one beautiful ;-)
		if(buffer.hasRemaining()) {
			// ERROR still data left in buffers
			// throw exception
            throw new IOException("tried receive() while there was data"
                    + " left in the buffer");
		}
		/*if (logger.isDebugEnabled()) {
			logger.debug("Receiving message...");
		}*/
		// receive the next message
		buffer.clear();
		int count = 0;
		while (count <= 0) {
			try {
				int temp = doReceive(buffer);	
				if (temp < 0) {
					close();
					throw new EOFException("End of Stream received from channel");
				}
				count += temp;
			} catch (ClosedChannelException e) {
				close();
				// actually, we can throw it now
				throw e;
			}
		}
		if (count < 0) {
			close();
			throw new EOFException("End of Stream received from channel");
		}
		this.count += count;
		buffer.flip();
	}
	
	/**
	 * @param buffer The buffer in which the data can be put
	 * @return the number of bytes that are read
	 */
	protected abstract int doReceive(ByteBuffer buffer) throws IOException;
	
	@Override
	public void close() throws IOException {
		// also closes the associated channel
		if(!closed) {
			doClose();
		}
		closed = true;
	}
	
	protected abstract void doClose() throws IOException;
	
	@Override
	public int available() throws IOException {
		if(closed) {
			throw new IOException("Stream is closed");
		}
		int result = buffer.remaining();
		if (result == 0) {
			// check whether a MX message is on its way
			result = doAvailable();
			//TODO catch exception
		}
		
		return result;
	}
	
	protected abstract int doAvailable() throws IOException;
	
	
	public int waitUntilAvailable(long timeout) throws IOException {
		int result = buffer.remaining();
		if(result > 0) {
			return result;
		}
		result = doWaitUntilAvailable(timeout);
		// FIXME synchronize this check on 'closed'?
		if(closed) {
			throw new IOException("Stream is closed");
		}
		return result;
	}
	
	protected abstract int doWaitUntilAvailable(long timeout) throws IOException;
	
	@Override
	public int bufferSize() {
		return buffer.capacity();
	}

	@Override
	public long bytesRead() {
		return count;
	}
	
	@Override
	public void resetBytesRead() {
		count = 0;
	}

	@Override
	public boolean readBoolean() throws IOException {
		return (readByte() == ((byte) 1));
	}
	
	@Override
    public int read() throws IOException {
        try {
            return readByte() & 0377;
        } catch (EOFException e) {
            return -1;
        }
    }

    public byte readByte() throws IOException {
    	if(closed) {
			throw new IOException("Stream is closed");
		}
    	
        byte result;

        try {
            result = buffer.get();
        } catch (BufferUnderflowException e) {
            receive();
            result = buffer.get();
        }

        /*if (logger.isDebugEnabled()) {
            logger.debug("received byte: " + result);
        }*/

        return result;
    }

	public char readChar() throws IOException {
    	if(closed) {
			throw new IOException("Stream is closed");
		}
    	
        char result;

        try {
            result = buffer.getChar();
        } catch (BufferUnderflowException e) {
            receive();
            result = buffer.getChar();
        }

        /*if (logger.isDebugEnabled()) {
            logger.debug("received char: " + result);
        }*/
        
        return result;
	}

    public double readDouble() throws IOException {
    	if(closed) {
			throw new IOException("Stream is closed");
		}
    	
        double result;

        try {
            result = buffer.getDouble();
        } catch (BufferUnderflowException e) {
        	receive();
            result = buffer.getDouble();
        }

        /*if (logger.isDebugEnabled()) {
            logger.debug("received double: " + result);
        }*/
        
        return result;
    }

    public float readFloat() throws IOException {
    	if(closed) {
			throw new IOException("Stream is closed");
		}
    	
        float result;

        try {
            result = buffer.getFloat();
        } catch (BufferUnderflowException e) {
        	receive();
            result = buffer.getFloat();
        }

        /*if (logger.isDebugEnabled()) {
            logger.debug("received float: " + result);
        }*/
        
        return result;
    }

    public int readInt() throws IOException {
    	if(closed) {
			throw new IOException("Stream is closed");
		}
    	
        int result;

        try {
            result = buffer.getInt();
        } catch (BufferUnderflowException e) {
        	receive();
            result = buffer.getInt();
        }

        /*if (logger.isDebugEnabled()) {
            logger.debug("received int: " + result);
        }*/
        
        return result;
    }

    public long readLong() throws IOException {
    	if(closed) {
			throw new IOException("Stream is closed");
		}
    	
        long result;

        try {
            result = buffer.getLong();
        } catch (BufferUnderflowException e) {
        	receive();
            result = buffer.getLong();
        }

        /*if (logger.isDebugEnabled()) {
            logger.debug("received long: " + result);
        }*/

        return result;
    }

    public short readShort() throws IOException {
    	if(closed) {
			throw new IOException("Stream is closed");
		}
    	
        short result;

        try {
            result = buffer.getShort();
        } catch (BufferUnderflowException e) {
        	receive();
            result = buffer.getShort();
        }

        /*if (logger.isDebugEnabled()) {
            logger.debug("received short: " + result);
        }*/

        return result;
    }

	
	public void readArray(boolean[] destination, int offset, int length)
			throws IOException {
        for (int i = offset; i < (offset + length); i++) {
        	destination[i] = readBoolean();
        }
	}

	public void readArray(byte[] destination, int offset, int length)
			throws IOException {
        for (int i = offset; i < (offset + length); i++) {
        	destination[i] = readByte();
        }
	}

	public void readArray(char[] destination, int offset, int length)
			throws IOException {
        for (int i = offset; i < (offset + length); i++) {
        	destination[i] = readChar();
        }
	}

	public void readArray(short[] destination, int offset, int length)
			throws IOException {
        for (int i = offset; i < (offset + length); i++) {
        	destination[i] = readShort();
        }
	}

	public void readArray(int[] destination, int offset, int length)
			throws IOException {
        for (int i = offset; i < (offset + length); i++) {
        	destination[i] = readInt();
        }
	}

	public void readArray(long[] destination, int offset, int length)
			throws IOException {
        for (int i = offset; i < (offset + length); i++) {
        	destination[i] = readLong();
        }
	}

	public void readArray(float[] destination, int offset, int length)
			throws IOException {
        for (int i = offset; i < (offset + length); i++) {
        	destination[i] = readFloat();
        }
	}

	public void readArray(double[] destination, int offset, int length)
			throws IOException {
        for (int i = offset; i < (offset + length); i++) {
        	destination[i] = readDouble();
        }
	}
}
