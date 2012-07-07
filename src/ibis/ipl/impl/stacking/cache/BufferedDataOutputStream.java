package ibis.ipl.impl.stacking.cache;

import ibis.io.Conversion;
import ibis.io.DataOutputStream;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.WriteMessage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.logging.Level;

public class BufferedDataOutputStream extends DataOutputStream {

    /*
     * The send port which generates for me new WriteMessages. I need them so I
     * can ship my buffer to the various receive ports.
     */
    final CacheSendPort port;
    /*
     * Used to convert every primitive type to bytes.
     */
    final Conversion c;
    /*
     * Buffer used to store read data.
     */
    private byte[] buffer;
    /*
     * The current index of the buffer.
     */
    private int index;
    /*
     * The buffer's capacity.
     */
    private final int capacity;
    /*
     * No of bytes written, not counting the ones currently in the buffer.
     */
    int bytes;
    private int noMsg;
    private boolean closed;

    public BufferedDataOutputStream(CacheSendPort sp) {
        this.port = sp;
        c = Conversion.loadConversion(false);
        this.index = 0;
        this.capacity = CacheManager.BUFFER_CAPACITY;
        this.buffer = new byte[this.capacity];
    }

    @Override
    public long bytesWritten() {
        return bytes + index;
    }

    @Override
    public int bufferSize() {
        return capacity;
    }

    /**
     * Send to all the connected receive ports the message built so far. 
     */
    private void stream(boolean isLastPart) throws IOException {
        
        /*
         * All of our destinations.
         */
        Set<ReceivePortIdentifier> rpis = new HashSet<ReceivePortIdentifier>(
                Arrays.asList(port.connectedTo()));
        while (!rpis.isEmpty()) {
            /*
             * I'm gonna start caching and uncaching connections. Be safe.
             */
            synchronized (port.cacheManager) {
                /*
                 * Try to get all connections alive, but say thank you for what
                 * you get.
                 *
                 * At least one connection... please.
                 */
                ReceivePortIdentifier[] connected = port.cacheManager.getSomeConnections(port, rpis, 0);
                /*
                 * At least one element has been removed.
                 */
                assert rpis.removeAll(Arrays.asList(connected));
                /*
                 * Send the message to whoever is connected.
                 */
                WriteMessage msg = null;
                try {
                    msg = port.sendPort.newMessage();
                    noMsg++;
                    msg.writeBoolean(isLastPart);
                    msg.writeInt(index);
                    msg.writeArray(buffer, 0, index);
                    msg.finish();
                } catch (IOException ex) {
                    msg.finish(ex);
                    CacheManager.log.log(Level.SEVERE,"Failed to write {0} bytes message "
                            + "to {1} ports.", new Object[]{index, rpis.size()});
                }
                CacheManager.log.log(Level.INFO, "\tSent msg: ({0}, {1})",
                        new Object[] {isLastPart, index});
            }
        }
        /*
         * Buffer is sent to everyone. Clear it.
         */
        index = 0;
    }

    @Override
    public void flush() throws IOException {
        if(closed) {
            return ;
        }
        stream(false);
        CacheManager.log.log(Level.INFO, "\n\tFlushed {0} intermediate messages to"
                + " {1} ports.\n", new Object[] {noMsg, port.connectedTo().length});
        noMsg = 0;
    }

    @Override
    public void close() throws IOException {
        closed = true;
        stream(true);
        CacheManager.log.log(Level.INFO, "\n\tStreamed {0} intermediate messages to"
                + " {1} ports.\n", new Object[] {noMsg, port.connectedTo().length});
        noMsg = 0;
    }

    /**
     * Checks if there is space for
     * <code>incr</code> more bytes and if not, the buffer is delivered.
     *
     * @param incr the space requested
     */
    private void checkAndStream(int incr) throws IOException {
        if (index + incr > capacity) {
            bytes += index;

            stream(false);
        }
    }

    @Override
    public void write(int b) throws IOException {
        writeByte((byte) b);
    }

    @Override
    public void writeArray(boolean[] val, int off, int len) throws IOException {
        do {
            int incr = Conversion.BOOLEAN_SIZE;
            checkAndStream(incr);

            int size = Math.min((capacity - index) / incr, len);

            c.boolean2byte(val, off, size, buffer, index);

            off += size;
            len -= size;
            index += size * incr;
        } while (len > 0);
    }

