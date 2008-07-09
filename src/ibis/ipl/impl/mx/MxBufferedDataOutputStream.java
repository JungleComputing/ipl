package ibis.ipl.impl.mx;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import org.apache.log4j.Logger;

/* Write methods are taken from NioAccumulator, some others are based on it */


public abstract class MxBufferedDataOutputStream extends MxDataOutputStream implements Config {

	//FIXME not threadsafe

	private static Logger logger = Logger.getLogger(MxBufferedDataOutputStream.class);
	
    public static final int SIZEOF_BYTE = 1;
    public static final int SIZEOF_CHAR = 2;
    public static final int SIZEOF_SHORT = 2;
    public static final int SIZEOF_INT = 4;
    public static final int SIZEOF_FLOAT = 4;
    public static final int SIZEOF_LONG = 8;
    public static final int SIZEOF_DOUBLE = 8;

    static final int INITIAL_CONNECTIONS_SIZE = 8;

    SendBuffer buffer;

    private ByteBuffer bytes;
    private CharBuffer chars;
    private ShortBuffer shorts;
    private IntBuffer ints;
    private LongBuffer longs;
    private FloatBuffer floats;
    private DoubleBuffer doubles;

	
	protected MxBufferedDataOutputStream() {
		super();
        buffer = SendBuffer.get();

        bytes = buffer.bytes;
        chars = buffer.chars;
        shorts = buffer.shorts;
        ints = buffer.ints;
        longs = buffer.longs;
        floats = buffer.floats;
        doubles = buffer.doubles;
	}

	@Override
	public int bufferSize() {
		return PRIMITIVE_BUFFER_SIZE;
	}


	protected abstract void doClose() throws IOException;

	/* (non-Javadoc)
	 * @see ibis.ipl.impl.mx.MxDataOutputStream#isEmpty()
	 */
	@Override
	protected boolean isEmpty() {
		return buffer.isEmpty();
	}

	public void writeBoolean(boolean value) throws IOException {
        if (value) {
            writeByte((byte) 1);
        } else {
            writeByte((byte) 0);
        }
    }

    public void writeByte(byte value) throws IOException {
        //logger.debug("writeByte(" + value + ")");

        try {
            bytes.put(value);
        } catch (BufferOverflowException e) {
            // buffer was full, send
        	flush();
        	finish();
            // and try again
            bytes.put(value);
        }
    }

    public void write(int value) throws IOException {
        writeByte((byte) value);
    }

    public void writeChar(char value) throws IOException {
        try {
        	chars.put(value);
        } catch (BufferOverflowException e) {
        	flush();
        	finish();
            chars.put(value);
        }
    }

    public void writeShort(short value) throws IOException {
        try {
        	shorts.put(value);
        } catch (BufferOverflowException e) {
        	flush();
        	finish();
            shorts.put(value);
        }
    }

    public void writeInt(int value) throws IOException {
        try {
            ints.put(value);
        } catch (BufferOverflowException e) {
        	flush();
        	finish();
            ints.put(value);
        }
    }

    public void writeLong(long value) throws IOException {
        try {
        	longs.put(value);
        } catch (BufferOverflowException e) {
        	flush();
        	finish();
            longs.put(value);
        }
    }

    public void writeFloat(float value) throws IOException {
        try {
        	floats.put(value);
        } catch (BufferOverflowException e) {
        	flush();
            floats.put(value);
        }
    }

    public void writeDouble(double value) throws IOException {
        try {
        	doubles.put(value);
        } catch (BufferOverflowException e) {
        	flush();
        	finish();
            doubles.put(value);
        }
    }

    public void writeArray(boolean[] array, int off, int len)
            throws IOException {
        for (int i = off; i < (off + len); i++) {
            if (array[i]) {
                writeByte((byte) 1);
            } else {
                writeByte((byte) 0);
            }
        }
    }

    public void writeArray(byte[] array, int off, int len) throws IOException {
        /*if (logger.isDebugEnabled()) {
            String message = "MxBufferedDataOutputStream.writeArray(byte[], off = " + off
                    + " len = " + len + ") Contents: ";

            for (int i = off; i < (off + len); i++) {
                message = message + array[i] + " ";
            }
            if (logger.isDebugEnabled()) {
                logger.debug(message);
            }
        }*/

        try {
        	bytes.put(array, off, len);
        } catch (BufferOverflowException e) {
            // do this the hard way

            while (len > 0) {
                if (!bytes.hasRemaining()) {
                	flush();
                	finish();
                }

                int size = Math.min(len, bytes.remaining());
                bytes.put(array, off, size);
                off += size;
                len -= size;
            }
        }
    }

    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        writeArray(b, off, len);
    }

    public void writeArray(char[] array, int off, int len) throws IOException {
        try {
            chars.put(array, off, len);
        } catch (BufferOverflowException e) {
            // do this the hard way

            while (len > 0) {
                if (!chars.hasRemaining()) {
                	flush();
                	finish();
                }

                int size = Math.min(len, chars.remaining());
                chars.put(array, off, size);
                off += size;
                len -= size;
            }
        }
    }

    public void writeArray(short[] array, int off, int len) throws IOException {
        try {
        	shorts.put(array, off, len);
        } catch (BufferOverflowException e) {
            // do this the hard way

            while (len > 0) {
                if (!shorts.hasRemaining()) {
                	flush();
                	finish();
                }

                int size = Math.min(len, shorts.remaining());
                shorts.put(array, off, size);
                off += size;
                len -= size;
            }
        }
    }

    public void writeArray(int[] array, int off, int len) throws IOException {
        try {
        	ints.put(array, off, len);
        } catch (BufferOverflowException e) {
            // do this the hard way

            while (len > 0) {
                if (!ints.hasRemaining()) {
                	flush();
                	finish();
                }

                int size = Math.min(len, ints.remaining());
                ints.put(array, off, size);
                off += size;
                len -= size;
            }
        }
    }

    public void writeArray(long[] array, int off, int len) throws IOException {
        try {
        	longs.put(array, off, len);
        } catch (BufferOverflowException e) {
            // do this the hard way

            while (len > 0) {
                if (!longs.hasRemaining()) {
                	flush();
                	finish();
                }

                int size = Math.min(len, longs.remaining());
                longs.put(array, off, size);
                off += size;
                len -= size;
            }
        }
    }

    public void writeArray(float[] array, int off, int len) throws IOException {
        try {
        	floats.put(array, off, len);
        } catch (BufferOverflowException e) {
            // do this the hard way

            while (len > 0) {
                if (!floats.hasRemaining()) {
                	flush();
                	finish();
                }

                int size = Math.min(len, floats.remaining());
                floats.put(array, off, size);
                off += size;
                len -= size;
            }
        }
    }

    public void writeArray(double[] array, int off, int len) throws IOException {
        try {
        	doubles.put(array, off, len);
        } catch (BufferOverflowException e) {
            // do this the hard way

            while (len > 0) {
                if (!doubles.hasRemaining()) {
                	flush();
                	finish();
                }

                int size = Math.min(len, doubles.remaining());
                doubles.put(array, off, size);
                off += size;
                len -= size;
            }
        }
    }
}
