package ibis.ipl.impl.stacking.cc.io;

import ibis.ipl.ReadMessage;
import ibis.ipl.impl.stacking.cc.CCReadMessage;
import ibis.ipl.impl.stacking.cc.CCReceivePort;
import java.io.EOFException;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DowncallBufferedDataInputStream extends BufferedDataInputStream {
    
    protected final static Logger logger = 
            LoggerFactory.getLogger(DowncallBufferedDataInputStream.class);
    
    public DowncallBufferedDataInputStream(CCReceivePort port)
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
                try {
                    if (!currentBaseMsgFinished) {
                        currentBaseMsgFinished = true;
                        currentBaseMsg.finish();
                    }
                } catch (Exception ex) {
                    logger.debug("Tried to close the base read message.", ex);
                }
                if (isLastPart) {
                    throw new EOFException("Requiring more"
                            + " data after depleting the last streamed buffer.");
                } else {
                    /*
                     * Get the next partial message to read from it.
                     */
                    currentBaseMsg = recvPort.recvPort.receive();
                    currentBaseMsgFinished = false;
                    /*
                     * Read my protocol.
                     */
                    isLastPart = currentBaseMsg.readByte() == 1 ? true : false;
                    remainingBytes = CCReadMessage.readIntFromBytes(currentBaseMsg);
                    logger.debug("Got a message: isLastPart={}, size={}",
                            new Object[]{remainingBytes, isLastPart});
                }
            }
            /*
             * I have at least some remaining bytes from which to read from.
             */
            int n = Math.min(capacity - (index + buffered_bytes), remainingBytes);
            /*
             * When having BYTE_SERIALIZATION, if we don't read in the entire
             * array, we get an exception.
             */
            byte[] temp = new byte[n];
            currentBaseMsg.readArray(temp, 0, temp.length);
            System.arraycopy(temp, 0, buffer, index + buffered_bytes, n);
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
            try {
                if (!currentBaseMsgFinished) {
                    currentBaseMsgFinished = true;
                    currentBaseMsg.finish();                    
                }
            } catch (Exception ex) {
                logger.debug("Tried to close the base read message.", ex);
            }
            /*
             * Drain the next partial message.
             */
            currentBaseMsg = recvPort.recvPort.receive();
            currentBaseMsgFinished = false;
            isLastPart = currentBaseMsg.readByte() == 1 ? true : false;
            remainingBytes = CCReadMessage.readIntFromBytes(currentBaseMsg);
            logger.debug("Skipping message: isLastPart={},"
                    + " bufSize={}.", new Object[] {isLastPart, remainingBytes});
            skipped++;
        }
        if(!currentBaseMsgFinished) {
            currentBaseMsgFinished = true;
            currentBaseMsg.finish();
        }
        logger.debug("Closed dataIn and pulled out {}"
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
