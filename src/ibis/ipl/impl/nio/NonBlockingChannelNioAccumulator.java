/* $Id$ */

package ibis.ipl.impl.nio;

import ibis.ipl.impl.ReceivePortIdentifier;

import java.io.IOException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class NonBlockingChannelNioAccumulator extends NioAccumulator {

    private static Logger logger = LoggerFactory.getLogger(
            NonBlockingChannelNioAccumulator.class);

    private final Selector selector;

    public NonBlockingChannelNioAccumulator(NioSendPort port)
            throws IOException {
        super(port);
        selector = Selector.open();
    }

    NioAccumulatorConnection newConnection(GatheringByteChannel channel,
            ReceivePortIdentifier peer) throws IOException {
        NioAccumulatorConnection result;
        SelectableChannel sChannel = (SelectableChannel) channel;

        sChannel.configureBlocking(false);

        result = new NioAccumulatorConnection(port, channel, peer);
        result.key = sChannel.register(selector, 0);
        result.key.attach(result);

        return result;
    }

    /**
     * Sends out a buffer to multiple channels. First adds buffer to pending
     * buffers list, then sends out as much data as possible
     */
    boolean doSend(SendBuffer buffer) throws IOException {

        if (logger.isDebugEnabled()) {
            logger.debug("doSend()");
        }

        if (nrOfConnections == 0) {
            logger.error("not connected");
            return true;
        } else if (nrOfConnections == 1) {
            if (logger.isDebugEnabled()) {
                logger.debug("sending to 1 connection");
            }
            NioAccumulatorConnection connection = connections[0];
            try {
                if (!connection.addToSendList(buffer)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(
                                "add failed, making room and trying again");
                    }
                    doFlush();
                    connection.addToSendList(buffer);
                }

                connection.send();
            } catch (IOException e) {
                port.lostConnection(connection.target, e);

                nrOfConnections--;
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("sending to " + nrOfConnections + " connections");
            }

            SendBuffer[] copies = SendBuffer.replicate(buffer, nrOfConnections);

            for (int i = 0; i < nrOfConnections; i++) {
                NioAccumulatorConnection connection = connections[i];
                try {
                    if (!connection.addToSendList(copies[i])) {
                        doFlush(connection);
                        connection.addToSendList(copies[i]);
                    }
                    connection.send();
                } catch (IOException e) {
                    // inform the SendPort
                    port.lostConnection(connection.target, e);

                    // remove connection
                    nrOfConnections--;
                    connections[i] = connections[nrOfConnections];
                    connections[nrOfConnections] = null;
                    SendBuffer.recycle(copies[nrOfConnections]);
                    i--;
                }
            }
        }
        return false;
    }

    void doFlush() throws IOException {
        doFlush(null);
    }

    void doFlush(NioAccumulatorConnection connection) throws IOException {
        int nrOfSendingConnections = 0;
        NioAccumulatorConnection selected;
        boolean done = false;

        if (logger.isDebugEnabled()) {
            if (connection == null) {
                logger.debug("doing a complete flush");
            } else {
                logger.debug("doing a flush of a single connection");
            }
        }

        if (logger.isDebugEnabled() && connection != null) {
            boolean found = false;
            for (int i = 0; i < nrOfConnections; i++) {
                if (connections[i] == connection) {
                    found = true;
                }
            }
            if (!found) {
                throw new IOException("tried to flush non existing connection");
            }
        }

        // first try to send out data one more time, and remember
        // which connections still have data left
        for (int i = 0; i < nrOfConnections; i++) {
            NioAccumulatorConnection conn = connections[i];
            try {
                if (!conn.send()) {
                    conn.key.interestOps(SelectionKey.OP_WRITE);
                    nrOfSendingConnections++;
                } else {
                    if (conn == connection) {
                        done = true;
                    }
                    conn.key.interestOps(0);
                }
            } catch (IOException e) {
                port.lostConnection(conn.target, e);
                nrOfConnections--;
                connections[i] = connections[nrOfConnections];
                connections[nrOfConnections] = null;
                i--;
            }
        }


        if (done || (nrOfSendingConnections == 0)) {
            if (logger.isDebugEnabled()) {
                logger.debug("flush done");
            }
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("did one send for each connection" + ", "
                    + nrOfSendingConnections + " connections with data"
                    + " left");
        }

        // continually do a select and send data, until all data has been send
        while (!done) {
            selector.selectedKeys().clear();
            try {
                selector.select();
            } catch (IOException e) {
                // IGNORE
            }
            if (logger.isDebugEnabled()) {
                logger.debug("selected " + selector.selectedKeys().size()
                        + " channels");
            }

            for (SelectionKey key : selector.selectedKeys()) {
                selected = (NioAccumulatorConnection) key.attachment();

                try {
                    if (selected.send()) {
                        if (key.interestOps() == 0) {
                            throw new IOException("selected non-active channel");
                        }
                        key.interestOps(0);
                        nrOfSendingConnections--;

                        if (selected == connection
                                || nrOfSendingConnections == 0) {
                            done = true;
                            if (logger.isDebugEnabled()) {
                                logger.debug("done flushing a connection, "
                                        + nrOfSendingConnections + " left");
                            }
                        }
                    }
                } catch (IOException e) {
                    nrOfSendingConnections--;
                    for (int i = 0; i < nrOfConnections; i++) {
                        if (connection == connections[i]) {
                            port.lostConnection(connections[i].target, e);
                            nrOfConnections--;
                            connections[i] = connections[nrOfConnections];
                            connections[nrOfConnections] = null;
                            break;
                        }
                    }
                }
            }
        }

        if (logger.isDebugEnabled() && (connection == null)) {
            for (int i = 0; i < nrOfConnections; i++) {
                NioAccumulatorConnection conn = connections[i];
                if (!conn.empty()) {
                    throw new IOException("data left to send after doing flush");
                }
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("flush done");
        }
    }
}
