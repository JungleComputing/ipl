/* $Id$ */

package ibis.ipl.impl.nio;

import ibis.ipl.impl.ReceivePortIdentifier;

import java.io.IOException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.SelectableChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ThreadNioAccumulator extends NioAccumulator {

    static final int LOST_CONNECTION_SIZE = 8;

    private static Logger logger = LoggerFactory.getLogger(ThreadNioAccumulator.class);

    SendReceiveThread thread;

    ThreadNioAccumulator(NioSendPort port, SendReceiveThread thread) {
        super(port);
        this.thread = thread;
    }

    NioAccumulatorConnection newConnection(GatheringByteChannel channel,
            ReceivePortIdentifier peer) throws IOException {
        SelectableChannel sChannel = (SelectableChannel) channel;

        sChannel.configureBlocking(false);

        if (logger.isDebugEnabled()) {
            logger.debug("creating new" + " ThreadNioAccumulatorConnection");
        }

        return new ThreadNioAccumulatorConnection(port, thread, channel, peer);
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
                port.lostConnection(connection.target, e);
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
                    SendBuffer.recycle(copies[i]);
                    port.lostConnection(connection.target, e);
                    nrOfConnections--;
                    connections[i] = connections[nrOfConnections];
                    connections[nrOfConnections] = null;
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
