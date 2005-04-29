/* $Id$ */

package ibis.impl.nio;

import java.io.IOException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.SelectableChannel;

import org.apache.log4j.Logger;

final class BlockingChannelNioAccumulator extends NioAccumulator {

    private static Logger logger = ibis.util.GetLogger
            .getLogger(BlockingChannelNioAccumulator.class);

    private final NioSendPort port;

    public BlockingChannelNioAccumulator(NioSendPort port) {
        super();
        this.port = port;
    }

    NioAccumulatorConnection newConnection(GatheringByteChannel channel,
            NioReceivePortIdentifier peer) throws IOException {
        NioAccumulatorConnection result;

        logger.debug("registering new connection");

        if ((nrOfConnections + 1) > 1) {
            logger.warn("" + (nrOfConnections + 1) + " connections from a `"
                    + port.type.name() + "` blocking send port");
        }

        SelectableChannel sChannel = (SelectableChannel) channel;

        sChannel.configureBlocking(true);

        result = new NioAccumulatorConnection(channel, peer);

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
            try {
                buffer.reset();
                while (buffer.hasRemaining()) {
                    connections[i].channel.write(buffer.byteBuffers);
                }
            } catch (IOException e) {
                // someting went wrong, close connection
                connections[i].close();

                // inform the SendPort
                port.lostConnection(connections[i].peer, e);

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
