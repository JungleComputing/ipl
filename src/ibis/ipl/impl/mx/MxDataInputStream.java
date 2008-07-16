package ibis.ipl.impl.mx;

import ibis.io.DataInputStream;

import java.io.EOFException;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.ClosedChannelException;

import org.apache.log4j.Logger;


public abstract class MxDataInputStream extends DataInputStream implements Config { // MxDataInputStream implements Config {
	
	private static Logger logger = Logger.getLogger(MxDataInputStream.class);
	
	private static final int SIZEOF_BYTE = 1;
    private static final int SIZEOF_CHAR = 2;
    private static final int SIZEOF_SHORT = 2;
    private static final int SIZEOF_INT = 4;
    private static final int SIZEOF_FLOAT = 4;
    private static final int SIZEOF_LONG = 8;
    private static final int SIZEOF_DOUBLE = 8;

    private static final int LONGS = 1;
    private static final int DOUBLES = 2;
    private static final int INTS = 3;
    private static final int FLOATS = 4;
    private static final int SHORTS = 5;
    private static final int CHARS = 6;
    private static final int BYTES = 7;
    private static final int SIZEOF_HEADER = 16;
    
    /** the size of the primitive buffers + header and padding sizes */
    private static final int BYTE_BUFFER_SIZE = 7 * PRIMITIVE_BUFFER_SIZE + SIZEOF_HEADER + SendBuffer.SIZEOF_PADDING;
    
	private ByteBuffer buffer;
	
    private ShortBuffer header;
    private LongBuffer longs;
    private DoubleBuffer doubles;
    private FloatBuffer floats;
    private IntBuffer ints;
    private ShortBuffer shorts;
    private CharBuffer chars;
    private ByteBuffer bytes;
    
    private ByteOrder order;

    private long count = 0;
	private boolean closed = false;
    
	public MxDataInputStream() {
		
		buffer = ByteBuffer.allocateDirect(BYTE_BUFFER_SIZE).order(order);
		buffer.flip(); // start with an empty buffer
		
		this.order = ByteOrder.BIG_ENDIAN;
		
        initViews(order);

        // make the views appear empty
        header.limit(0);
        longs.limit(0);
        doubles.limit(0);
        floats.limit(0);
        ints.limit(0);
        shorts.limit(0);
        chars.limit(0);
        bytes.limit(0);	
	}

    /**
     * (re) Initialize buffers in the right byte order.
     */
    protected void initViews(ByteOrder order) {
    	/* taken from NioDissipator */
        int position;
        int limit;

        //logger.debug("initializing views in " + order + " byte order");


        // remember position and limit;
        position = buffer.position();
        limit = buffer.limit();

        buffer.order(order);
        
        // clear so views will be set correctly
        buffer.clear();

        header = buffer.asShortBuffer();
        longs = buffer.asLongBuffer();
        doubles = buffer.asDoubleBuffer();
        floats = buffer.asFloatBuffer();
        ints = buffer.asIntBuffer();
        shorts = buffer.asShortBuffer();
        chars = buffer.asCharBuffer();
        bytes = buffer.duplicate();

        buffer.position(position);
        buffer.limit(limit);
    }
	
    private int remaining() {
    	/* taken form NioDissipator */
        return ((longs.remaining() * SIZEOF_LONG)
                + (doubles.remaining() * SIZEOF_DOUBLE)
                + (ints.remaining() * SIZEOF_INT)
                + (floats.remaining() * SIZEOF_INT)
                + (shorts.remaining() * SIZEOF_SHORT)
                + (chars.remaining() * SIZEOF_CHAR)
                + (bytes.remaining() * SIZEOF_BYTE));
    }
    
    /**
     * Sets a view correctly.
     * 
     * All received data is guaranteed to be aligned since we always send
     * buffers with ((size % MAX_DATA_SIZE) == 0)
     */
    private int setView(Buffer view, int start, int bytes, int dataSize) {
    	/* based on NioDissipator */
        int result = start + bytes;
        
        view.limit((start + bytes) / dataSize);
        view.position(start / dataSize);

        /*
            logger.debug("setView: set view: position(" + view.position()
                    + ") limit(" + view.limit() + "), in bytes: position("
                    + (view.position() * dataSize) + ") limit("
                    + (view.limit() * dataSize) + ")");
        */
        return result;
    }
    
