package ibis.impl.nio;

import ibis.io.Accumulator;
import ibis.ipl.IbisError;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.GatheringByteChannel;

/**
 * Nio Accumulator. Writes data to java.nio.ByteBuffer's
 *
 * A NioAccumulator may not send any stream header or trailer data.
 */
public abstract class NioAccumulator extends Accumulator implements Config {
    public static final int SIZEOF_BYTE = 1;
    public static final int SIZEOF_CHAR = 2;
    public static final int SIZEOF_SHORT = 2;
    public static final int SIZEOF_INT = 4;
    public static final int SIZEOF_LONG = 8;
    public static final int SIZEOF_FLOAT = 4;
    public static final int SIZEOF_DOUBLE = 8;

    static final int INITIAL_CONNECTIONS_SIZE = 8;

    private SendBuffer buffer;

    private ByteBuffer bytes;
    private CharBuffer chars;
    private ShortBuffer shorts;
    private IntBuffer ints;
    private LongBuffer longs;
    private FloatBuffer floats;
    private DoubleBuffer doubles;

    protected NioAccumulatorConnection[] connections;
    protected int nrOfConnections = 0;

    long count = 0;

    protected NioAccumulator() {
	buffer = SendBuffer.get();

	bytes = buffer.bytes;
	chars = buffer.chars;
	shorts = buffer.shorts;
	ints = buffer.ints;
	longs = buffer.longs;
	floats = buffer.floats;
	doubles = buffer.doubles;

	connections = new NioAccumulatorConnection[INITIAL_CONNECTIONS_SIZE];
    }

    synchronized public long bytesWritten() {
	return count;
    }

    synchronized public void resetBytesWritten() {
	count = 0;
    }

    synchronized void add(NioReceivePortIdentifier receiver, 
	    GatheringByteChannel channel) throws IOException {
	for(int i = 0; i < nrOfConnections; i++) {
	    if(connections[i].peer == receiver) {
		throw new IOException("tried to connect to a receiver we're"
			+ " already connected to");
	    }
	}

	if(nrOfConnections == connections.length) {
	    NioAccumulatorConnection[] newConnections 
			= new NioAccumulatorConnection[connections.length * 2];
	    for(int i = 0; i < connections.length; i++) {
		newConnections[i] = connections[i];
	    }
	    connections = newConnections;
	}

	connections[nrOfConnections] = newConnection(channel, receiver);
	nrOfConnections++;
    }

    synchronized void remove(NioReceivePortIdentifier receiver) {
	for(int i = 0; i < nrOfConnections; i++) {
	    if(connections[i].peer == receiver) {
		connections[i].close();
		connections[i] = connections[nrOfConnections - 1];
		connections[nrOfConnections - 1] = null;
		nrOfConnections--;
		return;
	    }
	}
	throw new IbisError("tried to remove non existing connections");
    }

    /**
     * Returns a list of all the receivers this accumulator is connected to
     */
    synchronized NioReceivePortIdentifier[] connections()  {
	NioReceivePortIdentifier[] result;

	result = new  NioReceivePortIdentifier[nrOfConnections];
	for (int i = 0; i < nrOfConnections; i++) {
	    result[i] = connections[i].peer;
	}
	return result;
    }

    synchronized private void send() throws IOException {
	if(buffer.isEmpty()) {
	    return;
	}

	buffer.flip();

	count += buffer.remaining();

	doSend(buffer);

	//get a new buffer
	buffer = SendBuffer.get();
	bytes = buffer.bytes;
	chars = buffer.chars;
	shorts = buffer.shorts;
	ints = buffer.ints;
	longs = buffer.longs;
	floats = buffer.floats;
	doubles = buffer.doubles;
    }

    synchronized public void flush() throws IOException {
	send();
	doFlush();
    }

    synchronized public void close() throws IOException {
	if(!buffer.isEmpty()) {
	    doSend(buffer);
	    doFlush();
	} else {
	    SendBuffer.recycle(buffer);
	    doFlush();
	}

	for(int i = 0; i < nrOfConnections; i++) {
	    connections[i].close();
	}

	buffer = null;
    }

