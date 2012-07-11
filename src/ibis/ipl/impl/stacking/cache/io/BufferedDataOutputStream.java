package ibis.ipl.impl.stacking.cache.io;

import ibis.io.Conversion;
import ibis.io.DataOutputStream;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.WriteMessage;
import ibis.ipl.impl.stacking.cache.CacheSendPort;
import ibis.ipl.impl.stacking.cache.manager.CacheManager;
import ibis.ipl.impl.stacking.cache.sidechannel.SideChannelProtocol;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    /*
     * Number of messages streamed at one flush.
     */
    private int noMsg;
    /*
     * If this output stream is closed.
     */
    private boolean closed;
    /*
     * When writing a message (all its streaming parts), need to make sure that
     * the receive port(s) have no other alive read message.
     */
    public final HashSet<ReceivePortIdentifier> yourReadMessageIsAliveFromMeSet;

    public BufferedDataOutputStream(CacheSendPort sp) {
        this.port = sp;
        c = Conversion.loadConversion(false);
        this.index = 0;
        this.capacity = CacheManager.BUFFER_CAPACITY;
        this.buffer = new byte[this.capacity];
        this.yourReadMessageIsAliveFromMeSet = new HashSet<ReceivePortIdentifier>();
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
        long start = System.currentTimeMillis();

        /*
         * All of our destinations.
         */
        Set<ReceivePortIdentifier> destRpis = new HashSet<ReceivePortIdentifier>(
                Arrays.asList(port.connectedTo()));
        /*
         * Subset of destionation RPIs of which we have their permission to
         * write to them.
         */
        Set<ReceivePortIdentifier> gotAttention;

        while (!destRpis.isEmpty()) {
            /*
             * We want the destined RPIs to have their live read message from
             * us, so as not to have interferance with other streaming messages.
             *
             * For this, either we have their attention from the 1st part of the
             * logical streamed message or we need to signal them that we want
             * to write a msg to them.
             *
             * Then, we need to wait for their approval, i.e. they will accept
             * an incoming message from us.
             *
             * Wait for K approvals.
             * 1 <= K <= min(MAX_CONNS, destRpis.size()).
             *
             * don't know what value for K!??!?!
             */
            int K = 1;
            gotAttention = iWantYouToReadFromMe(destRpis, K);

            destRpis.removeAll(gotAttention);

            synchronized (port.cacheManager) {
                while (!gotAttention.isEmpty()) {
                    /*
                     * Get some connections from the rpis in gotAttention array.
                     */
                    Set<ReceivePortIdentifier> connected =
                            port.cacheManager.getSomeConnections(
                            port, gotAttention, 0, false);

                    assert connected.size() > 0;

                    gotAttention.removeAll(connected);

                    /*
                     * Send the message to whoever is connected.
                     */
                    WriteMessage msg = port.sendPort.newMessage();
                    try {
                        noMsg++;
                        msg.writeBoolean(isLastPart);
                        msg.writeInt(index);
                        msg.writeArray(buffer, 0, index);
                        msg.finish();
                    } catch (IOException ex) {
                        msg.finish(ex);
                        CacheManager.log.log(Level.SEVERE, "Failed to write {0} bytes message "
                                + "to {1} ports.", new Object[]{index, destRpis.size()});
                    }
                    CacheManager.log.log(Level.INFO, "\tSent msg: ({0}, {1})",
                            new Object[]{isLastPart, index});

                    /*
                     * If this was the last part of the streamed message, I
                     * don't have the receive ports attention anymore.
                     */
                    if (isLastPart) {
                        yourLiveMessageIsNotMyConcern(connected);
                    }
                }
            }
        }
        CacheManager.addStreamTime(System.currentTimeMillis()-start);
        /*
         * Buffer is sent to everyone. Clear it.
         */
        index = 0;
    }

    private void yourLiveMessageIsNotMyConcern(Set<ReceivePortIdentifier> rpis) {
        for (ReceivePortIdentifier rpi : rpis) {
            yourReadMessageIsAliveFromMeSet.remove(rpi);
        }
    }

    private Set<ReceivePortIdentifier> iWantYouToReadFromMe(
            Set<ReceivePortIdentifier> rpis, int K) {
        
        for (ReceivePortIdentifier rpi : rpis) {
            if (!yourReadMessageIsAliveFromMeSet.contains(rpi)) {
                /*
                 * I have to let the receive port know that I want him to read
                 * my message.
                 */
                port.cacheManager.sideChannelHandler.newThreadSendProtocol(
                        port.identifier(), rpi,
                        SideChannelProtocol.READ_MY_MESSAGE);
            }
        }

        /*
         * Need to wait for K approvals.
         */
        Set<ReceivePortIdentifier> result;
        synchronized (yourReadMessageIsAliveFromMeSet) {
            result = new HashSet<ReceivePortIdentifier>(yourReadMessageIsAliveFromMeSet);
            result.retainAll(rpis);

            while (result.size() < K) {
                try {
                    yourReadMessageIsAliveFromMeSet.wait();
                } catch (InterruptedException ignoreMe) {
                }
                result.clear();
                result.addAll(yourReadMessageIsAliveFromMeSet);
                result.retainAll(rpis);
            }
        }

        return result;
    }

    @Override
    public void flush() throws IOException {
        if (closed) {
            return;
        }
        stream(false);
        CacheManager.log.log(Level.INFO, "\n\tFlushed {0} intermediate messages to"
                + " {1} ports.\n", new Object[]{noMsg, port.connectedTo().length});
        noMsg = 0;
    }

    @Override
    public void close() throws IOException {
        CacheManager.log.log(Level.INFO, "dataOut closing. closed was {0}", closed);
        if (closed) {
            return;
        }
        closed = true;
        stream(true);
        CacheManager.log.log(Level.INFO, "\n\tStreamed {0} intermediate messages to"
                + " {1} ports.\n", new Object[]{noMsg, port.connectedTo().length});
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
