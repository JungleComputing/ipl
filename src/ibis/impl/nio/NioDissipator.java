package ibis.impl.nio;

import ibis.io.Dissipator;
import ibis.io.SerializationInputStream;
import ibis.ipl.IbisError;

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
import java.nio.channels.ReadableByteChannel;

/**
 * Reads data into a single bytebuffer, and creates views of it to
 * drain it. Depends upon a subclass to do the actual reading from somewhere
 */
public abstract class NioDissipator extends Dissipator 
				    implements Config, Protocol {
    public static final int SIZEOF_BYTE = 1;
    public static final int SIZEOF_CHAR = 2;
    public static final int SIZEOF_SHORT = 2;
    public static final int SIZEOF_INT = 4;
    public static final int SIZEOF_LONG = 8;
    public static final int SIZEOF_FLOAT = 4;
    public static final int SIZEOF_DOUBLE = 8;

    //maximum number of bytes we want to put in the receivebuffer. The extra
    //space we use if a view happens to be split up between the send and
    //the beginning of the buffer. We can then copy the wrapped part to the
    //"hidden space" at the end.
    protected static final int BUFFER_LIMIT = BYTE_BUFFER_SIZE 
					- PRIMITIVE_BUFFER_SIZE;

    private static final int LONGS = 1;
    private static final int DOUBLES = 2;
    private static final int INTS = 3;
    private static final int FLOATS = 4;
    private static final int SHORTS = 5;
    private static final int CHARS = 6;
    private static final int BYTES = 7;

    protected static final int SIZEOF_HEADER = 16;

    /**
     * Circular buffer used for holding data. It contains "currently in use"
     * (by the user) data, "not yet used" data (received nut not given to user
     * yet) and "empty space"
     */
    protected ByteBuffer buffer;

    /**
     * Buffer with the same backing-store as the buffer, used for copying data
     * from one place to another in the buffer
     */
    private ByteBuffer copyFromBuffer;
    private ByteBuffer copyToBuffer;

    // variables used to keep track of the state of the "buffer"
    // They abide by the following equasion:
    // usedPosition <= usedLimit <= buffer.position() ( % BUFFER_LIMIT !)

    protected int usedPosition = 0; //first byte of in-use data
    protected int usedLimit = 0; //fist byte of not-used data (or empty space)

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

    SerializationInputStream sis = null;
    NioSendPortIdentifier peer;
    NioReceivePortIdentifier rpi;
    ReadableByteChannel channel;
    NioPortType type;

    protected NioDissipator(NioSendPortIdentifier peer, 
			    NioReceivePortIdentifier rpi,
			    ReadableByteChannel channel,
			    NioPortType type) throws IOException {
	this.peer = peer;
	this.rpi = rpi;
	this.channel = channel;
	this.type = type;

	order = ByteOrder.BIG_ENDIAN;

	buffer = ByteBuffer.allocateDirect(BYTE_BUFFER_SIZE);
	copyFromBuffer = buffer.duplicate();
	copyToBuffer = buffer.duplicate();
	buffer.limit(BUFFER_LIMIT);

	initViews(order);

	//make the views appear empty
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
	int position;
	int limit;

	if(DEBUG) {
	    Debug.message("buffers", this, "initializing views in " + order 
			       + " byte order");
	}

	//remember position and limit;
	position = buffer.position();
	limit = buffer.limit();

	buffer.order(order);

	//clear so views will be set correctly
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

    /**
     * Sets a view correctly.
     *
     * All received data is garanteed to be alligned since we always send
     * buffers with ((size % MAX_DATA_SIZE) == 0)
     */
    private int setView(Buffer view, int start, int bytes, int dataSize) {
	int result = (start + bytes) % BUFFER_LIMIT;

	if (result < start) {
	    //it wrapped, copy data from the start to the end of the buffer
	    copyFromBuffer.position(0);
	    copyFromBuffer.limit(result);
	    copyToBuffer.position(BUFFER_LIMIT);
	    copyToBuffer.limit(start + bytes);
	    copyToBuffer.put(copyFromBuffer);
	}

	view.limit((start + bytes) / dataSize);
	view.position(start / dataSize);

	if(DEBUG) {
	    Debug.message("buffers", this, "setView: set view: position("
		+ view.position() + ") limit(" + view.limit()
		+ "), in bytes: position(" + (view.position() * dataSize)
		+ ") limit(" + (view.limit() * dataSize) + ")");
	}

	return result;
    }

    /**
     * Returns the number of bytes available in the receivebuffer
     * which haven't been "claimed" for user data yet
     */
    int unUsedLength() {
	if (buffer.position() >= usedLimit) {
	    return buffer.position() - usedLimit;
	} else {
	    return buffer.position() + (BUFFER_LIMIT - usedLimit);
	}
    }

    /**
     * Returns the nuber of bytes "claimed" for user data
     */
    int usedLength() {
	if (usedLimit >= usedPosition) {
	    return usedPosition - usedLimit;
	} else {
	    return usedLimit + (BUFFER_LIMIT - usedPosition);
	}
    }

    void receive() throws IOException {
	ByteOrder receivedOrder;
	int next;
	int totalSize;
	int paddingLength;
	short[] headerArray = new short[SIZEOF_HEADER / SIZEOF_SHORT];

	if (DEBUG) {
	    Debug.enter("buffers", this, "receiving buffer");
	}

	if(ASSERT && (remaining() > 0)) {
	    if(DEBUG) {
		Debug.exit("buffers", this, 
			"!receiving with data still left in the buffer"
			+ ", content: "
			+ "l[" + longs.remaining()
			+ "] d[" + doubles.remaining()
			+ "] i[" + ints.remaining()
			+ "] f[" + floats.remaining()
			+ "] s[" + shorts.remaining()
			+ "] c[" + chars.remaining()
			+ "] b[" + bytes.remaining()
			+ "]");
	    }
	    throw new IbisError("tried receive() while there was data"
		    + " left in the buffer");
	}

	//release old used data 
	usedPosition = usedLimit;	

	//remember we can use the newly freed space to put data in
	if (buffer.limit() != BUFFER_LIMIT) {
	    if (buffer.position() < usedPosition) {
		buffer.limit(usedPosition - 1);
	    } else if (buffer.position() >= usedLimit) {
		buffer.limit(BUFFER_LIMIT);
	    }
	}

	if (DEBUG) {
	    Debug.message("buffers", this, "usedPosition = " + usedPosition
		    + " buffer.position(" + buffer.position() 
		    + ") buffer.limit(" + buffer.limit() + ")");
	}

	if(unUsedLength() < SIZEOF_HEADER) {
	    fillBuffer(SIZEOF_HEADER);
	}


	bytes.clear();
	//extract padding length
	paddingLength = (int) bytes.get(usedPosition + 1);

	//get byte order out of first byte in header
	if(bytes.get(usedPosition) == ((byte) 1)) {
	    receivedOrder = ByteOrder.BIG_ENDIAN;
	} else {
	    receivedOrder = ByteOrder.LITTLE_ENDIAN;
	}
	if(order != receivedOrder) {
	    //our buffers are in the wrong order, re-initialize
	    order = receivedOrder;
	    initViews(order);
	}

	next = setView(header, usedPosition, SIZEOF_HEADER, SIZEOF_SHORT);
	//extract header
	header.get(headerArray);

	totalSize = SIZEOF_HEADER + headerArray[LONGS] + headerArray[DOUBLES]
	    + headerArray[INTS] + headerArray[FLOATS] + headerArray[SHORTS]
	    + headerArray[CHARS] + headerArray[BYTES] + paddingLength;

	if(DEBUG) {
	    Debug.message("buffers", this, 
		    "total size of buffer we're receiving is: " + totalSize
		    + " padding: " + paddingLength);
	}


	if(unUsedLength() < totalSize) {
	    fillBuffer(totalSize);
	}

	//claim space
	usedLimit = (usedPosition + totalSize) % BUFFER_LIMIT;

	next = setView(longs, next, headerArray[LONGS], SIZEOF_LONG);
	next = setView(doubles, next, headerArray[DOUBLES], SIZEOF_DOUBLE);
	next = setView(ints, next, headerArray[INTS], SIZEOF_INT);
	next = setView(floats, next, headerArray[FLOATS], SIZEOF_FLOAT);
	next = setView(shorts, next, headerArray[SHORTS], SIZEOF_SHORT);
	next = setView(chars, next, headerArray[CHARS], SIZEOF_CHAR);
	next = setView(bytes, next, headerArray[BYTES], SIZEOF_BYTE);

	if(DEBUG) {
	    Debug.exit("buffers", this, "received: l[" + longs.remaining()
		    + "] d[" + doubles.remaining()
		    + "] i[" + ints.remaining()
		    + "] f[" + floats.remaining()
		    + "] s[" + shorts.remaining() + "] c[" + chars.remaining()
		    + "] b[" + bytes.remaining()
		    + "]");
	}

    }

    /**
     * Returns if there is a message waiting. Will not try to receive data 
     * unless there is a command, but the operands are not received yet. Also
     * inits serialization stream if needed
     *
     * @throws IOException if an error occured, or the peer closed the
     *			   connection.
     */
    boolean messageWaiting() throws IOException {
	byte command;

	while(true) {
	    if((sis == null && available() == 0) 
		    || (sis != null && sis.available() == 0)) {
		//no data available at all
		return false;
	    }

	    //create new input stream if needed
	    if (sis == null) {
		sis = type.createSerializationInputStream(this);
	    }

	    command = sis.readByte();

	    switch(command) {
		case NEW_RECEIVER:
		    sis.close();
		    sis = null;
		    break;
		case NEW_MESSAGE:
		    return true;
		case CLOSE_ALL_CONNECTIONS:
		    sis.close();
		    throw new IOException("connection closed by peer");
		case CLOSE_ONE_CONNECTION:
		    sis.close();
		    sis = null;

		    NioReceivePortIdentifier rpi;
		    rpi = new NioReceivePortIdentifier(this);

		    if (rpi.equals(this.rpi)) {
			throw new IOException("connection closed by peer");
		    }
		    break;
		default:
		    throw new IbisError("unknown opcode in command");
	    }
	}
    }

    /**
     * Reads data from the channel into the buffer ONCE.
     * Wraps the buffer if needed.
     */ 
    void readFromChannel() throws IOException {
	int count;

	if (DEBUG) {
	    Debug.message("channels", this, "reading into buffer, position("
		    + buffer.position() + ") limit(" + buffer.limit() + ")");
	}

	count = channel.read(buffer);
	if(count == -1) {
	    throw new IOException("END-OF-STREAM encountered");
	}
	this.count += count;

	if (DEBUG) {
	    Debug.message("channels", this, "read " + count + " bytes, total"
		    + " bytes read now " + this.count);
	}

	if((!buffer.hasRemaining()) && (buffer.limit() == BUFFER_LIMIT)
		&& (usedPosition > 0)) {
	    //wrap around
	    buffer.position(0);
	    buffer.limit(usedPosition - 1);
	    if (DEBUG) {
		Debug.message("channels", this, "buffer wrapped, position("
		    + buffer.position() + ") limit(" + buffer.limit() + ")");
	    }
	}
    }

    int remaining() {
	return (  (longs.remaining() * SIZEOF_LONG)
		+ (doubles.remaining() * SIZEOF_DOUBLE)
		+ (ints.remaining() * SIZEOF_INT)
		+ (floats.remaining() * SIZEOF_INT)
		+ (shorts.remaining() * SIZEOF_SHORT)
		+ (chars.remaining() * SIZEOF_CHAR)
		+ (bytes.remaining() * SIZEOF_BYTE));
    }

    /**
     * returns the number of bytes garanteed to be readable without blocking.
     *
     */
    public int available() {
	int result = remaining();

	if (unUsedLength() > 0) {
	    //mapping of unUnsedLength to really readable bytes is unpredictable
	    //so we assume there's only 1 byte in it
	    result += 1;
	}

	return result;
    }

    /**
     * returns if there is any data that hasn't been read.
     */
    boolean dataLeft() {
	try {
	    if(sis == null) {
		return (available() != 0);
	    } else {
		return (sis.available() != 0);
	    }
	} catch (IOException e) {
	    return false;
	}
    }

    public long bytesRead() {
	return count;
    }

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
	    result =  bytes.get();
	}

	if (DEBUG) {
	    Debug.message("data", this, "received byte: " + result);
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

    public void readArray(boolean ref[], int off, int len) 
	    throws IOException {
	for(int i = off; i < (off + len); i++) {
	    ref[i] = ((readByte() == (byte) 1) ? true : false) ; 
	}
    }


    public void readArray(byte ref[], int off, int len) 
		throws IOException {
	try {
	    bytes.get(ref, off, len);
	} catch (BufferUnderflowException e) {
	    //do this the hard way
	    int left = len;
	    int offset = off;
	    int size;

	    while(left > 0) {
		//copy as much as possible to the buffer
		size = Math.min(left, bytes.remaining());
		bytes.get(ref, offset, size);
		offset += size;
		left -= size;

		// if still needed, fetch some more bytes from the
		// channel
		if(left > 0) {
		    receive();
		}
	    }
	}

	if (DEBUG) {
	    String message = "received byte[], Contents: ";
	    for (int i = off; i < (off+len); i++) {
		message = message + ref[i] + " ";
	    }

	    Debug.message("data", this, message);
	}
    }

    public void readArray(char ref[], int off, int len) 
		throws IOException {
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

    public void readArray(short ref[], int off, int len) 
		throws IOException {
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

    public void readArray(int ref[], int off, int len) 
		throws IOException {
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

    public void readArray(long ref[], int off, int len) 
		throws IOException {
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

    public void readArray(float ref[], int off, int len) 
		throws IOException {
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

    public void readArray(double ref[], int off, int len) 
		throws IOException {
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

    public void close() throws IOException {
	//IGNORED
    }

    /**
     * fills the buffer upto at least "minimum" bytes.
     *
     * @throws IOException if an error occured on reading from the channel
     */
    protected abstract void fillBuffer(int minimum) throws IOException;

    /**
     * Closes this dissipator
     */
    public abstract void reallyClose() throws IOException;

}