    /**
     * {@inheritDoc}
     */
    public void writeBoolean(boolean value) throws IOException {
	if(DEBUG_LEVEL >=  RIDICULOUSLY_HIGH_DEBUG_LEVEL) {
	    System.err.println("NioAccumulator.writeBoolean(" + value + ")");
	}
	if(value) {
	    writeByte((byte) 1);
	} else {
	    writeByte((byte) 0);
	}
    }

    /**
     * {@inheritDoc}
     */
    public void writeByte(byte value) throws IOException {
	if(DEBUG) {
	    Debug.message("data", this, "writeByte(" + value + ")");
	}
	try {
	    bytes.put(value);
	} catch (BufferOverflowException e) {
	    //buffer was full, send
	    send();
	    //and try again
	    bytes.put(value); 
	}
    }

    /**
     * {@inheritDoc}
     */
    public void writeChar(char value) throws IOException {
	if(DEBUG_LEVEL >=  RIDICULOUSLY_HIGH_DEBUG_LEVEL) {
	    System.err.println("NioAccumulator.writeChar(" + value + ")");
	}
	try {
	    chars.put(value);
	} catch (BufferOverflowException e) {
	    send();
	    chars.put(value);
	}
    }

    /**
     * {@inheritDoc}
     */
    public void writeShort(short value) throws IOException {
	if(DEBUG_LEVEL >=  RIDICULOUSLY_HIGH_DEBUG_LEVEL) {
	    System.err.println("NioAccumulator.writeShort(" + value + ")");
	}
	try {
	    shorts.put(value);
	} catch (BufferOverflowException e) {
	    send();
	    shorts.put(value);
	}
    }

    /**
     * {@inheritDoc}
     */
    public void writeInt(int value) throws IOException {
	if(DEBUG_LEVEL >=  RIDICULOUSLY_HIGH_DEBUG_LEVEL) {
	    System.err.println("NioAccumulator.writeInt(" + value + ")");
	}
	try {
	    ints.put(value);
	} catch (BufferOverflowException e) {
	    send();
	    ints.put(value);
	}
    }

    /**
     * {@inheritDoc}
     */
    public void writeLong(long value) throws IOException {
	if(DEBUG_LEVEL >=  RIDICULOUSLY_HIGH_DEBUG_LEVEL) {
	    System.err.println("NioAccumulator.writeLong(" + value + ")");
	}
	try {
	    longs.put(value);
	} catch (BufferOverflowException e) {
	    send();
	    longs.put(value);
	}
    }

    /**
     * {@inheritDoc}
     */
    public void writeFloat(float value) throws IOException {
	if(DEBUG_LEVEL >=  RIDICULOUSLY_HIGH_DEBUG_LEVEL) {
	    System.err.println("NioAccumulator.writeFloat(" + value + ")");
	}
	try {
	    floats.put(value);
	} catch (BufferOverflowException e) {
	    send();
	    floats.put(value);
	}
    }

    /**
     * {@inheritDoc}
     */
    public void writeDouble(double value) throws IOException {
	if(DEBUG_LEVEL >=  RIDICULOUSLY_HIGH_DEBUG_LEVEL) {
	    System.err.println("NioAccumulator.writeDouble(" + value + ")");
	}
	try {
	    doubles.put(value);
	} catch (BufferOverflowException e) {
	    send();
	    doubles.put(value);
	}
    }

    public void writeArray(boolean[] array, int off, int len) 
	    throws IOException {
	if(DEBUG_LEVEL >=  RIDICULOUSLY_HIGH_DEBUG_LEVEL) {
	    System.err.println("NioAccumulator.writeArray(bool[], off = " + off
			       + " len = " + len + ")");
	}
	for(int i = off; i < (off + len); i++) {
	    if(array[i]) {
		writeByte((byte) 1);
	    } else {
		writeByte((byte) 0);
	    }
	}
    }

    public void writeArray(byte[] array, int off, int len) 
	    throws IOException {
	if(DEBUG) {
	    String message = "NioAccumulator.writeArray(byte[], off = " + off
			     + " len = " + len + ") Contents: ";

	    for(int i = off; i < (off+len); i++) {
		message = message + array[i] + " ";
	    }
	    Debug.message("data", this, message);
	}

	try {
	    bytes.put(array, off, len);
	} catch (BufferOverflowException e) {
	    //do this the hard way

	    while(len > 0) {
		if(!bytes.hasRemaining()) {
		    send();
		}

		int size = Math.min(len, bytes.remaining());
		bytes.put(array, off, size);
		off += size;
		len -= size;
	    }
	}
    }

