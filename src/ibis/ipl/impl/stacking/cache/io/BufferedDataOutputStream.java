package ibis.ipl.impl.stacking.cache.io;

import ibis.io.Conversion;
import ibis.io.DataOutputStream;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.WriteMessage;
import ibis.ipl.impl.stacking.cache.CacheSendPort;
import ibis.ipl.impl.stacking.cache.Loggers;
import ibis.ipl.impl.stacking.cache.manager.CacheManager;
import ibis.ipl.impl.stacking.cache.sidechannel.SideChannelProtocol;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
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
       
        /*
         * We want the destined RPIs to have their live read message from us, so
         * as not to have interferance with other streaming messages.
         *
         * For this, either we have their attention from the 1st part of the
         * logical streamed message or we need to signal them that we want to
         * write a msg to them.
         */
        iWantYouToReadFromMe(destRpis);

        while (!destRpis.isEmpty()) {
            /*
             * Then, we need to wait for their approval, i.e. they will accept
             * an incoming message from us.
             *
             * Wait for K approvals.
             * 1 <= K <= destRpis.size().
             */
            gotAttention = waitForSomeRepliesFrom(destRpis);
            
            destRpis.removeAll(gotAttention);

            port.cacheManager.lock.lock();
            try {
                while (!gotAttention.isEmpty()) {
                    /*
                     * I can send the message only to the rpis from gotAttention.
                     * Any other rpi to which I'm currently connected
                     * cannot and will not receive this message I am 
                     * about to stream.
                     */
                    boolean heKnows = false;
                    for(ReceivePortIdentifier rpi : port.baseSendPort.connectedTo()) {
                        if(!gotAttention.contains(rpi)) {
                            port.cacheManager.cacheConnection(port.identifier(), 
                                    rpi, heKnows);
                        }
                    }
                    
                    /*
                     * Now connect (eventually) to all the rpis
                     * which will certainly read our message.
                     */
                    Set<ReceivePortIdentifier> connected =
                            port.cacheManager.getSomeConnections(
                            port, gotAttention, 0, false);

                    assert connected.size() > 0;

                    gotAttention.removeAll(connected);

                    /*
                     * Send the message to whoever is connected.
                     */
                    WriteMessage msg = port.baseSendPort.newMessage();
                    try {
                        noMsg++;
                        msg.writeBoolean(isLastPart);
                        msg.writeInt(index);
                        msg.writeArray(buffer, 0, index);
                        msg.finish();
                    } catch (IOException ex) {
                        msg.finish(ex);
                        Loggers.writeMsgLog.log(Level.SEVERE, "Failed to write {0} bytes message "
                                + "to {1} ports.\n", new Object[]{index, destRpis.size()});
                    }
                    Loggers.writeMsgLog.log(Level.INFO, "\tSent msg: ({0}, {1}) to {2}"
                            + " receive ports.\n",
                            new Object[]{isLastPart, index, port.baseSendPort.connectedTo().length});

                    /*
                     * If this was the last part of the streamed message, I
                     * don't have the receive ports' attention anymore.
                     */
                    if (isLastPart) {
                        yourLiveMessageIsNotMyConcern(connected);
                    }
                }
            } finally {
                port.cacheManager.lock.unlock();
            }
        }
        CacheManager.addStreamTime(System.currentTimeMillis()-start);
        /*
         * Buffer is sent to everyone. Clear it.
         */
        index = 0;
    }

    private void yourLiveMessageIsNotMyConcern(Set<ReceivePortIdentifier> rpis) {
        synchronized (yourReadMessageIsAliveFromMeSet) {
            for (ReceivePortIdentifier rpi : rpis) {
                yourReadMessageIsAliveFromMeSet.remove(rpi);
            }
        }
    }

    private void iWantYouToReadFromMe(Set<ReceivePortIdentifier> rpis) {
        synchronized (yourReadMessageIsAliveFromMeSet) {
            for (ReceivePortIdentifier rpi : rpis) {
                if (!yourReadMessageIsAliveFromMeSet.contains(rpi)) {
                    /*
                     * I have to let the receive port know that I want him to
                     * read my message.
                     */
                    port.cacheManager.sideChannelHandler.newThreadSendProtocol(
                            port.identifier(), rpi,
                            SideChannelProtocol.READ_MY_MESSAGE);
                }
            }
        }
    }

    private Set<ReceivePortIdentifier> waitForSomeRepliesFrom(Set<ReceivePortIdentifier> rpis) {
        /*
         * Need to wait for at most rpis.size() approvals.
         */
        Set<ReceivePortIdentifier> result;
        synchronized (yourReadMessageIsAliveFromMeSet) {
            result = new HashSet<ReceivePortIdentifier>(yourReadMessageIsAliveFromMeSet);
            result.retainAll(rpis);

            /*
             * Wait for at most a timeout,
             * because we can get deadlock:
             * 3 machines all behave the same: they all want to send a message
             * to the other 2;
             * they all send READ_MY_MESSAGE but the 3rd sends ack to the 2nd,
             * the 2nd send ack to the 1st and the 1st sends ack to the 3rd.
             * 
             *
             * so if we wait for 2 ack's back, we are stuck.
             */
            long defaultTimeout = 10; // millis
            long deadline = System.currentTimeMillis() + defaultTimeout;
            while (result.size() < rpis.size()) {
                try {
                    long timeout = deadline - System.currentTimeMillis();
                    if (timeout <= 0) {
                        /*
                         * Return if I have at least 1 guy to whom I can write
                         * my message. don't wait for all approvals now.
                         */
                        if (result.size() > 0) {
                            return result;
                        }

                        /*
                         * I need more time.
                         */
                        timeout = defaultTimeout;
                        deadline = System.currentTimeMillis() + defaultTimeout;
                    } else {
                        timeout = defaultTimeout;
                    }
                    yourReadMessageIsAliveFromMeSet.wait(timeout);
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
        Loggers.writeMsgLog.log(Level.INFO, "\n\tFlushed {0} intermediate messages to"
                + " {1} ports.\n", new Object[]{noMsg, port.connectedTo().length});
        noMsg = 0;
    }

    @Override
    public void close() throws IOException {
        Loggers.writeMsgLog.log(Level.INFO, "dataOut closing.");
        if (closed) {
            return;
        }
        closed = true;
        stream(true);
        Loggers.writeMsgLog.log(Level.INFO, "\n\tStreamed {0} intermediate messages to"
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
