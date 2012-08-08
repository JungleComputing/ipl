package ibis.ipl.impl.stacking.cache.io;

import ibis.ipl.ReadMessage;
import ibis.ipl.impl.stacking.cache.CacheReceivePort;
import ibis.ipl.impl.stacking.cache.util.Loggers;
import java.io.EOFException;
import java.io.IOException;
import java.util.logging.Level;

public class DowncallBufferedDataInputStream extends BufferedDataInputStream {

    public DowncallBufferedDataInputStream(CacheReceivePort port)
            throws IOException {
        super(port);
    }

    /**
     * This ensures that there are at least 'len' bytes in the buffer available
     * for reading.
     *
     * @param len
     * @throws IOException
     */
    @Override
    protected void requestFromBuffer(int len) throws IOException {
        assert index + buffered_bytes <= capacity;
        assert len <= capacity;

        /*
         * I have enough info for the guy. It's ok.
         */
        if (buffered_bytes >= len) {
            return;
        }

        if (buffered_bytes == 0) {
            index = 0;
        } else if (index + buffered_bytes + len > capacity) {
            // not enough space for "len" more bytes
            // move index to 0, and we have enough space.
            System.arraycopy(buffer, index, buffer, 0, buffered_bytes);
            index = 0;
        }
        /*
         * Fill up the buffer with some data from the currentMsg, but at most
         * what currentMsg has left.
         */
        while (buffered_bytes < len) {
            if (remainingBytes <= 0) {
                /*
                 * The current message is depleted.
                 */
                currentBaseMsg.finish();
                if (isLastPart) {
                    throw new EOFException("Requiring more"
                            + " data after depleting the last streamed buffer.");
                } else {
                    /*
                     * Get the next partial message to read from it.
                     */
                    currentBaseMsg = port.recvPort.receive();
                    /*
                     * Read my protocol.
                     */
                    isLastPart = currentBaseMsg.readBoolean();
                    remainingBytes = currentBaseMsg.readInt();
                    Loggers.readMsgLog.log(Level.INFO, "Got a message: isLastPart={1}, size={0}",
                            new Object[]{remainingBytes, isLastPart});
                }
            }
            /*
             * I have at least some remaining bytes from which to read from.
             */
            int n = Math.min(capacity - (index + buffered_bytes), remainingBytes);
            currentBaseMsg.readArray(buffer, index + buffered_bytes, n);
            buffered_bytes += n;
            bytes += n;
            remainingBytes -= n;
        }
    }

    @Override
    public void finish() throws IOException {
        /*
         * The sender may have streamed N intermediate messages, but the
         * receiver does a close much sooner. Need to pull out the rest of the
         * messages.
         */
        int skipped = 0;
        while (!isLastPart) {
            currentBaseMsg.finish();
            /*
             * Drain the next partial message.
             */
            currentBaseMsg = port.recvPort.receive();
            isLastPart = currentBaseMsg.readBoolean();
            remainingBytes = currentBaseMsg.readInt();
            Loggers.readMsgLog.log(Level.FINE, "Skipping message: isLastPart={0},"
                    + " bufSize={1}.", new Object[] {isLastPart, remainingBytes});
            skipped++;
        }
        currentBaseMsg.finish();
        Loggers.readMsgLog.log(Level.INFO, "Closed dataIn and pulled out {0}"
                + " skipped messages.", skipped);
    }

    @Override
    public void offerToBuffer(boolean isLastPart, int remaining, ReadMessage msg) {
        throw new UnsupportedOperationException("DowncallBufferedDataInputStream"
                + " feeds itself explicitly on received data.");
    }

    @Override
    public void close() throws IOException {
    }
}
