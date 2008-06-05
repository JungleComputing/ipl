package ibis.ipl.impl.mx;

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

import org.apache.log4j.Logger;

import ibis.io.DataInputStream;

/**
 * @author Timo van Kessel
 * 
 */
/**
 * @author Timo van Kessel
 *
 */
public class MxDataInputStream extends DataInputStream implements Config {

    public static final int SIZEOF_BYTE = 1;
    public static final int SIZEOF_CHAR = 2;
    public static final int SIZEOF_SHORT = 2;
    public static final int SIZEOF_INT = 4;
    public static final int SIZEOF_FLOAT = 4;
    public static final int SIZEOF_DOUBLE = 8;
    public static final int SIZEOF_LONG = 8;
	
	private static final int LONGS = 1;
	private static final int DOUBLES = 2;
	private static final int INTS = 3;
	private static final int FLOATS = 4;
	private static final int SHORTS = 5;
	private static final int CHARS = 6;
	private static final int BYTES = 7;
	protected static final int SIZEOF_HEADER = 16; // in bytes

	private static Logger logger = Logger.getLogger(MxDataInputStream.class);
	
	MxReadChannel channel;
	long count;

	private ByteBuffer buffer;

	private ShortBuffer header;
	private LongBuffer longs;
	private DoubleBuffer doubles;
	private FloatBuffer floats;
	private IntBuffer ints;
	private ShortBuffer shorts;
	private CharBuffer chars;
	private ByteBuffer bytes;
	
	private boolean closed = false;
	
	/**
	 * @param channel the data source
	 */
	public MxDataInputStream(MxReadChannel channel, ByteOrder order) {
		this.channel = channel;
		count = 0;
		buffer = ByteBuffer.allocateDirect(BYTE_BUFFER_SIZE).order(order);
		initBuffers();
	}

	/**
	 * initializes the buffers
	 */
	private void initBuffers() {
		buffer.clear();
		// setup the header
		buffer.limit(SIZEOF_HEADER).position(0);
		header = buffer.asShortBuffer();
		// setup the other buffers
		buffer.limit(buffer.capacity()).position(SIZEOF_HEADER);
		longs = buffer.asLongBuffer();
		doubles = buffer.asDoubleBuffer();
		floats = buffer.asFloatBuffer();
		ints = buffer.asIntBuffer();
		shorts = buffer.asShortBuffer();
		chars = buffer.asCharBuffer();
		bytes = buffer.duplicate();
		
		buffer.clear();
	}

	@Override
	public int bufferSize() {
		// TODO What about this one? For what is it used?
		return -1;
	}

	@Override
	public long bytesRead() {
		return count;
	}

	@Override
	public void close() throws IOException {
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
        byte result;

        try {
            result = bytes.get();
        } catch (BufferUnderflowException e) {
            receive();
            result = bytes.get();
        }

        if (logger.isDebugEnabled()) {
            logger.debug("received byte: " + result);
        }

        return result;
    }

