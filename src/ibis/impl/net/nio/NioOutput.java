package ibis.impl.net.nio;

import ibis.impl.net.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.util.Hashtable;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.LongBuffer;
import java.nio.FloatBuffer;
import java.nio.DoubleBuffer;
import java.nio.channels.SocketChannel;
import java.nio.ByteOrder;
import java.nio.BufferOverflowException;

/**
 * The NIO TCP output implementation.
 */
public final class NioOutput extends NetOutput {

    public static int BUFFER_SIZE =	1024*1024;	// bytes
    public static int NR_OF_BUFFERS = 25;

    public static int	HEADER = 0,
    BYTE_BUFFER = 1,
    CHAR_BUFFER = 2,
    SHORT_BUFFER = 3,
    INT_BUFFER = 4,
    LONG_BUFFER = 5,
    FLOAT_BUFFER = 6,
    DOUBLE_BUFFER = 7;

    public static int NR_OF_PRIMITIVES = 7;

    public static int FIRST_BYTE_BUFFER = NR_OF_PRIMITIVES + 1;

    /**
     * The communication channel.
     */
    private SocketChannel			socketChannel = null;

    /**
     * The peer {@link ibis.impl.net.NetReceivePort NetReceivePort}
     * local number.
     */
    private Integer			 rpn	   = null;

    /**
     * The number of bytes send since the last reset of this counter.
     */
    private long			bytesSend	= 0;

    /**
     * did we put anything in a buffer since the last flush
     */
    private boolean			needFlush	= false;

    /**
     * The send buffers. The first buffer is reserved for the header
     * containing how many primitives are send, the next NR_OF_PRIMITIVES
     * are reserved for the buffers containing primitives, the rest of
     * the buffers can be used to store byte arrays that need to be
     * send
     */
    private ByteBuffer[]	buffers = new ByteBuffer[NR_OF_BUFFERS];

    /**
     * The views of the bytebuffers used to fill them.
     */
    private IntBuffer    header;
    private ByteBuffer   byteBuffer;
    private CharBuffer   charBuffer;
    private ShortBuffer  shortBuffer;
    private IntBuffer    intBuffer;
    private LongBuffer   longBuffer;
    private FloatBuffer  floatBuffer;
    private DoubleBuffer doubleBuffer;

    /* the number of buffers already in use, the minimum is 8, 1 header
     * and 7 primitive buffers
     */
    private int nextBufferToFill = FIRST_BYTE_BUFFER;


    /**
     * Constructor.
     *
     * @param sp the properties of the output's 
     * {@link ibis.impl.net.NetSendPort NetSendPort}.
     * @param driver the Tcp driver instance.
     * @param output the controlling output.
     */
    NioOutput(NetPortType pt, NetDriver driver, String context) 
	throws IOException {
	    super(pt, driver, context);
	    headerLength = 0;

	}


    private void setupBuffers(ByteOrder order) throws IOException {

	buffers[HEADER] = 
	    ByteBuffer.allocateDirect(NR_OF_PRIMITIVES * 4).
	    order(order);

	for(int i = BYTE_BUFFER; i <= DOUBLE_BUFFER; i++) {
	    buffers[i] = ByteBuffer.allocateDirect(BUFFER_SIZE).
		order(order);
	    buffers[i].clear();
	}

	header = buffers[HEADER].asIntBuffer();
	byteBuffer = buffers[BYTE_BUFFER]; // just a shorthand 
	charBuffer = buffers[CHAR_BUFFER].asCharBuffer();
	shortBuffer = buffers[SHORT_BUFFER].asShortBuffer();
	intBuffer = buffers[INT_BUFFER].asIntBuffer();
	longBuffer = buffers[LONG_BUFFER].asLongBuffer();
	floatBuffer = buffers[FLOAT_BUFFER].asFloatBuffer();
	doubleBuffer = buffers[DOUBLE_BUFFER].asDoubleBuffer();
    }

