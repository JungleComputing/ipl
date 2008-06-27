package ibis.ipl.impl.mx;

import java.io.EOFException;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.log4j.Logger;

import ibis.io.DataInputStream;

public class MxSimpleDataInputStream extends DataInputStream implements Config {

	private static Logger logger = Logger.getLogger(MxSimpleDataInputStream.class);
	
	MxReadChannel channel;
	long count;
	boolean closed;

	private ByteBuffer buffer;
	
	/**
	 * @param channel the data source
	 */
	public MxSimpleDataInputStream(MxReadChannel channel, ByteOrder order) {
		this.channel = channel;
		count = 0;
		buffer = ByteBuffer.allocateDirect(BYTE_BUFFER_SIZE).order(order);
		buffer.flip(); // start with an empty buffer
		closed = false;
	}

	@Override
	public int bufferSize() {
		return buffer.capacity();
	}

	@Override
	public long bytesRead() {
		return count;
	}

	@Override
	public void close() throws IOException {
		// also closes the associated channel
		if(!closed) {
			channel.close();
		}
		closed = true;
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

	/**
	 * receives a packet from the channel in the buffer.
	 * @throws IOException 
	 */
	private void receive() throws IOException {
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
		while (count == 0) {
			count = channel.read(buffer, 0);
			// TODO change this when the read op stops returning -1
			//TODO catch ConnClosedException?
		}
		if (count < 0) {
			throw new IOException("error receiving message");
		}
		this.count += count;
		buffer.flip();	
	}

	@Override
	public int available() throws IOException {
		if(closed) {
			throw new IOException("Stream is closed");
		}
		int result = buffer.remaining();
		if (result == 0) {
			// check whether a MX message is on its way
			result = channel.poll();
		}
		return result;
	}
	

	public int WaitUntilAvailable(long timeout) throws IOException {
		long deadline = System.currentTimeMillis() + timeout;
		int result = buffer.remaining();
		while (result == -1 && !closed) {
			// check whether a MX message is on its way
			//FIXME empty messages)
			if(timeout <= 0) {
				result = channel.poll(0);
			} else {
				result = channel.poll(System.currentTimeMillis() - deadline);
				if(System.currentTimeMillis() > deadline) {
					return -1;
				}
			}
		}
		if(closed) {
			throw new IOException("Stream is closed");
		}
		return result;
	}
	
}
