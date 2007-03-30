/* $Id: BlockingChannelNioAccumulator.java 2974 2005-04-29 15:30:11Z ceriel $ */

package ibis.impl.nio;

import ibis.impl.ReceivePortIdentifier;

import java.io.IOException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.SelectableChannel;

import org.apache.log4j.Logger;

final class BlockingChannelNioAccumulator extends NioAccumulator {

    private static Logger logger = Logger.getLogger(
            BlockingChannelNioAccumulator.class);

    public BlockingChannelNioAccumulator(NioSendPort port) {
        super(port);
    }

    NioAccumulatorConnection newConnection(GatheringByteChannel channel,
            ReceivePortIdentifier peer) throws IOException {
        NioAccumulatorConnection result;

        logger.debug("registering new connection");

        if (nrOfConnections != 0) {
            logger.warn("" + (nrOfConnections + 1)
                    + " connections from a blocking send port");
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

    void doFlush() throws IOException {
        // NOTHING
    }
}
