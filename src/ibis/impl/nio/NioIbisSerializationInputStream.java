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
import java.nio.BufferUnderflowException;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.SelectableChannel;

import java.io.IOException;

import ibis.io.IbisSerializationInputStream;
import ibis.ipl.IbisError;


/**
 * Does "normal" Ibis serialization, but with a different output method,
 * using (Nio)Buffers instead of arrays
 */
final class NioIbisSerializationInputStream extends IbisSerializationInputStream implements Config { 
    private int HEADER = 0;
    private int BYTE = 1;
    private int CHAR = 2;
    private int SHORT = 3;
    private int INT = 4;
    private int LONG = 5;
    private int FLOAT = 6;
    private int DOUBLE = 7;

    private int NR_OF_BUFFERS = 8;

    /**
     * Buffers used to hold data which has been received. 
     * Has one header buffer, and one buffer for each primitive type
     * except Boolean
     */
    private ByteBuffer[] buffers = new ByteBuffer[NR_OF_BUFFERS];

    /*
     * used to receive how many of each primitive type were send
     */
    private ShortBuffer header;

    /*
     * Views of "buffers" used to drain them
     */
    private ByteBuffer bytes;
    private CharBuffer chars;
    private ShortBuffer shorts;
    private IntBuffer ints;
    private LongBuffer longs;
    private FloatBuffer floats;
    private DoubleBuffer doubles;

    private long count = 0;

    /**
     * The channel we use for output. Not private because NioReceivePort
     * uses it.
     */
    final ScatteringByteChannel channel;

    /**
     * Selector used to see if a read on the channel will give us someting
     */
    private Selector selector = null;

    /*
     * Byte order of the sending NioIbisSerializationOutputStream
     */
    private ByteOrder peerOrder;

    NioIbisSerializationInputStream(ScatteringByteChannel channel) 
	throws IOException {
	    super();

	    this.channel = channel;

	    //receive the byte order of the peer stream

	    ByteBuffer temp = ByteBuffer.allocate(1);

	    channel.read(temp);
	    while(temp.hasRemaining()) {
		//do a select to make sure we can receive at least one more byte
		if(selector == null) {
		    selector = Selector.open();
		    ((SelectableChannel) channel)
			.register(selector, SelectionKey.OP_READ);
		}
		selector.select();
		
		channel.read(temp);
	    }

	    switch(temp.get(0)) {
		case 0:
		    if(DEBUG_LEVEL >= LOW_DEBUG_LEVEL) {
			System.err.println("NioIbisSerializationInputStream"
				+ " using little endian buffers");
		    }
		    peerOrder = ByteOrder.LITTLE_ENDIAN;
		    break;
		case 1:
		    if(DEBUG_LEVEL >= LOW_DEBUG_LEVEL) {
			System.err.println("NioIbisSerializationInputStream"
				+ " using BIG endian buffers");
		    }
		    peerOrder = ByteOrder.BIG_ENDIAN;
		    break;
		default:
		    throw new IbisError("unknown byteorder received from peer");
	    }

	    //create buffers used to hold data

	    buffers[0] = ByteBuffer.allocateDirect(
		    (NR_OF_BUFFERS - 1) * SIZEOF_SHORT).order(peerOrder);

	    for(int i = 1; i < NR_OF_BUFFERS; i++) {
		buffers[i] = ByteBuffer.allocateDirect(Config.BUFFER_SIZE)
		    .order(peerOrder);
	    }

	    //create views of the data (and make them apear empty)
	    header = buffers[HEADER].asShortBuffer();
	    bytes = buffers[BYTE];
	    chars = buffers[CHAR].asCharBuffer();
	    shorts = buffers[SHORT].asShortBuffer();
	    ints = buffers[INT].asIntBuffer();
	    longs = buffers[LONG].asLongBuffer();
	    floats = buffers[FLOAT].asFloatBuffer();
	    doubles = buffers[DOUBLE].asDoubleBuffer();

	    bytes.limit(0);
	    chars.limit(0);
	    shorts.limit(0);
	    ints.limit(0);
	    longs.limit(0);
	    floats.limit(0);
	    doubles.limit(0);

	}

