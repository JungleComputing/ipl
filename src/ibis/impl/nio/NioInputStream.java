package ibis.impl.nio;

import java.nio.ByteBuffer;
import java.nio.BufferUnderflowException;
import java.nio.channels.ScatteringByteChannel;

import java.io.IOException;

import ibis.io.SerializationInputStream;

import ibis.ipl.IbisError;

/**
 * a "Cheating" SerializationInputStream which is acually only able to
 * receive bytes. Calling any other function in the api will give you a
 * nice Exception ;)
 */
public final class NioInputStream extends SerializationInputStream
					    implements Config {

    public static final int DEFAULT_BUFFER_SIZE = 60 * 1024;


    ScatteringByteChannel channel; // channel we read from
    private ByteBuffer buffer;
    private boolean readAhead; //do we try to read as much as possible or not
    private long count;

    /**
     * Create a "optimal" inputstream (when used for lot's of data)
     */
    NioInputStream(ScatteringByteChannel channel) throws IOException {
	this(channel, true, DEFAULT_BUFFER_SIZE, true);
    }

    /**
     * Create a NioInputStream.
     *
     * @param channel the channel to get the data from
     * @param readAhead whether to try to read as much data as possible,
     * or just the minimal needed amount (WARNING! may block if the channel
     * is not in non-blocking mode
     * @param direct whether or not to use a direct buffer (see nio
     * documentation for explanation of wat this means
     */
    NioInputStream(ScatteringByteChannel channel, boolean readAhead,
	    int bufferSize, boolean direct) throws IOException {
	super();

	this.channel = channel;
	this.readAhead = readAhead;

	if(direct) {
	    buffer = ByteBuffer.allocateDirect(bufferSize);
	} else {
	    buffer = ByteBuffer.allocate(bufferSize);
	}
	buffer.limit(0); // make the buffer appear empty
    }

    /**
     * returns if this stream is buffering any data. It will never do this
     * if read ahead has been disabled.
     */
    boolean emptyBuffer() {
	return !buffer.hasRemaining();
    }

    /**
     * returns if this stream does readAhead. WARNING: if the underlying
     * channel is in blocking mode this means it might deadlock when you do
     * a read
     */
    boolean readAhead() {
	return this.readAhead;
    }

    /**
     * turns read ahead for this stream on or off. WARNING: if the underlying
     * channel is in blocking mode this means it might deadlock when you do
     * a read
     */
    void readAhead(boolean readAhead) {
	this.readAhead = readAhead;
    }

    /**
     * Returns the number of bytes read since the last reset.
     *
     * @return the number of bytes read since the last reset
     */
    long getCount() {
	return count;
    }

    /**
     * returns the number of bytes available for reading, AFTER it tries to
     * read from the channel
     */
    long poll() throws IOException {

	if(!readAhead) {
	    return buffer.remaining();
	}

	//remember the current position (from which we were reading)
	buffer.mark();

	buffer.position(buffer.limit());
	buffer.limit(buffer.capacity());

	channel.read(buffer);

	buffer.limit(buffer.position());
	buffer.reset(); // position = mark

	return buffer.remaining();
    }


    /**
     * resets the number of bytes read to zero
     */
    void resetCount() {
	count = 0;
    }



    /**
     * read at least "minimum" bytes from the underlying channel.
     * may read more if readAhead has been enabled
     * if the channel is NOT in non-blocking mode, it may block
     * indefinitely!
     */
    private void readAtLeast(int minimum) throws IOException {
	buffer.clear();

	if(!readAhead) {
	    buffer.limit(minimum);
	}

	while(buffer.position() < minimum) {
	    channel.read(buffer);
	}
	buffer.flip();
	if(DEBUG_LEVEL >= RIDICULOUSLY_HIGH_DEBUG_LEVEL) {
	    System.err.println("readAtLeast(" + minimum + ") read " +
		    buffer.limit() + " bytes");
	}
    }

    // InputStream interface

    /**
     * returns the number of bytes you can at least read from this stream 
     * without blocking.
     */
    public int available() {
	return buffer.remaining();
    }

    /**
     * Closes the input stream. Doesn't close the underlying channel!
     */
    public void close() throws IOException {
	if(DEBUG_LEVEL >= RIDICULOUSLY_HIGH_DEBUG_LEVEL) {
	    System.err.println("closing NioInputStream, position = "
		    + buffer.position() + " limit = " + buffer.limit());
	}
	if((DEBUG_LEVEL >= ERROR_DEBUG_LEVEL) && !emptyBuffer()) {
	    System.err.println("WARNING: closed NioInputStream with data"
		    + " still left in the buffer");
	}
	buffer.clear().limit(0);
    }

    /**
     * REALLY closes the input stream
     */
    void reallyClose() throws IOException {
	close();
	channel.close();
    }

    /**
     * return a byte from the stream
     */
    public int read() throws IOException {
	return readByte();
    }


    /**
     * return a byte from the stream
     */
    public byte readByte() throws IOException {
	count += 1;
	byte result;
	try {
	    result =  buffer.get();
	} catch (BufferUnderflowException e) {
	    // buffer was empty
	    readAtLeast(1);

	    result = buffer.get();
	}
	if(DEBUG_LEVEL >= RIDICULOUSLY_HIGH_DEBUG_LEVEL) {
	    System.err.println("read a total of " + count + " bytes, "
		    + buffer.remaining() + " bytes in buffer" );
	}

	return result;
    }

    /**
     * reads b.length bytes from the stream in b
     */
    public int read(byte[] b) throws IOException{
	return read(b, 0, b.length);
    }

    /**
     * reads len bytes from the stream into b
     */
    public int read(byte[] b, int off, int len) throws IOException {
//	for(int i = off; i < (off+len); i++) {
//	    b[i] = readByte();
//	}
//	return len;


	try {
	    count += len;
	    buffer.get(b, off, len);
	} catch (BufferUnderflowException e) {
	    int left = len;
	    int size;

	    while(left > 0) {
		// empty out buffer
		size = Math.min(left, buffer.remaining());
		buffer.get(b, off, size);
		off += size;
		left -= size;

		// if still needed, fetch some more bytes from the
		// channel
		if(left > 0) {
		    size = Math.min(left, buffer.capacity());
		    readAtLeast(size);
		    buffer.get(b, off, size);
		    off += size;
		    left -= size;
		}
	    }
	}
	if(DEBUG_LEVEL >= RIDICULOUSLY_HIGH_DEBUG_LEVEL) {
	    System.err.println("read a total of " + count + " bytes, "
		    + buffer.remaining() + " bytes in buffer" );
	}
	return len;
    }

    // SerializationInputStream interface

    /**
     * Returns the name of this implementation
     *
     * @return the name of this implementation.
     */
    public String serializationImplName() {
	return "nio-none";
    }

    public void readArray(byte[] dest) throws IOException {
	if(read(dest, 0, dest.length) != dest.length) {
	    throw new IbisError("reading of entire array failed!");
	}
    }

    public void readArray(byte[] b, int off, int len) throws IOException {
	if(read(b, off, len) != len) {
	    throw new IbisError("tried to get " + len
		    + " elements, but got less");
	}
    }


    // End of usefull functions. Only empty functions from here

    public void clear() {
    }

    public void statistics() {
    }

    public boolean readBoolean() throws IOException {
	throw new IOException("Illegal data type read");
    }

    public char readChar() throws IOException {
	throw new IOException("Illegal data type read");
    }

    public short readShort() throws IOException {
	throw new IOException("Illegal data type read");
    }

    public int readInt() throws IOException {
	throw new IOException("Illegal data type read");
    }

    public long readLong() throws IOException {
	throw new IOException("Illegal data type read");
    }

    public float readFloat() throws IOException {
	throw new IOException("Illegal data type read");
    }

    public double readDouble() throws IOException {
	throw new IOException("Illegal data type read");
    }

    public int readUnsignedByte() throws IOException {
	throw new IOException("Illegal data type read");
    }

    public int readUnsignedShort() throws IOException {
	throw new IOException("Illegal data type read");
    }

    public String readUTF() throws IOException {
	throw new IOException("Illegal data type read");
    }

    public Class readClass() throws IOException, ClassNotFoundException {
	throw new IOException("Illegal data type read");
    }

    public Object readObjectOverride() throws IOException 
	, ClassNotFoundException {
	    throw new IOException("Illegal data type read");
    }

    public GetField readFields() throws IOException, ClassNotFoundException {
	throw new IOException("Illegal data type read");
    }

    public void defaultReadObject() throws IOException {
	throw new IOException("Illegal data type read");
    }

    /**
     * @exception IOException when called, this is illegal.
     */
    public void readArray(boolean[] ref, int off, int len) throws IOException {
	throw new IOException("Illegal data type read");
    }

    /**
     * @exception IOException when called, this is illegal.
     */
    public void readArray(char[] ref, int off, int len) throws IOException {
	throw new IOException("Illegal data type read");
    }

    /**
     * @exception IOException when called, this is illegal.
     */
    public void readArray(short[] ref, int off, int len) throws IOException {
	throw new IOException("Illegal data type read");
    }

    /**
     * @exception IOException when called, this is illegal.
     */
    public void readArray(int[] ref, int off, int len) throws IOException {
	throw new IOException("Illegal data type read");
    }

    /**
     * @exception IOException when called, this is illegal.
     */
    public void readArray(long[] ref, int off, int len) throws IOException {
	throw new IOException("Illegal data type read");
    }

    /**
     * @exception IOException when called, this is illegal.
     */
    public void readArray(float[] ref, int off, int len) throws IOException {
	throw new IOException("Illegal data type read");
    }

    /**
     * @exception IOException when called, this is illegal.
     */
    public void readArray(double[] ref, int off, int len) throws IOException {
	throw new IOException("Illegal data type read");
    }

    /**
     * @exception IOException when called, this is illegal.
     */
    public void readArray(Object[] ref, int off, int len) 
	throws IOException, ClassNotFoundException {
	    throw new IOException("Illegal data type read");
	}
}

