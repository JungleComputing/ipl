package ibis.impl.nio;

import java.io.IOException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.SelectionKey;

class NioAccumulatorConnection implements Config {
    static final int MAX_SEND_BUFFERS = 32;

    GatheringByteChannel channel;

    NioReceivePortIdentifier peer;

    //placeholder for the accumulator to put a selection key in
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
     * @return true if the add was succesfull, false if all the buffers are
     *	       full
     */
    boolean addToSendList(SendBuffer buffer) {
        if (full()) {
            return false;
        }

        pendingBuffers[bufferLimit] = buffer;

        if (DEBUG) {
            Debug.message("channels", this, "adding new buffer to send list"
                    + " at position " + bufferLimit);
        }
        bufferLimit = (bufferLimit + 1) % MAX_SEND_BUFFERS;

        return true;
    }

    /**
     * Send out data while it is possible to send without blocking.
     * Assumes non-blocking channel, recycles empty buffers.
     *
     * @return true if are we done sending, or false if there is more 
     * data in the buffer.
     */
    boolean send() throws IOException {
        long count;

        if (DEBUG) {
            Debug.enter("channels", this, "sending");
        }
        while (!empty()) {
            count = channel.write(pendingBuffers[bufferPosition].byteBuffers);

            if (DEBUG) {
                Debug.message("channels", this, "send " + count + " bytes");
            }

            if (pendingBuffers[bufferPosition].hasRemaining()) {
                if (DEBUG) {
                    Debug.exit("channels", this, "buffer has some bytes"
                            + " remaining");
                }
                return false;
            } else {
                SendBuffer.recycle(pendingBuffers[bufferPosition]);

                bufferPosition = (bufferPosition + 1) % MAX_SEND_BUFFERS;
                if (DEBUG) {
                    Debug.message("channels", this, "completely send buffer,"
                            + " trying next one too");
                }
            }
        }
        if (DEBUG) {
            Debug.exit("channels", this, "done sending");
        }
        return true;
    }

    void close() throws IOException {
        channel.close();
    }
}