    /**
     * returns number of bytes read from the channel - the number of bytes
     * in the buffer. Equals the number of bytes given to the user
     */
    long getCount() {
	return count - (chars.remaining() * SIZEOF_CHAR)
	    - (shorts.remaining() * SIZEOF_SHORT)
	    - (ints.remaining() * SIZEOF_INT)
	    - (longs.remaining() * SIZEOF_LONG)
	    - (floats.remaining() * SIZEOF_FLOAT)
	    - (doubles.remaining() * SIZEOF_DOUBLE);
    }

    void resetCount() {
	count = 0;
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
    public int available() throws IOException {
	return bytes.remaining()
	    + (chars.remaining() * SIZEOF_CHAR)
	    + (shorts.remaining() * SIZEOF_SHORT) 
	    + (ints.remaining() * SIZEOF_INT) 
	    + (longs.remaining() * SIZEOF_LONG) 
	    + (floats.remaining() * SIZEOF_FLOAT) 
	    + (doubles.remaining() * SIZEOF_DOUBLE);
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws IOException {
	if(available() > 0) {
	    throw new IOException("serializationstream closed with data still"
		    + " left in the buffer");
	}
    }

    private void receive() throws IOException {
	ByteBuffer lastBuffer;

	int selects = 0;

	if(ASSERT) {
	    if(bytes.hasRemaining() ||
		    chars.hasRemaining() ||
		    shorts.hasRemaining() ||
		    ints.hasRemaining() ||
		    longs.hasRemaining() ||
		    floats.hasRemaining() ||
		    doubles.hasRemaining() ) {
		if(DEBUG_LEVEL >= ERROR_DEBUG_LEVEL) {
		    System.err.println("data still in buffer while doing"
			    + " a receive: b[" + bytes.remaining()
			    + "] c[" + chars.remaining()
			    + "] s[" + shorts.remaining()
			    + "] i[" + ints.remaining()
			    + "] l[" + longs.remaining()
			    + "] f[" + floats.remaining()
			    + "] d[" + doubles.remaining()
			    + "]");
		}
		throw new IbisError("Trying to do a receive while still data"
			+ " left in a buffer");
	    }
	}

	//receive header

	buffers[HEADER].clear();
	
	if(DEBUG_LEVEL >= RIDICULOUSLY_HIGH_DEBUG_LEVEL) {
	    System.err.println("reading header from channel");
	}

	count += channel.read(buffers[HEADER]);
	while(buffers[HEADER].hasRemaining()) {
	    //do a select to make sure we can receive at least one more byte
	    if(selector == null) {
		selector = Selector.open();
		((SelectableChannel) channel)
		    .register(selector, SelectionKey.OP_READ);
	    }
	    selector.select();

	    if (DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
		selects++;
	    }

	    count += channel.read(buffers[HEADER]);
	}

	header.position(0).limit(buffers[HEADER].position() / SIZEOF_SHORT);

	//set up primitive buffers so they can be filled
	buffers[BYTE].clear().limit(header.get());
	buffers[CHAR].clear().limit(header.get());
	buffers[SHORT].clear().limit(header.get());
	buffers[INT].clear().limit(header.get());
	buffers[LONG].clear().limit(header.get());
	buffers[FLOAT].clear().limit(header.get());
	buffers[DOUBLE].clear().limit(header.get());

	//find the last buffer that needs to be filled
	lastBuffer = buffers[0];
	for(int i = 1; i < NR_OF_BUFFERS; i++) {
	    if(buffers[i].hasRemaining()) {
		lastBuffer = buffers[i];
	    }
	}

	if(DEBUG_LEVEL >= RIDICULOUSLY_HIGH_DEBUG_LEVEL) {
	    System.err.println("getting: b[" + buffers[BYTE].remaining()
		+ "] c[" + buffers[CHAR].remaining()
		+ "] s[" + buffers[SHORT].remaining()
		+ "] i[" + buffers[INT].remaining()
		+ "] l[" + buffers[LONG].remaining()
		+ "] f[" + buffers[FLOAT].remaining()
		+ "] d[" + buffers[DOUBLE].remaining()
		+ "]");
	}

	//read the data from the channel
	count += channel.read(buffers, 1, NR_OF_BUFFERS - 1);
	while(lastBuffer.hasRemaining()) {
	    //do a select to make sure we can receive at least one more byte
	    if(selector == null) {
		selector = Selector.open();
		((SelectableChannel) channel)
		    .register(selector, SelectionKey.OP_READ);
	    }
	    selector.select();

	    if (DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
		selects++;
	    }

	    count += channel.read(buffers, 1, NR_OF_BUFFERS - 1);
	}

	//set the buffers up so they can be drained
	bytes.flip();
	chars.position(0).limit(buffers[CHAR].position() / SIZEOF_CHAR);
	shorts.position(0).limit(buffers[SHORT].position() / SIZEOF_SHORT);
	ints.position(0).limit(buffers[INT].position() / SIZEOF_INT);
	longs.position(0).limit(buffers[LONG].position() / SIZEOF_LONG);
	floats.position(0).limit(buffers[FLOAT].position() / SIZEOF_FLOAT);
	doubles.position(0).limit(buffers[DOUBLE].position() / SIZEOF_DOUBLE);

	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("received: b[" + bytes.remaining()
		    + "] c[" + chars.remaining()
		    + "] s[" + shorts.remaining()
		    + "] i[" + ints.remaining()
		    + "] l[" + longs.remaining()
		    + "] f[" + floats.remaining()
		    + "] d[" + doubles.remaining()
		    + "], did " + selects + " selects");
	}

    }

    /**
     * {@inheritDoc}
     */
    public boolean readBoolean() throws IOException {
	return (readByte() == ((byte) 1));
    }

    /**
     * {@inheritDoc}
     */
    public byte readByte() throws IOException {
	try {
	    return bytes.get();
	} catch (BufferUnderflowException e) {
	    receive();
	    return bytes.get();
	}
    }

    /**
     * {@inheritDoc}
     */
    public char readChar() throws IOException {
	try {
	    return chars.get();
	} catch (BufferUnderflowException e) {
	    receive();
	    return chars.get();
	}
    }

    /**
     * {@inheritDoc}
     */
    public short readShort() throws IOException {
	try {
	    return shorts.get();
	} catch (BufferUnderflowException e) {
	    receive();
	    return shorts.get();
	}
    }

    /**
     * {@inheritDoc}
     */
    public int readInt() throws IOException {
	try {
	    return ints.get();
	} catch (BufferUnderflowException e) {
	    receive();
	    return ints.get();
	}
    }

    /**
     * {@inheritDoc}
     */
    public long readLong() throws IOException {
	try {
	    return longs.get();
	} catch (BufferUnderflowException e) {
	    receive();
	    return longs.get();
	}
    }

    /**
     * {@inheritDoc}
     */
    public float readFloat() throws IOException {
	try {
	    return floats.get();
	} catch (BufferUnderflowException e) {
	    receive();
	    return floats.get();
	}
    }

    /**
     * {@inheritDoc}
     */
    public double readDouble() throws IOException {
	try {
	    return doubles.get();
	} catch (BufferUnderflowException e) {
	    receive();
	    return doubles.get();
	}
    }

    /**
     * {@inheritDoc}
     */
    protected void readBooleanArray(boolean ref[], int off, int len) throws IOException {
	for(int i = off; i < (off + len); i++) {
	    ref[i] = ((readByte() == (byte) 1) ? true : false) ; 
	}
    }


    /**
     * {@inheritDoc}
     */
    protected void readByteArray(byte ref[], int off, int len) throws IOException {
	try {
	    bytes.get(ref, off, len);
	} catch (BufferUnderflowException e) {
	    //do this the hard way
	    int left = len;
	    int size;

	    while(left > 0) {
		//copy as much as possible to the buffer
		size = Math.min(left, bytes.remaining());
		bytes.get(ref, off, size);
		off += size;
		left -= size;

		// if still needed, fetch some more bytes from the
		// channel
		if(left > 0) {
		    receive();
		}
	    }
	}
    }

    /**
     * {@inheritDoc}
     */
    protected void readCharArray(char ref[], int off, int len) throws IOException {
	try {
	    chars.get(ref, off, len);
	} catch (BufferUnderflowException e) {
	    //do this the hard way
	    int left = len;
	    int size;

	    while(left > 0) {
		//copy as much as possible to the buffer
		size = Math.min(left, chars.remaining());
		chars.get(ref, off, size);
		off += size;
		left -= size;

		// if still needed, fetch some more bytes from the
		// channel
		if(left > 0) {
		    receive();
		}
	    }
	}
    }

    /**
     * {@inheritDoc}
     */
    protected void readShortArray(short ref[], int off, int len) throws IOException {
	try {
	    shorts.get(ref, off, len);
	} catch (BufferUnderflowException e) {
	    //do this the hard way
	    int left = len;
	    int size;

	    while(left > 0) {
		//copy as much as possible to the buffer
		size = Math.min(left, shorts.remaining());
		shorts.get(ref, off, size);
		off += size;
		left -= size;

		// if still needed, fetch some more bytes from the
		// channel
		if(left > 0) {
		    receive();
		}
	    }
	}
    }

    /**
     * {@inheritDoc}
     */
    protected void readIntArray(int ref[], int off, int len) throws IOException {
	try {
	    ints.get(ref, off, len);
	} catch (BufferUnderflowException e) {
	    //do this the hard way
	    int left = len;
	    int size;

	    while(left > 0) {
		//copy as much as possible to the buffer
		size = Math.min(left, ints.remaining());
		ints.get(ref, off, size);
		off += size;
		left -= size;

		// if still needed, fetch some more bytes from the
		// channel
		if(left > 0) {
		    receive();
		}
	    }
	}
    }

    /**
     * {@inheritDoc}
     */
    protected void readLongArray(long ref[], int off, int len) throws IOException {
	try {
	    longs.get(ref, off, len);
	} catch (BufferUnderflowException e) {
	    //do this the hard way
	    int left = len;
	    int size;

	    while(left > 0) {
		//copy as much as possible to the buffer
		size = Math.min(left, longs.remaining());
		longs.get(ref, off, size);
		off += size;
		left -= size;

		// if still needed, fetch some more bytes from the
		// channel
		if(left > 0) {
		    receive();
		}
	    }
	}
    }

    /**
     * {@inheritDoc}
     */
    protected void readFloatArray(float ref[], int off, int len) throws IOException {
	try {
	    floats.get(ref, off, len);
	} catch (BufferUnderflowException e) {
	    //do this the hard way
	    int left = len;
	    int size;

	    while(left > 0) {
		//copy as much as possible to the buffer
		size = Math.min(left, floats.remaining());
		floats.get(ref, off, size);
		off += size;
		left -= size;

		// if still needed, fetch some more bytes from the
		// channel
		if(left > 0) {
		    receive();
		}
	    }
	}
    }

    /**
     * {@inheritDoc}
     */
    protected void readDoubleArray(double ref[], int off, int len) throws IOException {
	try {
	    doubles.get(ref, off, len);
	} catch (BufferUnderflowException e) {
	    //do this the hard way
	    int left = len;
	    int size;

	    while(left > 0) {
		//copy as much as possible to the buffer
		size = Math.min(left, doubles.remaining());
		doubles.get(ref, off, size);
		off += size;
		left -= size;

		// if still needed, fetch some more bytes from the
		// channel
		if(left > 0) {
		    receive();
		}
	    }
	}
    }


}