    protected void receive() throws IOException {
    	ByteOrder receivedOrder;
		if(remaining() > 0) {
			// ERROR still data left in buffers
            throw new IOException("tried receive() while there was data"
                    + " left in the buffer: " + remaining());
		}
		// receive the next message
		buffer.clear();
		int count = 0;
		while (count <= 0) {
			try {
				count = doReceive(buffer);	
				if (count < 0) {
					close();
					throw new EOFException("End of Stream received from channel");
				}
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
		
		bytes.clear();
        if (bytes.get(0) == ((byte) 1)) {
            receivedOrder = ByteOrder.BIG_ENDIAN;
        } else {
            receivedOrder = ByteOrder.LITTLE_ENDIAN;
        }
        if (order != receivedOrder) {
            // our buffers are in the wrong order, re-initialize
            order = receivedOrder;
            initViews(order);
        }
        
		int next;
		short[] headerArray = new short[SIZEOF_HEADER / SIZEOF_SHORT];
		next = setView(header, 0, SIZEOF_HEADER, SIZEOF_SHORT);
		header.get(headerArray);
		
        next = setView(longs, next, headerArray[LONGS], SIZEOF_LONG);
        next = setView(doubles, next, headerArray[DOUBLES], SIZEOF_DOUBLE);
        next = setView(ints, next, headerArray[INTS], SIZEOF_INT);
        next = setView(floats, next, headerArray[FLOATS], SIZEOF_FLOAT);
        next = setView(shorts, next, headerArray[SHORTS], SIZEOF_SHORT);
        next = setView(chars, next, headerArray[CHARS], SIZEOF_CHAR);
        next = setView(bytes, next, headerArray[BYTES], SIZEOF_BYTE);
        
        logger.debug("received: l[" + longs.remaining() + "] d["
                    + doubles.remaining() + "] i[" + ints.remaining() + "] f["
                    + floats.remaining() + "] s[" + shorts.remaining() + "] c["
                    + chars.remaining() + "] b[" + bytes.remaining() + "]");
         
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
		int result = remaining();
		if (result == 0) {
			// check whether a MX message is on its way
			result = doAvailable();
			//TODO catch exception
		}
		
		return result;
	}
	
	protected abstract int doAvailable() throws IOException;
	
	
	public int waitUntilAvailable(long timeout) throws IOException {
		int result = remaining();
		while (result <= 0) {
			result = doWaitUntilAvailable(timeout);
			synchronized(this) {
				if(closed) {
					throw new IOException("Stream is closed");
				}
			} 
			if (result > 0) {
				//TODO This is very good for the performance (latency), now do this in a nicer way
				receive();
			}
		}
		return result;
	}
	
	protected abstract int doWaitUntilAvailable(long timeout) throws IOException;
	
	@Override
	public int bufferSize() {
		// TODO is this right?
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


	
	public boolean readBoolean() throws IOException {
        return (readByte() == ((byte) 1));
    }

    public byte readByte() throws IOException {
        byte result;

        try {
            result = bytes.get();
        } catch (BufferUnderflowException e) {
            receive();
            result = bytes.get();
        }
        //logger.debug("received byte: " + result);
        return result;
    }

    public int read() throws IOException {
        try {
            return readByte() & 0377;
        } catch (EOFException e) {
            return -1;
        }
    }

    public char readChar() throws IOException {
        try {
            return chars.get();
        } catch (BufferUnderflowException e) {
            receive();
            return chars.get();
        }
    }

    public short readShort() throws IOException {
        try {
            return shorts.get();
        } catch (BufferUnderflowException e) {
            receive();
            return shorts.get();
        }
    }

    public int readInt() throws IOException {
        try {
            return ints.get();
        } catch (BufferUnderflowException e) {
            receive();
            return ints.get();
        }
    }

    public long readLong() throws IOException {
        try {
            return longs.get();
        } catch (BufferUnderflowException e) {
            receive();
            return longs.get();
        }
    }

    public float readFloat() throws IOException {
        try {
            return floats.get();
        } catch (BufferUnderflowException e) {
            receive();
            return floats.get();
        }
    }

    public double readDouble() throws IOException {
        try {
            return doubles.get();
        } catch (BufferUnderflowException e) {
            receive();
            return doubles.get();
        }
    }
    
    @Override
    public int read(byte ref[], int off, int len) throws IOException {
    	// from java.io.InputStream
    	// not strictly needed in Ibis
        try {
            bytes.get(ref, off, len);
        } catch (BufferUnderflowException e) {
            // do this the hard way
            int left = len;
            int offset = off;
            int size;

            try {
                while (left > 0) {
                    // copy as much as possible to the buffer
                    size = Math.min(left, bytes.remaining());
                    bytes.get(ref, offset, size);
                    offset += size;
                    left -= size;

                    // if still needed, fetch some more bytes from the
                    // channel
                    if (left > 0) {
                        receive();
                    }
                }
            } catch (EOFException e2) {
                len = offset - off;
                if (len == 0) {
                    return -1;
                }
            }
        }
/*
        if (logger.isDebugEnabled()) {
            String message = "received byte[], Contents: ";
            for (int i = off; i < (off + len); i++) {
                message = message + ref[i] + " ";
            }

            logger.debug(message);
        }*/
        return len;
    }
    
    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public void readArray(boolean ref[], int off, int len) throws IOException {
        for (int i = off; i < (off + len); i++) {
            ref[i] = ((readByte() == (byte) 1) ? true : false);
        }
    }

    public void readArray(byte ref[], int off, int len) throws IOException {
        try {
            bytes.get(ref, off, len);
        } catch (BufferUnderflowException e) {
            // do this the hard way
            int left = len;
            int offset = off;
            int size;

            while (left > 0) {
                // copy as much as possible to the buffer
                size = Math.min(left, bytes.remaining());
                bytes.get(ref, offset, size);
                offset += size;
                left -= size;

                // if still needed, fetch some more bytes from the
                // channel
                if (left > 0) {
                    receive();
                }
            }
        }
        /*
        if (logger.isDebugEnabled()) {
            String message = "received byte[], Contents: ";
            for (int i = off; i < (off + len); i++) {
                message = message + ref[i] + " ";
            }

            logger.debug(message);
        }
        */
    }

    public void readArray(char ref[], int off, int len) throws IOException {
        try {
            chars.get(ref, off, len);
        } catch (BufferUnderflowException e) {
            // do this the hard way
            int left = len;
            int size;

            while (left > 0) {
                // copy as much as possible to the buffer
                size = Math.min(left, chars.remaining());
                chars.get(ref, off, size);
                off += size;
                left -= size;

                // if still needed, fetch some more bytes from the
                // channel
                if (left > 0) {
                    receive();
                }
            }
        }
    }

    public void readArray(short ref[], int off, int len) throws IOException {
        try {
            shorts.get(ref, off, len);
        } catch (BufferUnderflowException e) {
            // do this the hard way
            int left = len;
            int size;

            while (left > 0) {
                // copy as much as possible to the buffer
                size = Math.min(left, shorts.remaining());
                shorts.get(ref, off, size);
                off += size;
                left -= size;

                // if still needed, fetch some more bytes from the
                // channel
                if (left > 0) {
                    receive();
                }
            }
        }
    }

    public void readArray(int ref[], int off, int len) throws IOException {
        try {
            ints.get(ref, off, len);
        } catch (BufferUnderflowException e) {
            // do this the hard way
            int left = len;
            int size;

            while (left > 0) {
                // copy as much as possible to the buffer
                size = Math.min(left, ints.remaining());
                ints.get(ref, off, size);
                off += size;
                left -= size;

                // if still needed, fetch some more bytes from the
                // channel
                if (left > 0) {
                    receive();
                }
            }
        }
    }

    public void readArray(long ref[], int off, int len) throws IOException {
        try {
            longs.get(ref, off, len);
        } catch (BufferUnderflowException e) {
            // do this the hard way
            int left = len;
            int size;

            while (left > 0) {
                // copy as much as possible to the buffer
                size = Math.min(left, longs.remaining());
                longs.get(ref, off, size);
                off += size;
                left -= size;

                // if still needed, fetch some more bytes from the
                // channel
                if (left > 0) {
                    receive();
                }
            }
        }
    }

    public void readArray(float ref[], int off, int len) throws IOException {
        try {
            floats.get(ref, off, len);
        } catch (BufferUnderflowException e) {
            // do this the hard way
            int left = len;
            int size;

            while (left > 0) {
                // copy as much as possible to the buffer
                size = Math.min(left, floats.remaining());
                floats.get(ref, off, size);
                off += size;
                left -= size;

                // if still needed, fetch some more bytes from the
                // channel
                if (left > 0) {
                    receive();
                }
            }
        }
    }

    public void readArray(double ref[], int off, int len) throws IOException {
        try {
            doubles.get(ref, off, len);
        } catch (BufferUnderflowException e) {
            // do this the hard way
            int left = len;
            int size;

            while (left > 0) {
                // copy as much as possible to the buffer
                size = Math.min(left, doubles.remaining());
                doubles.get(ref, off, size);
                off += size;
                left -= size;

                // if still needed, fetch some more bytes from the
                // channel
                if (left > 0) {
                    receive();
                }
            }
        }
    }
	
	
}
