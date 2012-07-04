package ibis.ipl.impl.stacking.cache;

import ibis.io.Conversion;
import ibis.ipl.ReadMessage;
import java.io.IOException;

public class DowncallBufferedDataInputStream extends BufferedDataInputStream {

    DowncallBufferedDataInputStream(ReadMessage msg, CacheReceivePort port) 
            throws IOException {
        super(port);

        super.currentMsg = msg;
        super.lastPart = msg.readBoolean();
        super.remainingBytes = msg.readInt();
    }

    /**
     * This ensures that there are at least 'len' bytes in the buffer available
     * for reading.
     *
     * @param len
     * @throws IOException
     */
    @Override
    protected void fillBuffer(int len) throws IOException {
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
                if (lastPart) {
                    /*
                     * If it was also the last part of the message, then I have
                     * no more to offer.
                     */
                    throw new java.io.EOFException("EOF encountered");
                }
                /*
                 * Get the next partial message to read from it.
                 */
                currentMsg = port.recvPort.receive();
                /*
                 * Read my protocol.
                 */
                lastPart = currentMsg.readBoolean();
                remainingBytes = currentMsg.readInt();
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
        currentMsg.finish();
    }

    @Override
    protected void offer(boolean isLastPart, ReadMessage msg) {
        throw new UnsupportedOperationException("DowncallBufferedDataInputStream"
                + " feeds itself explicitly on received data.");
    }
}
