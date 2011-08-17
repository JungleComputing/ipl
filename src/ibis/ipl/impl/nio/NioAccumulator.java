/* $Id$ */

package ibis.ipl.impl.nio;

import ibis.io.DataOutputStream;
import ibis.ipl.impl.ReceivePortIdentifier;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Nio Accumulator. Writes data to java.nio.ByteBuffers.
 * 
 * A NioAccumulator may not send any stream header or trailer data.
 */
public abstract class NioAccumulator extends DataOutputStream implements Config {
    public static final int SIZEOF_BYTE = 1;

    public static final int SIZEOF_CHAR = 2;

    public static final int SIZEOF_SHORT = 2;

    public static final int SIZEOF_INT = 4;

    public static final int SIZEOF_LONG = 8;

    public static final int SIZEOF_FLOAT = 4;

    public static final int SIZEOF_DOUBLE = 8;

    static final int INITIAL_CONNECTIONS_SIZE = 8;

    private static Logger logger = LoggerFactory.getLogger(NioAccumulator.class);

    private SendBuffer buffer;

    private ByteBuffer bytes;

    private CharBuffer chars;

    private ShortBuffer shorts;

    private IntBuffer ints;

    private LongBuffer longs;

    private FloatBuffer floats;

    private DoubleBuffer doubles;

    NioAccumulatorConnection[] connections
            = new NioAccumulatorConnection[INITIAL_CONNECTIONS_SIZE];

    int nrOfConnections = 0;

    NioSendPort port;

    long count = 0;

    protected NioAccumulator(NioSendPort port) {
        this.port = port;

        buffer = SendBuffer.get();

        bytes = buffer.bytes;
        chars = buffer.chars;
        shorts = buffer.shorts;
        ints = buffer.ints;
        longs = buffer.longs;
        floats = buffer.floats;
        doubles = buffer.doubles;
    }

    public int bufferSize() {
        return PRIMITIVE_BUFFER_SIZE;
    }
    
    synchronized public long bytesWritten() {
        return count;
    }

    synchronized public void resetBytesWritten() {
        count = 0;
    }

    synchronized public long getAndResetBytesWritten() {
        long result = count;
        count = 0;

        return result;
    }

    synchronized NioAccumulatorConnection add(GatheringByteChannel channel,
            ReceivePortIdentifier receiver) throws IOException {
        if (nrOfConnections == connections.length) {
            NioAccumulatorConnection[] newConnections = new NioAccumulatorConnection[connections.length * 2];
            for (int i = 0; i < connections.length; i++) {
                newConnections[i] = connections[i];
            }
            connections = newConnections;
        }

        NioAccumulatorConnection c = newConnection(channel, receiver);
        connections[nrOfConnections] = c;
        nrOfConnections++;

        return c;
    }

    synchronized void removeConnection(ReceivePortIdentifier receiver)
            throws IOException {
        for (int i = 0; i < nrOfConnections; i++) {
            if (connections[i].target == receiver) {
                nrOfConnections--;
                connections[i] = connections[nrOfConnections];
                connections[nrOfConnections] = null;
                return;
            }
        }
        throw new IOException("tried to remove non existing connections");
    }

    synchronized private void send() throws IOException {
        if (buffer.isEmpty()) {
            return;
        }

        buffer.flip();

        count += buffer.remaining();

        if (doSend(buffer)) {
            // buffer was completely send, just clear it and use it again
            buffer.clear();
        } else {
            // get a new buffer
            buffer = SendBuffer.get();
            bytes = buffer.bytes;
            chars = buffer.chars;
            shorts = buffer.shorts;
            ints = buffer.ints;
            longs = buffer.longs;
            floats = buffer.floats;
            doubles = buffer.doubles;
        }
    }

    /*
     * makes sure all data given to the accumulator is send ,or at least copied.
     */
    synchronized public void flush() throws IOException {
        send();
        doFlush();
    }

    synchronized public void close() throws IOException {
        if (!buffer.isEmpty()) {
            doSend(buffer);
            doFlush();
        } else {
            SendBuffer.recycle(buffer);
            doFlush();
        }
        buffer = null;
    }

    public void writeBoolean(boolean value) throws IOException {
        if (value) {
            writeByte((byte) 1);
        } else {
            writeByte((byte) 0);
        }
    }

    public void writeByte(byte value) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("writeByte(" + value + ")");
        }
        try {
            bytes.put(value);
        } catch (BufferOverflowException e) {
            // buffer was full, send
            send();
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
            send();
            chars.put(value);
        }
    }

    public void writeShort(short value) throws IOException {
        try {
            shorts.put(value);
        } catch (BufferOverflowException e) {
            send();
            shorts.put(value);
        }
    }

    public void writeInt(int value) throws IOException {
        try {
            ints.put(value);
        } catch (BufferOverflowException e) {
            send();
            ints.put(value);
        }
    }

    public void writeLong(long value) throws IOException {
        try {
            longs.put(value);
        } catch (BufferOverflowException e) {
            send();
            longs.put(value);
        }
    }

    public void writeFloat(float value) throws IOException {
        try {
            floats.put(value);
        } catch (BufferOverflowException e) {
            send();
            floats.put(value);
        }
    }

    public void writeDouble(double value) throws IOException {
        try {
            doubles.put(value);
        } catch (BufferOverflowException e) {
            send();
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
        if (logger.isDebugEnabled()) {
            String message = "NioAccumulator.writeArray(byte[], off = " + off
                    + " len = " + len + ") Contents: ";

            for (int i = off; i < (off + len); i++) {
                message = message + array[i] + " ";
            }
            if (logger.isDebugEnabled()) {
                logger.debug(message);
            }
        }

        try {
            bytes.put(array, off, len);
        } catch (BufferOverflowException e) {
            // do this the hard way

            while (len > 0) {
                if (!bytes.hasRemaining()) {
                    send();
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
                    send();
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
        try {
            ints.put(array, off, len);
        } catch (BufferOverflowException e) {
            // do this the hard way

            while (len > 0) {
                if (!ints.hasRemaining()) {
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
        try {
            longs.put(array, off, len);
        } catch (BufferOverflowException e) {
            // do this the hard way

            while (len > 0) {
                if (!longs.hasRemaining()) {
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
        try {
            floats.put(array, off, len);
        } catch (BufferOverflowException e) {
            // do this the hard way

            while (len > 0) {
                if (!floats.hasRemaining()) {
                    send();
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
                    send();
                }

                int size = Math.min(len, doubles.remaining());
                doubles.put(array, off, size);
                off += size;
                len -= size;
            }
        }
    }
    
    public void writeByteBuffer(ByteBuffer b) throws IOException {
        try {
            bytes.put(b);
        } catch (BufferOverflowException e) {
            // do this the hard way
            int len = b.limit() - b.position();
            while (len > 0) {
                if (!bytes.hasRemaining()) {
                    send();
                }

                int size = Math.min(len, bytes.remaining());
                b.limit(b.position() + size);
                b.put(b);
                len -= size;
            }
        }
    }

    abstract NioAccumulatorConnection newConnection(
            GatheringByteChannel channel, ReceivePortIdentifier peer)
            throws IOException;

    /**
     * @return is the buffer already send or not. If it is not, the
     *         implementation will recycle it when it's done with it.
     */
    abstract boolean doSend(SendBuffer buffer) throws IOException;

    abstract void doFlush() throws IOException;
}