    /*
     * Sets up an outgoing TCP connection (using nio).
     *
     * @param rpn {@inheritDoc}
     * @param is {@inheritDoc}
     * @param os {@inheritDoc}
     */
    public void setupConnection(NetConnection cnx)
	throws IOException {
	    ByteOrder peerOrder;

	    if (this.rpn != null) {
		throw new Error("connection already established");
	    }

	    this.rpn = cnx.getNum();

	    try {
		ObjectInputStream is = new ObjectInputStream(
			cnx.getServiceLink().
			getInputSubStream(this, "nio"));

		Hashtable	rInfo = (Hashtable)is.readObject();
		is.close();
		InetAddress raddr =  (InetAddress)rInfo.get("tcp_address");
		int rport = ((Integer)rInfo.get("tcp_port")).intValue();
		InetSocketAddress sa = new InetSocketAddress(raddr, rport);

		socketChannel = SocketChannel.open(sa);

		socketChannel.socket().setTcpNoDelay(true);

		/* figure out what byteOrder we need
		   for the output buffers */

		if( ( (String)rInfo.get("byte_order") ).
			compareTo(ByteOrder.LITTLE_ENDIAN.toString()) == 0) {
		    peerOrder = ByteOrder.LITTLE_ENDIAN;
		} else {
		    peerOrder = ByteOrder.BIG_ENDIAN;
		}
		setupBuffers(peerOrder);

	    } catch (ClassNotFoundException e) {
		throw new Error(e);
	    }
	}

    public void finish() throws IOException {
	super.finish();

	flush();
    }

    /** 
     * writes out all buffers to the channel 
     * public because of the Accumulator interface
     */
    public void flush() throws IOException {
	int firstSendBuffer, lastSendBuffer = 0;

	if (!needFlush && (nextBufferToFill == FIRST_BYTE_BUFFER)) {
	    // no data to send
	    return;
	}

	if(needFlush) {
	    /* fill the header buffer with the sizes of the 
	       primitive buffers */ 
	    header.clear();
	    header.put(byteBuffer.position());
	    header.put(charBuffer.position() * 2);
	    header.put(shortBuffer.position() * 2);
	    header.put(intBuffer.position() * 4);
	    header.put(longBuffer.position() * 8);
	    header.put(floatBuffer.position() * 4);
	    header.put(doubleBuffer.position() * 8);

	    /* set up the bytearrays so that they can be drained */
	    buffers[HEADER].position(0);
	    buffers[HEADER].limit(header.position() * 4);

	    byteBuffer.flip();

	    buffers[CHAR_BUFFER].position(0);
	    buffers[CHAR_BUFFER].limit(charBuffer.position() * 2);

	    buffers[SHORT_BUFFER].position(0);
	    buffers[SHORT_BUFFER].limit(shortBuffer.position() * 2);

	    buffers[INT_BUFFER].position(0);
	    buffers[INT_BUFFER].limit(intBuffer.position() * 4);

	    buffers[LONG_BUFFER].position(0);
	    buffers[LONG_BUFFER].limit(longBuffer.position() * 8);

	    buffers[FLOAT_BUFFER].position(0);
	    buffers[FLOAT_BUFFER].limit(floatBuffer.position() * 4);

	    buffers[DOUBLE_BUFFER].position(0);
	    buffers[DOUBLE_BUFFER].limit(doubleBuffer.position() * 8);

	    firstSendBuffer = 0;
	} else {
	    firstSendBuffer = FIRST_BYTE_BUFFER;
	}

	if (nextBufferToFill > FIRST_BYTE_BUFFER ) {
	    lastSendBuffer = nextBufferToFill - 1;
	} else {
	    for(int i = HEADER; i <= DOUBLE_BUFFER; i++) {
		if(buffers[i].hasRemaining()) {
		    lastSendBuffer = i;
		}
	    }
	} 

	/* write the array of buffers to the channel */
	do {
	    bytesSend += socketChannel.write(buffers, 
		    firstSendBuffer, (lastSendBuffer - firstSendBuffer) + 1);
	} while(buffers[lastSendBuffer].hasRemaining());

	/* release all the (non-header non-primitive) buffers written */
	for (int i = FIRST_BYTE_BUFFER; i < nextBufferToFill; i++) {
	    buffers[i] = null;
	}
	nextBufferToFill = FIRST_BYTE_BUFFER;

	if(needFlush) {

	    /* clear the primitive buffers so that they can be filled
	       again */
	    byteBuffer.clear();
	    charBuffer.clear();
	    shortBuffer.clear();
	    intBuffer.clear();
	    longBuffer.clear();
	    floatBuffer.clear();
	    doubleBuffer.clear();

	    needFlush = false;
	}

    }	

