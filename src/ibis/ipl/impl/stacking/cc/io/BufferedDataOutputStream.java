package ibis.ipl.impl.stacking.cc.io;

import ibis.io.Conversion;
import ibis.io.DataOutputStream;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.WriteMessage;
import ibis.ipl.impl.stacking.cc.CCSendPort;
import ibis.ipl.impl.stacking.cc.sidechannel.SideChannelProtocol;
import ibis.ipl.impl.stacking.cc.util.Loggers;
import ibis.ipl.impl.stacking.cc.util.Timers;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.logging.Level;

public class BufferedDataOutputStream extends DataOutputStream {
    /*
     * The send port which generates for me new WriteMessages. I need them so I
     * can ship my buffer to the various receive ports.
     */
    final CCSendPort port;
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
    public int index;
    /*
     * The buffer's capacity.
     */
    private final int capacity;
    /*
     * No of bytes written, not counting the ones currently in the buffer.
     */
    private int bytes;
    /*
     * Number of messages streamed at one flush.
     */
    private int noMsg;
    /*
     * When writing a message (all its streaming parts), need to make sure that
     * the receive port(s) have no other alive read message.
     */
    public final HashSet<ReceivePortIdentifier> yourReadMessageIsAliveFromMeSet;

    public BufferedDataOutputStream(CCSendPort port) {
        this.port = port;
        c = Conversion.loadConversion(false);
        this.index = 0;
        this.capacity = port.ccIbis.buffer_capacity;
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
        Loggers.writeMsgLog.log(Level.FINE, "Now streaming buffer size: {0}",
                index);
        Timers.streamTimer.start();

        /*
         * All of our destinations.
         */
        Set<ReceivePortIdentifier> destRpis = new HashSet<ReceivePortIdentifier>(
                Arrays.asList(port.connectedTo()));

        /*
         * We want the destined RPIs to have their live read message from us, so
         * as not to have interferance with other streaming messages.
         *
         * For this, either we have their attention from the 1st part of the
         * logical streamed message or we need to signal them that we want to
         * write a msg to them.
         */
        iWantYouToReadFromMe(destRpis);

        Loggers.ccLog.log(Level.INFO, "Waiting for replies from {0} recv ports...",
                destRpis.size());

        /*
         * Then, we need to wait for their approval, i.e. they will accept an
         * incoming message from us.
         */
        waitForAllRepliesFrom(destRpis);
        Loggers.ccLog.log(Level.INFO, "Got all replies.");

        port.ccManager.lock.lock();
        Loggers.lockLog.log(Level.INFO, "Lock locked for streaming.");
        byte one = 1, zero = 0;
        try {
            while (!destRpis.isEmpty()) {
                /*
                 * I need to wait for any reserved alive connection to be
                 * handled.
                 */
                Loggers.lockLog.log(Level.FINE, "Lock will be released:"
                        + " waiting on live reserved connections...");
                while (port.ccManager.containsReservedAlive(port.identifier())) {
                    try {
                        port.ccManager.reservationsCondition.await();
                    } catch (InterruptedException ignoreMe) {
                    }
                }
                Loggers.lockLog.log(Level.FINE, "Lock reaquired.");
                /*
                 * I can send the message only to the rpis from gotAttention.
                 * Any other rpi to which I'm currently connected cannot and
                 * will not receive this message I am about to stream.
                 */
                boolean heKnows = false;
                for (ReceivePortIdentifier rpi : port.baseSendPort.connectedTo()) {
                    if (!destRpis.contains(rpi)) {
                        port.ccManager.cacheConnection(port.identifier(),
                                rpi, heKnows);
                    }
                }

                /*
                 * Now connect to some rpis.
                 */
                Set<ReceivePortIdentifier> connected =
                        port.ccManager.getSomeConnections(
                        port, destRpis, 0, false);

                assert connected.size() > 0;

                destRpis.removeAll(connected);

                /*
                 * Send the message to whoever is connected.
                 */
                WriteMessage msg = port.baseSendPort.newMessage();
                try {
                    noMsg++;
                    msg.writeByte(isLastPart ? one : zero);
                    msg.writeByte((byte) ((index >>> 24) & 0xff));
                    msg.writeByte((byte) ((index >>> 16) & 0xff));
                    msg.writeByte((byte) ((index >>> 8) & 0xff));
                    msg.writeByte((byte) ((index) & 0xff));
                    if (index > 0) {
                        if (port.getPortType().hasCapability(PortType.SERIALIZATION_BYTE)) {
                            /*
                             * If serialization is byte only, then if we don't
                             * write the whole array it will throw an exception.
                             */
                            if (index > capacity / 2) {
                                msg.writeArray(buffer, 0, buffer.length);
                            } else {
                                byte[] temp = new byte[index];
                                System.arraycopy(buffer, 0, temp, 0, index);
                                msg.writeArray(temp, 0, temp.length);
                            }
                        } else {
                            msg.writeArray(buffer, 0, index);
                        }
                    }
                    msg.finish();
                } catch (IOException ex) {
                    msg.finish(ex);
                    Loggers.writeMsgLog.log(Level.SEVERE, "Failed to write {0} bytes message "
                            + "to {1} ports.\n", new Object[]{index, destRpis.size()});
                }
                Loggers.writeMsgLog.log(Level.INFO, "\tWrite msg finished. "
                        + "Sent: ({0}, {1}).\n",
                        new Object[]{isLastPart, index});

                /*
                 * If this was the last part of the streamed message, I don't
                 * have the receive ports' attention anymore.
                 */
                if (isLastPart) {
                    yourLiveMessageIsNotMyConcern(connected);
                }
            }
        } finally {
            Loggers.lockLog.log(Level.INFO, "Releasing lock in stream.");
            port.ccManager.lock.unlock();
        }
        Loggers.writeMsgLog.log(Level.INFO, "Streaming finished to all destined"
                + " rpis.");
        Timers.streamTimer.stop();
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

    private void iWantYouToReadFromMe(Set<ReceivePortIdentifier> rpis)
            throws IOException {

        List<ReceivePortIdentifier> rpisList = new ArrayList<ReceivePortIdentifier>();

        synchronized (yourReadMessageIsAliveFromMeSet) {
            for (ReceivePortIdentifier rpi : rpis) {
                if (!yourReadMessageIsAliveFromMeSet.contains(rpi)) {
                    rpisList.add(rpi);
                }
            }
            
            if(rpisList.isEmpty()) {
                return;
            }

            long[] seqNo;
            if (rpisList.size() > 1) {
                /*
                 * Get the next sequences for all these rpis.
                 */
                String[] rpiNames = new String[rpisList.size()];
                for (int i = 0; i < rpisList.size(); i++) {
                    rpiNames[i] = rpisList.get(i).toString();
                }

                seqNo = port.ccIbis.registry().getMultipleSequenceNumbers(rpiNames);

                for (int i = 0; i < rpiNames.length; i++) {
                    Loggers.ccLog.log(Level.FINE, "{0} has seqNo:\t{1}",
                            new Object[]{rpiNames[i], seqNo[i]});
                }
            } else {
                seqNo = new long[1];
                seqNo[0] = -1;
            }

            for (int i = 0; i < rpisList.size(); i++) {
                if (!yourReadMessageIsAliveFromMeSet.contains(rpisList.get(i))) {
                    /*
                     * I have to let the receive port know that I want him to
                     * read my message.
                     */
                    port.ccManager.sideChannelHandler.newThreadRMMProtocol(
                            port.identifier(), rpisList.get(i),
                            SideChannelProtocol.READ_MY_MESSAGE,
                            seqNo[i]);
                }
            }
        }
    }

    private void waitForAllRepliesFrom(Set<ReceivePortIdentifier> rpis) {
        /*
         * Need to wait for rpis.size() approvals.
         *
         * Here I have a critical deadlock, and kinda unsolvable.
         *
         * Scenario: 3 machines all behave the same: they all want to send a
         * message to the other 2; they all send READ_MY_MESSAGE but the 3rd
         * sends ack to the 2nd, the 2nd send ack to the 1st and the 1st sends
         * ack to the 3rd.
         *
         * so if we wait for 2 ack's back, we are stuck.
         *
         * But I need to wait for all the ack's, otherwise I cannot stream my
         * message.
         * 
         * 
         * later edit: solved it by implementing totally ordered multicasting.
         */
        Set<ReceivePortIdentifier> result;
        synchronized (yourReadMessageIsAliveFromMeSet) {
            result = new HashSet<ReceivePortIdentifier>(yourReadMessageIsAliveFromMeSet);
            result.retainAll(rpis);
            Loggers.ccLog.log(Level.FINEST, "RPs reading my msgs: {0}", yourReadMessageIsAliveFromMeSet);
            Loggers.ccLog.log(Level.FINEST, "Temporary result is: {0}", result);

            while (result.size() < rpis.size()) {
                try {
                    yourReadMessageIsAliveFromMeSet.wait();
                } catch (InterruptedException ignoreMe) {
                    // Gotcha!! -- pokemon style
                }

                result.clear();
                result.addAll(yourReadMessageIsAliveFromMeSet);
                result.retainAll(rpis);
                Loggers.ccLog.log(Level.FINEST, "RPs reading my msgs:: {0}", yourReadMessageIsAliveFromMeSet);
                Loggers.ccLog.log(Level.FINEST, "Temporary result is:: {0}", result);
            }
        }
    }

    @Override
    public void flush() throws IOException {
        stream(false);
        Loggers.writeMsgLog.log(Level.INFO, "\tFlushed {0} intermediate messages to"
                + " {1} ports.\n", new Object[]{noMsg, port.connectedTo().length});
        noMsg = 0;
    }

    @Override
    public void close() throws IOException {
        stream(true);
        Loggers.writeMsgLog.log(Level.INFO, "\tStreamed {0} intermediate messages to"
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
