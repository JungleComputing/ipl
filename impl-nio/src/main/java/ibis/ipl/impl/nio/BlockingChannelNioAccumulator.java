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
import java.nio.channels.SelectableChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ibis.ipl.impl.ReceivePortIdentifier;

final class BlockingChannelNioAccumulator extends NioAccumulator {

    private static Logger logger = LoggerFactory.getLogger(BlockingChannelNioAccumulator.class);

    public BlockingChannelNioAccumulator(NioSendPort port) {
        super(port);
    }

    @Override
    NioAccumulatorConnection newConnection(GatheringByteChannel channel, ReceivePortIdentifier peer) throws IOException {
        NioAccumulatorConnection result;

        logger.debug("registering new connection");

        if (nrOfConnections != 0) {
            logger.warn("" + (nrOfConnections + 1) + " connections from a blocking send port");
        }

        SelectableChannel sChannel = (SelectableChannel) channel;

        sChannel.configureBlocking(true);

        result = new NioAccumulatorConnection(port, channel, peer);

        logger.debug("registered new connection");

        return result;
    }

    /**
     * Sends out a buffer to multiple channels. Doesn't buffer anything
     */
    @Override
    boolean doSend(SendBuffer buffer) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("sending a buffer");
        }

        buffer.mark();

        for (int i = 0; i < nrOfConnections; i++) {
            NioAccumulatorConnection connection = connections[i];
            try {
                buffer.reset();
                while (buffer.hasRemaining()) {
                    logger.debug("Write...");
                    connection.channel.write(buffer.byteBuffers);
                }
            } catch (IOException e) {
                // inform the SendPort
                logger.debug("lost connection", e);
                port.lostConnection(connection.target, e);
                // remove connection
                nrOfConnections--;
                connections[i] = connections[nrOfConnections];
                connections[nrOfConnections] = null;
                i--;
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("done sending a buffer");
        }
        return true; // signal we are done with the buffer now
    }

    @Override
    void doFlush() throws IOException {
        // NOTHING
    }
}