    /*
     * {@inheritDoc}
     */
    public long getCount() {
	return bytesSend;
    }

    /*
     * {@inheritDoc}
     */
    public void resetCount() {
	bytesSend = 0;
    }

    /*
     * {@inheritDoc}
     */
    public void writeBoolean(boolean value) throws IOException {
	try {
	    /* least efficient way possible of doing this, 
	     * i think (ideas welcome) --N
	     */
	    byteBuffer.put((byte) (value ? 1 : 0) );
	    needFlush = true;

	} catch (BufferOverflowException e) {
	    // the buffer was already full, flush...
	    flush();
	    // and try again...
	    writeBoolean(value);
	}
    }

    /*
     * {@inheritDoc}
     */
    public void writeByte(byte value) throws IOException {
	try {
	    byteBuffer.put(value);
	    needFlush = true;

	} catch (BufferOverflowException e) {
	    // the buffer was already full, flush...
	    flush();
	    // and try again...
	    writeByte(value);
	}
    }



    /*
     * {@inheritDoc}
     */
    public void writeChar(char value) throws IOException {
	try {
	    charBuffer.put(value);
	    needFlush = true;

	} catch (BufferOverflowException e) {
	    // the buffer was already full, flush...
	    flush();
	    // and try again...
	    writeChar(value);
	}
    }

    /*
     * {@inheritDoc}
     */
    public void writeShort(short value) throws IOException {
	try {
	    shortBuffer.put(value);
	    needFlush = true;

	} catch (BufferOverflowException e) {
	    // the buffer was already full, flush...
	    flush();
	    // and try again...
	    writeShort(value);
	} 
    }

    /*
     * {@inheritDoc}
     */
    public void writeInt(int value) throws IOException {
	try {
	    intBuffer.put(value);
	    needFlush = true;

	} catch (BufferOverflowException e) {
	    // the buffer was already full, flush...
	    flush();
	    // and try again...
	    writeInt(value);
	}
    }

    /*
     * {@inheritDoc}
     */
    public void writeLong(long value) throws IOException {
	try {
	    longBuffer.put(value);
	    needFlush = true;

	} catch (BufferOverflowException e) {
	    // the buffer was already full, flush...
	    flush();
	    // and try again...
	    writeLong(value);
	} 
    }

    /*
     * {@inheritDoc}
     */
    public void writeFloat(float value) throws IOException {
	try {
	    floatBuffer.put(value);
	    needFlush = true;

	} catch (BufferOverflowException e) {
	    // the buffer was already full, flush...
	    flush();
	    // and try again...
	    writeFloat(value);
	} 
    }

    /*
     * {@inheritDoc}
     */
    public void writeDouble(double value) throws IOException {
	try {
	    doubleBuffer.put(value);
	    needFlush = true;

	} catch (BufferOverflowException e) {
	    // the buffer was already full, flush...
	    flush();
	    // and try again...
	    writeDouble(value);
	} 
    }

    /*
     * {@inheritDoc}
     */
    public void writeString(String value) throws IOException {
	throw new IOException("writing Strings not implemented");
    }

    /*
     * {@inheritDoc}
     */
    public void writeObject(Object value) throws IOException {
	throw new IOException("writing Objects not implemented");
    }

