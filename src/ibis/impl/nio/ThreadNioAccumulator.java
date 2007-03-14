/* $Id: ThreadNioAccumulator.java 3146 2005-08-29 11:58:55Z ceriel $ */

package ibis.impl.nio;

import ibis.impl.ReceivePortIdentifier;
import ibis.impl.SendPortConnectionInfo;

import java.io.IOException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.SelectableChannel;

import org.apache.log4j.Logger;

final class ThreadNioAccumulator extends NioAccumulator implements Config {

    static final int LOST_CONNECTION_SIZE = 8;

    private static Logger logger = Logger.getLogger(ThreadNioAccumulator.class);

    SendReceiveThread thread;

    SendPortConnectionInfo[] connections;

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

        connections = null;

        return new ThreadNioAccumulatorConnection(port, thread, channel, peer);
    }

    void removeConnection(SendPortConnectionInfo c) {
        connections = null;
    }

    boolean doSend(SendBuffer buffer) throws IOException {

        if (connections == null) {
            connections = port.connections();
        }

        if (logger.isDebugEnabled()) {
            logger.debug("doing send");
        }

        if (connections.length == 0) {
            if (logger.isInfoEnabled()) {
                logger.info("no connections to send to");
            }
            return true;
        } else if (connections.length == 1) {
            if (logger.isDebugEnabled()) {
                logger.debug("sending to one(1) connection");
            }
            ThreadNioAccumulatorConnection connection;
            connection = (ThreadNioAccumulatorConnection) connections[0];
            try {
                connection.addToThreadSendList(buffer);
            } catch (IOException e) {
                port.lostConnection(connection.target, e);
                connections = null;
                if (logger.isInfoEnabled()) {
                    logger.error("(only) connection lost");
                    return false; // don't do normal exit message
                }
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("sending to " + connections.length
                        + " connections");
            }

            SendBuffer[] copies = SendBuffer.replicate(buffer,
                    connections.length);

            int nrOfConnections = connections.length;
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
            if (nrOfConnections != connections.length) {
                connections = null;
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
