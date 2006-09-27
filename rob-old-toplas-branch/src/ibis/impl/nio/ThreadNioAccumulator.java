/* $Id$ */

package ibis.impl.nio;

import java.io.IOException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.SelectableChannel;

import org.apache.log4j.Logger;

final class ThreadNioAccumulator extends NioAccumulator implements Config {

    static final int LOST_CONNECTION_SIZE = 8;

    private static Logger logger = ibis.util.GetLogger.getLogger(ThreadNioAccumulator.class);

    NioSendPort port;

    SendReceiveThread thread;

    ThreadNioAccumulator(NioSendPort port, SendReceiveThread thread) {
        super();
        this.port = port;
        this.thread = thread;
    }

    NioAccumulatorConnection newConnection(GatheringByteChannel channel,
            NioReceivePortIdentifier peer) throws IOException {
        SelectableChannel sChannel = (SelectableChannel) channel;

        sChannel.configureBlocking(false);

        if (logger.isDebugEnabled()) {
            logger.debug("creating new" + " ThreadNioAccumulatorConnection");
        }

        return new ThreadNioAccumulatorConnection(thread, channel, peer);
    }

    boolean doSend(SendBuffer buffer) throws IOException {

        if (logger.isDebugEnabled()) {
            logger.debug("doing send");
        }

        if (nrOfConnections == 0) {
            if (logger.isInfoEnabled()) {
                logger.info("no connections to send to");
            }
            return true;
        } else if (nrOfConnections == 1) {
            if (logger.isDebugEnabled()) {
                logger.debug("sending to one(1) connection");
            }
            ThreadNioAccumulatorConnection connection;
            connection = (ThreadNioAccumulatorConnection) connections[0];
            try {
                connection.addToThreadSendList(buffer);
            } catch (IOException e) {
                connection.close();
                port.lostConnection(connection.peer, e);
                connections[0] = null;
                nrOfConnections = 0;
                if (logger.isInfoEnabled()) {
                    logger.error("(only) connection lost");
                    return false; // don't do normal exit message
                }
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("sending to " + nrOfConnections + " connections");
            }

            SendBuffer[] copies = SendBuffer.replicate(buffer, nrOfConnections);

            for (int i = 0; i < nrOfConnections; i++) {
                ThreadNioAccumulatorConnection connection;
                connection = (ThreadNioAccumulatorConnection) connections[i];

                try {
                    connection.addToThreadSendList(copies[i]);
                } catch (IOException e) {
                    if (logger.isInfoEnabled()) {
                        logger.info("connection lost");
                    }
                    connection.close();
                    port.lostConnection(connection.peer, e);
                    nrOfConnections--;
                    connections[i] = connections[nrOfConnections];
                    connections[nrOfConnections] = null;
                    SendBuffer.recycle(copies[nrOfConnections]);
                    i--;
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("done send");
        }
        return false;
    }

    void doFlush() throws IOException {
        /*
         * if (logger.isDebugEnabled()) { logger.info("buffers", this, "doing
         * flush"); } for (int i = 0; i < nrOfConnections; i++) {
         * ((ThreadNioAccumulatorConnection) connections[i]).waitUntilEmpty(); }
         * if (logger.isDebugEnabled()) { logger.info("buffers", this, "done
         * flush"); }
         */
    }
}