    /*
     * {@inheritDoc}
     */
    public void writeArray(boolean [] destination,
	    int offset,
	    int size) throws IOException {

	ByteBuffer buffer = ByteBuffer.allocateDirect(size);

	for(int i = offset; i <  (offset + size); i++) {
	    if(destination[i] == true) {
		buffer.put((byte)1);
	    } else {
		buffer.put((byte)0);
	    }
	}

	buffer.flip();

	if(nextBufferToFill >= NR_OF_BUFFERS) {
	    flush();
	}			

	buffers[nextBufferToFill] = buffer;
	nextBufferToFill += 1;

    }


    /*
     * {@inheritDoc}
     *
     * This function wraps the given byte array into a Nio ByteBuffer,
     * then adds it to the buffers that need to be send out.
     */
    public void writeArray(byte [] destination,
	    int offset,
	    int size) throws IOException {
	ByteBuffer buffer = null;
	int length;

	if (size < 256) {
	    while(size > 0) {
		if(!byteBuffer.hasRemaining()) {
		    flush();
		}

		length = Math.min(byteBuffer.remaining(), size);
		byteBuffer.put(destination, offset, length);
		size -= length;
		offset += length;

		needFlush = true;
	    }
	} else {
	    if(nextBufferToFill >= NR_OF_BUFFERS) {
		flush();
	    }			

	    buffers[nextBufferToFill] = 
		ByteBuffer.wrap(destination, offset, size);

	    nextBufferToFill += 1;

	}

    }

    public void writeArray(char [] destination,
	    int offset,
	    int size) throws IOException {
	int length;

	while(size > 0) {
	    if(!charBuffer.hasRemaining()) {
		flush();
	    }

	    length = Math.min(charBuffer.remaining(), size);
	    charBuffer.put(destination, offset, length);
	    size -= length;
	    offset += length;

	    needFlush = true;

	}
    }

    public void writeArray(short [] destination,
	    int offset,
	    int size) throws IOException {
	int length;
	while(size > 0) {
	    if(!shortBuffer.hasRemaining()) {
		flush();
	    }

	    length = Math.min(shortBuffer.remaining(), size);
	    shortBuffer.put(destination, offset, length);
	    size -= length;
	    offset += length;

	    needFlush = true;

	}
    }

    public void writeArray(int [] destination,
	    int offset,
	    int size) throws IOException {
	int length;
	while(size > 0) {
	    if(!intBuffer.hasRemaining()) {
		flush();
	    }

	    length = Math.min(intBuffer.remaining(), size);
	    intBuffer.put(destination, offset, length);
	    size -= length;
	    offset += length;

	    needFlush = true;

	}
    }

    public void writeArray(long [] destination,
	    int offset,
	    int size) throws IOException {
	int length;

	while(size > 0) {
	    if(!longBuffer.hasRemaining()) {
		flush();
	    }

	    length = Math.min(longBuffer.remaining(), size);
	    longBuffer.put(destination, offset, length);
	    size -= length;
	    offset += length;

	    needFlush = true;
	}
    }

    public void writeArray(float [] destination,
	    int offset,
	    int size) throws IOException {
	int length;

	while(size > 0) {
	    if(!floatBuffer.hasRemaining()) {
		flush();
	    }

	    length = Math.min(floatBuffer.remaining(), size);
	    floatBuffer.put(destination, offset, length);
	    size -= length;
	    offset += length;

	    needFlush = true;

	}
    }

    public void writeArray(double [] destination,
	    int offset,
	    int size) throws IOException {
	int length;

	while(size > 0) {
	    if(!doubleBuffer.hasRemaining()) {
		flush();
	    }

	    length = Math.min(doubleBuffer.remaining(), size);
	    doubleBuffer.put(destination, offset, length);
	    size -= length;
	    offset += length;

	    needFlush = true;

	}
    }

    /*
     * {@inheritDoc}
     */
    public synchronized void close(Integer num) throws IOException {
	if (rpn == num) {

	    if (socketChannel != null) {
		flush();
		socketChannel.close();
	    }

	    rpn = null;
	}
    }

    /**
     * {@inheritDoc}
     */
    public void free() throws IOException {

	if (socketChannel != null) {
	    socketChannel = null;
	}

	rpn	  = null;

	super.free();
    }
}