    public void writeArray(char[] array, int off, int len) 
	    throws IOException {
	if(DEBUG_LEVEL >=  RIDICULOUSLY_HIGH_DEBUG_LEVEL) {
	    System.err.println("NioAccumulator.writeArray(c[], off = " + off
			       + " len = " + len + ")");
	}
	try {
	    chars.put(array, off, len);
	} catch (BufferOverflowException e) {
	    //do this the hard way

	    while(len > 0) {
		if(!chars.hasRemaining()) {
		    send();
		}

		int size = Math.min(len, chars.remaining());
		chars.put(array, off, size);
		off += size;
		len -= size;
	    }
	}
    }

    public void writeArray(short[] array, int off, int len) 
	    throws IOException {
	if(DEBUG_LEVEL >=  RIDICULOUSLY_HIGH_DEBUG_LEVEL) {
	    System.err.println("NioAccumulator.writeArray(s[], off = " + off
			       + " len = " + len + ")");
	}
	try {
	    shorts.put(array, off, len);
	} catch (BufferOverflowException e) {
	    //do this the hard way

	    while(len > 0) {
		if(!shorts.hasRemaining()) {
		    send();
		}

		int size = Math.min(len, shorts.remaining());
		shorts.put(array, off, size);
		off += size;
		len -= size;
	    }
	}
    }

    public void writeArray(int[] array, int off, int len) throws IOException {
	if(DEBUG_LEVEL >=  RIDICULOUSLY_HIGH_DEBUG_LEVEL) {
	    System.err.println("NioAccumulator.writeArray(i[], off = " + off
			       + " len = " + len + ")");
	}
	try {
	    ints.put(array, off, len);
	} catch (BufferOverflowException e) {
	    //do this the hard way

	    while(len > 0) {
		if(!ints.hasRemaining()) {
		    send();
		}

		int size = Math.min(len, ints.remaining());
		ints.put(array, off, size);
		off += size;
		len -= size;
	    }
	}
    }

    public void writeArray(long[] array, int off, int len) throws IOException {
	if(DEBUG_LEVEL >=  RIDICULOUSLY_HIGH_DEBUG_LEVEL) {
	    System.err.println("NioAccumulator.writeArray(l[], off = " + off
			       + " len = " + len + ")");
	}
	try {
	    longs.put(array, off, len);
	} catch (BufferOverflowException e) {
	    //do this the hard way

	    while(len > 0) {
		if(!longs.hasRemaining()) {
		    send();
		}

		int size = Math.min(len, longs.remaining());
		longs.put(array, off, size);
		off += size;
		len -= size;
	    }
	}
    }

    public void writeArray(float[] array, int off, int len) throws IOException {
	if(DEBUG_LEVEL >=  RIDICULOUSLY_HIGH_DEBUG_LEVEL) {
	    System.err.println("NioAccumulator.writeArray(f[], off = " + off
			       + " len = " + len + ")");
	}
	try {
	    floats.put(array, off, len);
	} catch (BufferOverflowException e) {
	    //do this the hard way

	    while(len > 0) {
		if(!floats.hasRemaining()) {
		    send();
		}

		int size = Math.min(len, floats.remaining());
		floats.put(array, off, size);
		off += size;
		len -= size;
	    }
	}
    }

    public void writeArray(double[] array, int off, int len) 
	    throws IOException {
	if(DEBUG_LEVEL >=  RIDICULOUSLY_HIGH_DEBUG_LEVEL) {
	    System.err.println("NioAccumulator.writeArray(d[], off = " + off
			       + " len = " + len + ")");
	}
	try {
	    doubles.put(array, off, len);
	} catch (BufferOverflowException e) {
	    //do this the hard way

	    while(len > 0) {
		if(!doubles.hasRemaining()) {
		    send();
		}

		int size = Math.min(len, doubles.remaining());
		doubles.put(array, off, size);
		off += size;
		len -= size;
	    }
	}
    }

    abstract NioAccumulatorConnection newConnection(
	    GatheringByteChannel channel, NioReceivePortIdentifier peer)
	throws IOException;

    abstract void doSend(SendBuffer buffer) throws IOException;

    abstract void doFlush() throws IOException;
}
