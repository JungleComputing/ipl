package ibis.impl.nio;

import java.io.IOException;

import java.lang.Math;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.ShortBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.FloatBuffer;
import java.nio.DoubleBuffer;
import java.nio.ByteOrder;
import java.nio.BufferOverflowException;

import ibis.io.IbisSerializationOutputStream;
import ibis.ipl.IbisError;


/**
 * Does "normal" Ibis serialization, but with a different output method, 
 * using (Nio)Buffers instead of arrays
 */
final class NioIbisSerializationOutputStream 
		    extends IbisSerializationOutputStream implements Config {

    private int HEADER = 0;
    private int BYTE = 1;
    private int CHAR = 2;
    private int SHORT = 3;
    private int INT = 4;
    private int LONG = 5;
    private int FLOAT = 6;
    private int DOUBLE = 7;

    private int NR_OF_BUFFERS = 8;

    private NioChannelSplitter out;

    /**
     * Buffers used to hold data which is to be send.
     * Has one header buffer, and one buffer for each primitive type
     * except Boolean
     */
    private ByteBuffer[] buffers = new ByteBuffer[NR_OF_BUFFERS];

    /*
     * used to send how many of each primitive type are send
     */
    private ShortBuffer header;

    /*
     * Views of "buffers" used to fill it
     */
    private ByteBuffer bytes;
    private CharBuffer chars;
    private ShortBuffer shorts;
    private IntBuffer ints;
    private LongBuffer longs;
    private FloatBuffer floats;
    private DoubleBuffer doubles;

    NioIbisSerializationOutputStream(NioChannelSplitter out) 
							throws IOException {
	super();

	this.out = out;

	ByteOrder order = ByteOrder.nativeOrder();

	// make buffer to store data in.
	buffers[0] = ByteBuffer.
	    allocateDirect((NR_OF_BUFFERS - 1) * SIZEOF_SHORT).order(order);

	for(int i = 1; i < NR_OF_BUFFERS; i++) {
	    buffers[i] = ByteBuffer.allocateDirect(Config.BUFFER_SIZE)
						.order(order);
	}

	header = buffers[HEADER].asShortBuffer();
	bytes = buffers[BYTE];
	chars = buffers[CHAR].asCharBuffer();
	shorts = buffers[SHORT].asShortBuffer();
	ints = buffers[INT].asIntBuffer();
	longs = buffers[LONG].asLongBuffer();
	floats = buffers[FLOAT].asFloatBuffer();
	doubles = buffers[DOUBLE].asDoubleBuffer();

	//send our byte order to the inputstream on the other side
	bytes.clear();
	if(order == ByteOrder.BIG_ENDIAN) {
	    bytes.put((byte) 1);
	} else {
	    bytes.put((byte) 0);
	}
	bytes.flip();
	out.write(bytes);
	bytes.clear();
    }

    /**
     * {@inheritDoc}
     */
    public String serializationImplName() {
	return "nio-ibis";
    }

    /**
     * {@inheritDoc}
     */
    public void writeArray(Object ref, int offset, int len, int type)
							throws IOException {
	switch(type) {
	    case TYPE_BOOLEAN:
		writeBooleanArray((boolean[]) ref, offset, len);
		break;
	    case TYPE_BYTE:
		writeByteArray((byte[]) ref, offset, len);
		break;
	    case TYPE_CHAR:
		writeCharArray((char[]) ref, offset, len);
		break;
	    case TYPE_SHORT:
		writeShortArray((short[]) ref, offset, len);
		break;
	    case TYPE_INT:
		writeIntArray((int[]) ref, offset, len);
		break;
	    case TYPE_LONG:
		writeLongArray((long[]) ref, offset, len);
		break;
	    case TYPE_FLOAT:
		writeFloatArray((float[]) ref, offset, len);
		break;
	    case TYPE_DOUBLE:
		writeDoubleArray((double[]) ref, offset, len);
		break;
	    default:
		throw new IbisError("unknown array type");
	}
    }

    /**
     * Flushes all the data from the buffers to the underlying channel(s).
     */
    public void flush() throws IOException {
	if(bytes.position() + chars.position() + shorts.position()
	   + ints.position() + longs.position() + floats.position()
	   + doubles.position() == 0) {
	    if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
		System.err.println("not doing a flush since it's not needed");
	    }
	    return;
	}

	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("sending: b[" + bytes.position()
				    + "] c[" + chars.position()
				    + "] s[" + shorts.position()
				    + "] i[" + ints.position()
				    + "] l[" + longs.position()
				    + "] f[" + floats.position()
				    + "] d[" + doubles.position()
				    + "]");
	}

	//fill header with the number of BYTES of each type we are going to send
	header.clear();
	header.put((short) bytes.position());
	header.put((short) (chars.position() * SIZEOF_CHAR));
	header.put((short) (shorts.position() * SIZEOF_SHORT));
	header.put((short) (ints.position() * SIZEOF_INT));
	header.put((short) (longs.position() * SIZEOF_LONG));
	header.put((short) (floats.position() * SIZEOF_FLOAT));
	header.put((short) (doubles.position() * SIZEOF_DOUBLE));
	buffers[HEADER].limit(header.position() * SIZEOF_SHORT).position(0);

	//set up primitive buffers so they can be send
	bytes.flip();
	buffers[CHAR].limit(chars.position() * SIZEOF_CHAR).position(0);
	buffers[SHORT].limit(shorts.position() * SIZEOF_SHORT).position(0);
	buffers[INT].limit(ints.position() * SIZEOF_INT).position(0);
	buffers[LONG].limit(longs.position() * SIZEOF_LONG).position(0);
	buffers[FLOAT].limit(floats.position() * SIZEOF_FLOAT).position(0);
	buffers[DOUBLE].limit(doubles.position() * SIZEOF_DOUBLE).position(0);

	//write the buffer to the splitter (will block untill it's all gone)
	out.write(buffers);

	//clear out the buffers so they can be filled again
	bytes.clear();
	chars.clear();
	shorts.clear();
	ints.clear();
	longs.clear();
	floats.clear();
	doubles.clear();
    }

    private void writeBooleanArray(boolean[] array, int off, int len) 
						    throws IOException {
	for(int i = off; i < (off + len); i++) {
	    if(array[i]) {
		writeByte(1);
	    } else {
		writeByte(0);
	    }
	}
    }

    private void writeByteArray(byte[] array, int off, int len)
						    throws IOException {
	try {
	    bytes.put(array, off, len);
	} catch (BufferOverflowException e) {
	    //do this the hard way

	    while(len > 0) {
		if(!bytes.hasRemaining()) {
		    flush();
		}

		int size = Math.min(len, bytes.remaining());
		bytes.put(array, off, size);
		off += size;
		len -= size;
	    }
	}
    }

    private void writeCharArray(char[] array, int off, int len)
						    throws IOException {
	try {
	    chars.put(array, off, len);
	} catch (BufferOverflowException e) {
	    //do this the hard way

	    while(len > 0) {
		if(!chars.hasRemaining()) {
		    flush();
		}

		int size = Math.min(len, chars.remaining());
		chars.put(array, off, size);
		off += size;
		len -= size;
	    }
	}
    }

    private void writeShortArray(short[] array, int off, int len)
						    throws IOException {
	try {
	    shorts.put(array, off, len);
	} catch (BufferOverflowException e) {
	    //do this the hard way

	    while(len > 0) {
		if(!shorts.hasRemaining()) {
		    flush();
		}

		int size = Math.min(len, shorts.remaining());
		shorts.put(array, off, size);
		off += size;
		len -= size;
	    }
	}
    }

    private void writeIntArray(int[] array, int off, int len)
						    throws IOException {
	try {
	    ints.put(array, off, len);
	} catch (BufferOverflowException e) {
	    //do this the hard way

	    while(len > 0) {
		if(!ints.hasRemaining()) {
		    flush();
		}

		int size = Math.min(len, ints.remaining());
		ints.put(array, off, size);
		off += size;
		len -= size;
	    }
	}
    }

    private void writeLongArray(long[] array, int off, int len)
						    throws IOException {
	try {
	    longs.put(array, off, len);
	} catch (BufferOverflowException e) {
	    //do this the hard way

	    while(len > 0) {
		if(!longs.hasRemaining()) {
		    flush();
		}

		int size = Math.min(len, longs.remaining());
		longs.put(array, off, size);
		off += size;
		len -= size;
	    }
	}
    }

    private void writeFloatArray(float[] array, int off, int len)
						    throws IOException {
	try {
	    floats.put(array, off, len);
	} catch (BufferOverflowException e) {
	    //do this the hard way

	    while(len > 0) {
		if(!floats.hasRemaining()) {
		    flush();
		}

		int size = Math.min(len, floats.remaining());
		floats.put(array, off, size);
		off += size;
		len -= size;
	    }
	}
    }

    private void writeDoubleArray(double[] array, int off, int len)
						    throws IOException {
	try {
	    doubles.put(array, off, len);
	} catch (BufferOverflowException e) {
	    //do this the hard way

	    while(len > 0) {
		if(!doubles.hasRemaining()) {
		    flush();
		}

		int size = Math.min(len, doubles.remaining());
		doubles.put(array, off, size);
		off += size;
		len -= size;
	    }
	}
    }

    /**
     * {@inheritDoc}
     */
    public void writeBoolean(boolean value) throws IOException {
	if(value) {
	    writeByte(1);
	} else {
	    writeByte(0);
	}
    }

    /**
     * {@inheritDoc}
     */
    public void writeByte(int value) throws IOException {
	try {
	    bytes.put((byte) value);
	} catch (BufferOverflowException e) {
	    //buffer was full, flush
	    flush();
	    //and try again
	    bytes.put((byte) value); 
	}
    }

    /**
     * {@inheritDoc}
     */
    public void writeChar(char value) throws IOException {
	try {
	    chars.put(value);
	} catch (BufferOverflowException e) {
	    flush();
	    chars.put(value);
	}
    }

    /**
     * {@inheritDoc}
     */
    public void writeShort(int value) throws IOException {
	try {
	    shorts.put((short) value);
	} catch (BufferOverflowException e) {
	    flush();
	    shorts.put((short) value);
	}
    }

    /**
     * {@inheritDoc}
     */
    public void writeInt(int value) throws IOException {
	try {
	    ints.put(value);
	} catch (BufferOverflowException e) {
	    flush();
	    ints.put(value);
	}
    }

    /**
     * {@inheritDoc}
     */
    public void writeLong(long value) throws IOException {
	try {
	    longs.put(value);
	} catch (BufferOverflowException e) {
	    flush();
	    longs.put(value);
	}
    }

    /**
     * {@inheritDoc}
     */
    public void writeFloat(float value) throws IOException {
	try {
	    floats.put(value);
	} catch (BufferOverflowException e) {
	    flush();
	    floats.put(value);
	}
    }

    /**
     * {@inheritDoc}
     */
    public void writeDouble(double value) throws IOException {
	try {
	    doubles.put(value);
	} catch (BufferOverflowException e) {
	    flush();
	    doubles.put(value);
	}
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws IOException {
	flush();
	out.close();
    }

}
