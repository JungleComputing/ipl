package ibis.impl.nio;

import java.io.IOException;

import ibis.io.SerializationOutputStream;

import java.nio.channels.GatheringByteChannel;
import java.nio.ByteBuffer;
import java.nio.BufferOverflowException;

import java.lang.Math;

/**
 * "Cheating" Serialization Outputstream. Actually is only able to write bytes.
 * If you try to write anything else to it, it will throw an Exception. 
 * Outputs to the given channel.
 */
public final class NioOutputStream extends SerializationOutputStream 
						implements Config {


    public static final int DEFAULT_BUFFER_SIZE = 8 * 1024;

    GatheringByteChannel channel;
    private ByteBuffer buffer;

    /**
     * Constructor to make an "optimal" outputstream.
     * Optimal when used for lots of data
     */
    NioOutputStream(GatheringByteChannel channel) throws IOException {
	this(channel, DEFAULT_BUFFER_SIZE, true);
    }

    NioOutputStream(GatheringByteChannel channel,
	    int bufferSize, boolean direct) throws IOException {
	super();

	this.channel = channel;

	if(direct) {
	    buffer = ByteBuffer.allocateDirect(bufferSize);
	} else {
	    buffer = ByteBuffer.allocate(bufferSize);
	}

	buffer.clear();
    }


    // OutputStream interface

    /**
     * Writes all data out to the channel. If the channel is in 
     * non-blocking mode, this might take a few tries.
     */
    public void flush() throws IOException {
	if (DEBUG_LEVEL >=  RIDICULOUSLY_HIGH_DEBUG_LEVEL) {
	    System.err.println("NioOutputStream: flushing " +
		    buffer.position() + " bytes");
	}

	buffer.flip();
	do {	
	    channel.write(buffer);
	} while(buffer.hasRemaining()); 
	buffer.clear();
    }

    public void write(int b) throws IOException {
	try {
	    buffer.put((byte) (0xff & b));
	} catch (BufferOverflowException e) {
	    // buffer was full, flush
	    flush();
	    // and try again
	    buffer.put((byte) (0xff & b));
	}
    }

    public void write(byte[] b) throws IOException {
	write(b, 0, b.length);
    }

    public void write(byte[] b, int off, int len) throws IOException {
	try {
	    buffer.put(b, off, len);
	} catch (BufferOverflowException e) {
	    // there was more data than bytes free in the buffer
	    // do it the hard way...

	    while(len > 0) {
		if(!buffer.hasRemaining()) {
		    flush();
		}

		int size = Math.min(len, buffer.remaining());
		buffer.put(b, off, size);
		off += size;
		len -= size;
	    }
	}
    }

    /** 
     * Flushes the buffer to the  channel, if needed.
     * Doesn't actually close the channel because then the ObjectOutputStream
     * will close it when we don't want to
     */
    public void close() throws IOException {
	if (DEBUG_LEVEL >=  VERY_HIGH_DEBUG_LEVEL) {
	    System.err.println("NioOutputStream.close() called");
	}
	if(buffer.position() != 0) {
	    flush();
	}
    }

    /**
     * REALLY closes the channel. Be carefull with this one, as someone else
     * may also be using the channel!
     */
    void reallyClose() throws IOException {
	close();
	channel.close();
    }



    // SerializationOutputStream interface

    public void writeByte(int b) throws IOException {
	write(b);
    }

    public void writeArray(byte[] ref, int off, int len) throws IOException {
	write(ref, off, len);
    }

    /**
     * Returns the name of the current serialization implementation: "nio-none".
     *
     * @return the name of the current serialization implementation.
     */
    public String serializationImplName() {
	return "nio-none";
    }




    // End of useful functions. Just throws an exception for the rest




    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeUTF(String str) throws IOException {
	throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeClass(Class ref) throws IOException {
	throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeBoolean(boolean value) throws IOException {
	throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeChar(char value) throws IOException {
	throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeShort(int value) throws IOException {
	throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeInt(int value) throws IOException {
	throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeLong(long value) throws IOException {
	throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeFloat(float value) throws IOException {
	throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeDouble(double value) throws IOException {
	throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeBytes(String s) throws IOException {
	throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeChars(String s) throws IOException {
	throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeObjectOverride(Object ref) throws IOException {
	throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeUnshared(Object ref) throws IOException {
	throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeFields() throws IOException {
	throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public PutField putFields() throws IOException {
	throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void defaultWriteObject() throws IOException {
	throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeArray(boolean[] ref, int off, int len) throws IOException {
	throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeArray(short[] ref, int off, int len) throws IOException {
	throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeArray(char[] ref, int off, int len) throws IOException {
	throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeArray(int[] ref, int off, int len) throws IOException {
	throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeArray(long[] ref, int off, int len) throws IOException {
	throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeArray(float[] ref, int off, int len) throws IOException {
	throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeArray(double[] ref, int off, int len) throws IOException {
	throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeArray(Object[] ref, int off, int len) throws IOException {
	throw new IOException("Illegal data type written");
    }

    /**
     * No statistics are printed.
     */
    public void statistics() {
    }
}
