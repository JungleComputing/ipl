/* $Id$ */

package ibis.impl.nio;

import java.io.IOException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.SelectionKey;

import org.apache.log4j.Logger;

class NioAccumulatorConnection implements Config {
    static final int MAX_SEND_BUFFERS = 32;

    private static Logger logger = ibis.util.GetLogger
            .getLogger(NioAccumulatorConnection.class);

    GatheringByteChannel channel;

    NioReceivePortIdentifier peer;

    // placeholder for the accumulator to put a selection key in
    SelectionKey key;

    SendBuffer[] pendingBuffers;

    int bufferPosition = 0;

    int bufferLimit = 0;

    NioAccumulatorConnection(GatheringByteChannel channel,
            NioReceivePortIdentifier peer) {
        this.channel = channel;
        this.peer = peer;

        pendingBuffers = new SendBuffer[MAX_SEND_BUFFERS];
    }

    boolean full() {
        return ((bufferLimit + 1) % MAX_SEND_BUFFERS) == bufferPosition;
    }

    boolean empty() {
        return bufferLimit == bufferPosition;
    }

    /**
     * Adds given buffer to list of buffer which will be send out.
     * 
     * @return true if the add was succesfull, false if all the buffers are full
     */
    boolean addToSendList(SendBuffer buffer) {
        if (full()) {
            return false;
        }

        pendingBuffers[bufferLimit] = buffer;

        if (logger.isDebugEnabled()) {
            logger.debug("adding new buffer to send list" + " at position "
                    + bufferLimit);
        }
        bufferLimit = (bufferLimit + 1) % MAX_SEND_BUFFERS;

        return true;
    }

    /**
     * Send out data while it is possible to send without blocking. Assumes
     * non-blocking channel, recycles empty buffers.
     * 
     * @return true if are we done sending, or false if there is more data in
     *         the buffer.
     */
    boolean send() throws IOException {
        long count;

        if (logger.isDebugEnabled()) {
            logger.debug("sending");
        }
        while (!empty()) {
            count = channel.write(pendingBuffers[bufferPosition].byteBuffers);

            if (logger.isDebugEnabled()) {
                logger.debug("send " + count + " bytes");
            }

            if (pendingBuffers[bufferPosition].hasRemaining()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("buffer has some bytes" + " remaining");
                }
                return false;
            }
            SendBuffer.recycle(pendingBuffers[bufferPosition]);
            
            bufferPosition = (bufferPosition + 1) % MAX_SEND_BUFFERS;
            if (logger.isDebugEnabled()) {
            	logger.debug("completely send buffer,"
            			+ " trying next one too");
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("done sending");
        }
        return true;
    }

    void close() throws IOException {
        channel.close();
    }
}
