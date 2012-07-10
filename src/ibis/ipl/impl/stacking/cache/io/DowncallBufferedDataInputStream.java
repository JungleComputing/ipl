package ibis.ipl.impl.stacking.cache.io;

import ibis.ipl.ReadMessage;
import ibis.ipl.impl.stacking.cache.CacheReceivePort;
import ibis.ipl.impl.stacking.cache.manager.CacheManager;
import java.io.EOFException;
import java.io.IOException;
import java.util.logging.Level;

public class DowncallBufferedDataInputStream extends BufferedDataInputStream {

    private boolean isLastPart;

    public DowncallBufferedDataInputStream(ReadMessage m, CacheReceivePort port)
            throws IOException {
        super(port);

        super.currentMsg = m;
        this.isLastPart = m.readBoolean();
        super.remainingBytes = m.readInt();

        CacheManager.log.log(Level.INFO, "Got a msg: isLastPart={1}, size={0}",
                new Object[] {remainingBytes, isLastPart});
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
                currentMsg.finish();
                if (isLastPart) {
                    throw new EOFException("Requiring more"
                            + " data after depleting the last streamed buffer.");
                } else {
                    /*
                     * Get the next partial message to read from it.
                     */
                    currentMsg = port.recvPort.receive();
                    /*
                     * Read my protocol.
                     */
                    isLastPart = currentMsg.readBoolean();
                    remainingBytes = currentMsg.readInt();
                    CacheManager.log.log(Level.INFO, "Got a message: isLastPart={1}, size={0}",
                            new Object[]{remainingBytes, isLastPart});
                }
            }
            /*
             * I have at least some remaining bytes from which to read from.
             */
            int n = Math.min(capacity - (index + buffered_bytes), remainingBytes);
            currentMsg.readArray(buffer, index + buffered_bytes, n);
            buffered_bytes += n;
            bytes += n;
            remainingBytes -= n;
        }
    }

    @Override
    public void close() throws IOException {
        /*
         * The sender may have streamed N intermediate messages, but the
         * receiver does a close much sooner. Need to pull out the rest of the
         * messages.
         */
        int skipped = 0;
        while (!isLastPart) {
            currentMsg.finish();
            /*
             * Drain the next partial message.
             */
            currentMsg = port.recvPort.receive();
            isLastPart = currentMsg.readBoolean();
            skipped++;
        }
        currentMsg.finish();
        CacheManager.log.log(Level.INFO, "Closed dataIn and pulled out {0}"
                + " skipped messages.", skipped);
    }

    @Override
    public void offerToBuffer(boolean isLastPart, ReadMessage msg) {
        throw new UnsupportedOperationException("DowncallBufferedDataInputStream"
                + " feeds itself explicitly on received data.");
    }
}