    @Override
    public void writeArray(byte[] val, int off, int len) throws IOException {
        do {
            int incr = Conversion.BYTE_SIZE;
            checkAndStream(incr);

            int size = Math.min((capacity - index) / incr, len);

            System.arraycopy(val, off, buffer, index, size);

            off += size;
            len -= size;
            index += size * incr;
        } while (len > 0);
    }

    @Override
    public void writeArray(char[] val, int off, int len) throws IOException {
        do {
            int incr = Conversion.CHAR_SIZE;
            checkAndStream(incr);

            int size = Math.min((capacity - index) / incr, len);

            c.char2byte(val, off, size, buffer, index);

            off += size;
            len -= size;
            index += size * incr;
        } while (len > 0);
    }

    @Override
    public void writeArray(double[] val, int off, int len) throws IOException {
        do {
            int incr = Conversion.DOUBLE_SIZE;
            checkAndStream(incr);

            int size = Math.min((capacity - index) / incr, len);

            c.double2byte(val, off, size, buffer, index);

            off += size;
            len -= size;
            index += size * incr;
        } while (len > 0);
    }

    @Override
    public void writeArray(float[] val, int off, int len) throws IOException {
        do {
            int incr = Conversion.FLOAT_SIZE;
            checkAndStream(incr);

            int size = Math.min((capacity - index) / incr, len);

            c.float2byte(val, off, size, buffer, index);

            off += size;
            len -= size;
            index += size * incr;
        } while (len > 0);
    }

    @Override
    public void writeArray(int[] val, int off, int len) throws IOException {
        do {
            int incr = Conversion.INT_SIZE;
            checkAndStream(incr);

            int size = Math.min((capacity - index) / incr, len);

            c.int2byte(val, off, size, buffer, index);

            off += size;
            len -= size;
            index += size * incr;
        } while (len > 0);
    }

    @Override
    public void writeArray(long[] val, int off, int len) throws IOException {
        do {
            int incr = Conversion.LONG_SIZE;
            checkAndStream(incr);

            int size = Math.min((capacity - index) / incr, len);

            c.long2byte(val, off, size, buffer, index);

            off += size;
            len -= size;
            index += size * incr;
        } while (len > 0);
    }

    @Override
    public void writeArray(short[] val, int off, int len) throws IOException {
        do {
            int incr = Conversion.SHORT_SIZE;
            checkAndStream(incr);

            int size = Math.min((capacity - index) / incr, len);

            c.short2byte(val, off, size, buffer, index);

            off += size;
            len -= size;
            index += size * incr;
        } while (len > 0);
    }

    @Override
    public void writeBoolean(boolean val) throws IOException {
        byte b = c.boolean2byte(val);
        checkAndStream(1);
        buffer[index++] = b;
    }

    @Override
    public void writeByte(byte val) throws IOException {
        checkAndStream(1);
        buffer[index++] = val;
    }

    @Override
    public void writeChar(char val) throws IOException {
        int incr = Conversion.CHAR_SIZE;
        checkAndStream(incr);
        c.char2byte(val, buffer, index);
        index += incr;
    }

    @Override
    public void writeDouble(double val) throws IOException {
        int incr = Conversion.DOUBLE_SIZE;
        checkAndStream(incr);
        c.double2byte(val, buffer, index);
        index += incr;
    }

    @Override
    public void writeFloat(float val) throws IOException {
        int incr = Conversion.FLOAT_SIZE;
        checkAndStream(incr);
        c.float2byte(val, buffer, index);
        index += incr;
    }

    @Override
    public void writeInt(int val) throws IOException {
        int incr = Conversion.INT_SIZE;
        checkAndStream(incr);
        c.int2byte(val, buffer, index);
        index += incr;
    }

    @Override
    public void writeLong(long val) throws IOException {
        int incr = Conversion.LONG_SIZE;
        checkAndStream(incr);
        c.long2byte(val, buffer, index);
        index += incr;
    }

    @Override
    public void writeShort(short val) throws IOException {
        int incr = Conversion.SHORT_SIZE;
        checkAndStream(incr);
        c.short2byte(val, buffer, index);
        index += incr;
    }

    @Override
    public void writeByteBuffer(ByteBuffer val) throws IOException {

        int len = val.limit() - val.position();
        int size = capacity - index;

        while (len >= size) {
            bytes += size;
            val.get(buffer, index, size);
            stream(false);
            len -= size;
            size = capacity;
            index = 0;
        }
        if (len > 0) {
            val.get(buffer, index, len);
            index += len;
        }
    }

    @Override
    public void resetBytesWritten() {
        bytes = 0;
    }
}
