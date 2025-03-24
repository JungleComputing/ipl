/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* $Id$ */

package ibis.ipl.impl.nio;

import java.io.IOException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.SelectionKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ibis.ipl.impl.ReceivePortIdentifier;
import ibis.ipl.impl.SendPortConnectionInfo;

class NioAccumulatorConnection extends SendPortConnectionInfo {
    static final int MAX_SEND_BUFFERS = 32;

    private static Logger logger = LoggerFactory.getLogger(NioAccumulatorConnection.class);

    GatheringByteChannel channel;

    SendBuffer[] pendingBuffers;

    int bufferPosition = 0;

    int bufferLimit = 0;

    // placeholder for the accumulator to put a selection key in
    SelectionKey key;

    NioAccumulatorConnection(NioSendPort port, GatheringByteChannel channel, ReceivePortIdentifier peer) {
        super(port, peer);
        pendingBuffers = new SendBuffer[MAX_SEND_BUFFERS];
        this.channel = channel;
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
            logger.debug("adding new buffer to send list" + " at position " + bufferLimit);
        }
        bufferLimit = (bufferLimit + 1) % MAX_SEND_BUFFERS;

        return true;
    }

    /**
     * Send out data while it is possible to send without blocking. Assumes
     * non-blocking channel, recycles empty buffers.
     *
     * @return true if are we done sending, or false if there is more data in the
     *         buffer.
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
                logger.debug("completely send buffer," + " trying next one too");
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("done sending");
        }
        return true;
    }

    @Override
    public void closeConnection() {
        try {
            channel.close();
        } catch (Throwable e) {
            // ignored
        }
    }
}
