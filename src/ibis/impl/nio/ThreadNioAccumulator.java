/* $Id$ */

package ibis.impl.nio;

import java.io.IOException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.SelectableChannel;

import org.apache.log4j.Logger;

final class ThreadNioAccumulator extends NioAccumulator implements Config {

    static final int LOST_CONNECTION_SIZE = 8;

    static Logger logger = Logger.getLogger(ThreadNioAccumulator.class
            .getName());

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
            logger.info("creating new" + " ThreadNioAccumulatorConnection");
        }

        return new ThreadNioAccumulatorConnection(thread, channel, peer);
    }

    boolean doSend(SendBuffer buffer) throws IOException {
        SendBuffer copy;

        if (logger.isDebugEnabled()) {
            logger.info("doing send");
        }

        if (nrOfConnections == 0) {
            if (logger.isDebugEnabled()) {
                logger.error("no connections to send to");
            }
            return true;
        } else if (nrOfConnections == 1) {
            if (logger.isDebugEnabled()) {
                logger.info("sending to one(1) connection");
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
                if (logger.isDebugEnabled()) {
                    logger.error("(only) connection lost");
                    return false; // don't do normal exit message
                }
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.info("sending to " + nrOfConnections + " connections");
            }

            SendBuffer[] copies = SendBuffer.replicate(buffer, nrOfConnections);

            for (int i = 0; i < nrOfConnections; i++) {
                ThreadNioAccumulatorConnection connection;
                connection = (ThreadNioAccumulatorConnection) connections[i];

                try {
                    connection.addToThreadSendList(copies[i]);
                } catch (IOException e) {
                    if (logger.isDebugEnabled()) {
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
            logger.info("done send");
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