	public char readChar() throws IOException {
        try {
            return chars.get();
        } catch (BufferUnderflowException e) {
            receive();
            return chars.get();
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

    public float readFloat() throws IOException {
        try {
            return floats.get();
        } catch (BufferUnderflowException e) {
            receive();
            return floats.get();
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

    public short readShort() throws IOException {
        try {
            return shorts.get();
        } catch (BufferUnderflowException e) {
            receive();
            return shorts.get();
        }
    }

	
	public void readArray(boolean[] destination, int offset, int length)
			throws IOException {
        for (int i = offset; i < (offset + length); i++) {
        	destination[i] = ((readByte() == (byte) 1) ? true : false);
        }
	}

	public void readArray(byte[] destination, int offset, int length)
			throws IOException {
        try {
            bytes.get(destination, offset, length);
        } catch (BufferUnderflowException e) {
            // do this the hard way
            int left = length;
            int off = offset;
            int size;

            while (left > 0) {
                // copy as much as possible to the buffer
                size = Math.min(left, bytes.remaining());
                bytes.get(destination, off, size);
                off += size;
                left -= size;

                // if still needed, fetch some more bytes from the
                // channel
                if (left > 0) {
                    receive();
                }
            }
        }
        if (logger.isDebugEnabled()) {
            String message = "received byte[], Contents: ";
            for (int i = offset; i < (offset + length); i++) {
                message = message + destination[i] + " ";
            }

            logger.debug(message);
        }
	}

	public void readArray(char[] destination, int offset, int length)
			throws IOException {
        try {
            chars.get(destination, offset, length);
        } catch (BufferUnderflowException e) {
            // do this the hard way
            int left = length;
            int off = offset;
            int size;

            while (left > 0) {
                // copy as much as possible to the buffer
                size = Math.min(left, bytes.remaining());
                chars.get(destination, off, size);
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

	public void readArray(short[] destination, int offset, int length)
			throws IOException {
        try {
            shorts.get(destination, offset, length);
        } catch (BufferUnderflowException e) {
            // do this the hard way
            int left = length;
            int off = offset;
            int size;

            while (left > 0) {
                // copy as much as possible to the buffer
                size = Math.min(left, bytes.remaining());
                shorts.get(destination, off, size);
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

	public void readArray(int[] destination, int offset, int length)
			throws IOException {
        try {
            ints.get(destination, offset, length);
        } catch (BufferUnderflowException e) {
            // do this the hard way
            int left = length;
            int off = offset;
            int size;

            while (left > 0) {
                // copy as much as possible to the buffer
                size = Math.min(left, bytes.remaining());
                ints.get(destination, off, size);
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

	public void readArray(long[] destination, int offset, int length)
			throws IOException {
        try {
            longs.get(destination, offset, length);
        } catch (BufferUnderflowException e) {
            // do this the hard way
            int left = length;
            int off = offset;
            int size;

            while (left > 0) {
                // copy as much as possible to the buffer
                size = Math.min(left, bytes.remaining());
                longs.get(destination, off, size);
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

	public void readArray(float[] destination, int offset, int length)
			throws IOException {
        try {
            floats.get(destination, offset, length);
        } catch (BufferUnderflowException e) {
            // do this the hard way
            int left = length;
            int off = offset;
            int size;

            while (left > 0) {
                // copy as much as possible to the buffer
                size = Math.min(left, bytes.remaining());
                floats.get(destination, off, size);
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

	public void readArray(double[] destination, int offset, int length)
			throws IOException {
        try {
            doubles.get(destination, offset, length);
        } catch (BufferUnderflowException e) {
            // do this the hard way
            int left = length;
            int off = offset;
            int size;

            while (left > 0) {
                // copy as much as possible to the buffer
                size = Math.min(left, bytes.remaining());
                doubles.get(destination, off, size);
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

	/**
	 * receives a packet from the channel in the buffer.
	 * @throws IOException 
	 */
	private void receive() throws IOException {
		
		if(remaining() > 0) {
			// ERROR still data left in buffers
			throw new IOException("still data left in buffers");
		}
		// receive the next message
		buffer.clear();
		int count = channel.read(buffer);
		if (count <= 0) {
			// TODO error
		}
		this.count += count;
		
		// now fill the buffers
		buffer.flip();
		header.rewind();
		
		header.get(LONGS);
		buffer.position(SIZEOF_HEADER);
		int start = 0;
		
		start += setView(longs, start, header.get(LONGS), SIZEOF_LONG);
		start += setView(doubles, start, header.get(DOUBLES), SIZEOF_DOUBLE);
		start += setView(floats, start, header.get(FLOATS), SIZEOF_FLOAT);
		start += setView(ints, start, header.get(INTS), SIZEOF_INT);
		start += setView(shorts, start, header.get(SHORTS), SIZEOF_SHORT);
		start += setView(chars, start, header.get(CHARS), SIZEOF_CHAR);
		start += setView(bytes, start, header.get(BYTES), SIZEOF_BYTE);
		
		// sanity check:	
		if(start != buffer.limit()) {
			// TODO error handling
			// throw an IOException?
			System.err.println("error in MxDataInputStream.receive()");
			System.exit(1);
		}
	}

	/**
	 * @param longs2
	 * @param next
	 * @param s
	 * @param sizeofLong
	 * @return The size of the view in bytes
	 */
	private int setView(Buffer view, int start, int bytes, int dataSize) {
		view.limit((start+bytes)/dataSize);
		view.position(start/dataSize);
		return bytes;
	}

	/**
	 * Copy from NioDissipator
	 * @return
	 */
    int remaining() {
        return ((longs.remaining() * SIZEOF_LONG)
                + (doubles.remaining() * SIZEOF_DOUBLE)
                + (ints.remaining() * SIZEOF_INT)
                + (floats.remaining() * SIZEOF_INT)
                + (shorts.remaining() * SIZEOF_SHORT)
                + (chars.remaining() * SIZEOF_CHAR)
                + (bytes.remaining() * SIZEOF_BYTE));
    }
	
}
