/* $Id$ */

package ibis.impl.nio;

import ibis.ipl.ConnectionClosedException;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.ReceiveTimedOutException;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.Upcall;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

import org.apache.log4j.Logger;

final class NonBlockingChannelNioReceivePort extends NioReceivePort implements
        Config {
    static final int INITIAL_ARRAY_SIZE = 8;

    static Logger logger = Logger
            .getLogger(NonBlockingChannelNioReceivePort.class.getName());

    private NonBlockingChannelNioDissipator[] connections;

    private int nrOfConnections = 0;

    private NonBlockingChannelNioDissipator[] pendingConnections;

    private int nrOfPendingConnections = 0;

    private boolean closing = false;

    Selector selector;

    NonBlockingChannelNioReceivePort(NioIbis ibis, NioPortType type,
            String name, Upcall upcall, boolean connectionAdministration,
            ReceivePortConnectUpcall connUpcall) throws IOException {
        super(ibis, type, name, upcall, connectionAdministration, connUpcall);

        connections = new NonBlockingChannelNioDissipator[INITIAL_ARRAY_SIZE];
        pendingConnections = new NonBlockingChannelNioDissipator[INITIAL_ARRAY_SIZE];

        selector = Selector.open();
    }

    synchronized void newConnection(NioSendPortIdentifier spi, Channel channel)
            throws IOException {
        NonBlockingChannelNioDissipator dissipator;

        if (logger.isDebugEnabled()) {
            logger.info("registering new connection");
        }

        if (!((channel instanceof ReadableByteChannel) && (channel instanceof SelectableChannel))) {
            if (logger.isDebugEnabled()) {
                logger.error("!wrong channel type");
            }
            throw new IOException("wrong type of channel on"
                    + " creating connection");
        }

        SelectableChannel sh = (SelectableChannel) channel;
        dissipator = new NonBlockingChannelNioDissipator(spi, ident,
                (ReadableByteChannel) channel, type);

        if (nrOfConnections == connections.length) {
            NonBlockingChannelNioDissipator[] newConnections;
            newConnections = new NonBlockingChannelNioDissipator[connections.length * 2];
            for (int i = 0; i < connections.length; i++) {
                newConnections[i] = connections[i];
            }
            connections = newConnections;
        }
        connections[nrOfConnections] = dissipator;
        nrOfConnections++;

        if (nrOfPendingConnections == pendingConnections.length) {
            NonBlockingChannelNioDissipator[] newPendingConnections;
            newPendingConnections = new NonBlockingChannelNioDissipator[pendingConnections.length * 2];

            for (int i = 0; i < pendingConnections.length; i++) {
                newPendingConnections[i] = pendingConnections[i];
            }
            pendingConnections = newPendingConnections;
        }
        pendingConnections[nrOfPendingConnections] = dissipator;
        nrOfPendingConnections++;

        if (logger.isDebugEnabled()) {
            logger.info("waking up selector");
        }

        // wake up selector if needed
        selector.wakeup();

        if (nrOfConnections == 1) {
            notifyAll();
        }

        if (logger.isDebugEnabled()) {
            logger.info("registerred new connection");
        }
    }

    synchronized void errorOnRead(NioDissipator dissipator, Exception cause) {
        if (logger.isDebugEnabled()) {
            logger.error("lost connection: " + cause);
        }

        for (int i = 0; i < nrOfPendingConnections; i++) {
            if (dissipator == pendingConnections[i]) {
                nrOfPendingConnections--;
                pendingConnections[i] = pendingConnections[nrOfPendingConnections];
                pendingConnections[nrOfPendingConnections] = null;
                if (logger.isDebugEnabled()) {
                    logger.info("lost connection removed from pending list");
                }
            }
        }

        for (int i = 0; i < nrOfConnections; i++) {
            if (dissipator == connections[i]) {
                try {
                    dissipator.reallyClose();
                } catch (IOException e) {
                    // IGNORE
                }
                connectionLost(dissipator, cause);
                nrOfConnections--;
                connections[i] = connections[nrOfConnections];
                connections[nrOfConnections] = null;

                if (nrOfConnections == 0) {
                    if (logger.isDebugEnabled()) {
                        logger.info("no more connections, waking up selector");
                    }
                    selector.wakeup();
                }
                if (logger.isDebugEnabled()) {
                    logger.info("removed connection");
                }
                return;
            }
        }
        if (logger.isDebugEnabled()) {
            logger.error("lost connection not found");
        }
    }

    synchronized void registerPendingConnections() throws IOException {
        SelectableChannel sh;

        if (logger.isDebugEnabled() && nrOfPendingConnections > 0) {
            logger.info("registerring " + nrOfPendingConnections
                    + " connections");
        }

        for (int i = 0; i < nrOfPendingConnections; i++) {
            sh = (SelectableChannel) pendingConnections[i].channel;

            sh.register(selector, SelectionKey.OP_READ, pendingConnections[i]);
            pendingConnections[i] = null;
        }
        nrOfPendingConnections = 0;
    }

    NioDissipator getReadyDissipator(long deadline) throws IOException {
        boolean deadlinePassed = false;
        boolean firstTry = true;
        long time;
        Iterator keys;
        SelectionKey key;
        NonBlockingChannelNioDissipator dissipator = null;

        if (logger.isDebugEnabled()) {
            logger.info("trying to find a dissipator"
                    + " with a message waiting");
        }

        while (!deadlinePassed) {
            registerPendingConnections();

            synchronized (this) {
                if (nrOfConnections == 0) {
                    if (closing) {
                        if (logger.isDebugEnabled()) {
                            logger.error("exiting because we have no "
                                    + "connections (as requested)");
                        }
                        throw new ConnectionClosedException();
                    } else {
                        if (deadline == -1) {
                            deadlinePassed = true;
                            continue;
                        } else if (deadline == 0) {
                            try {
                                if (logger.isDebugEnabled()) {
                                    logger.info("wait()ing for a connection");
                                }
                                wait();
                            } catch (InterruptedException e) {
                                // IGNORE
                            }
                            continue;
                        } else {
                            time = System.currentTimeMillis();
                            if (time >= deadline) {
                                deadlinePassed = true;
                            } else {
                                try {
                                    if (logger.isDebugEnabled()) {
                                        logger
                                                .info("wait()ing for a connection");
                                    }
                                    wait();
                                } catch (InterruptedException e) {
                                }
                                continue;
                            }
                        }
                    }
                }

                if (firstTry && nrOfConnections == 1) {
                    // optimisticly do a single receive, to avoid
                    // the select statement below if possible
                    try {
                        connections[0].readFromChannel();
                    } catch (IOException e) {
                        errorOnRead(connections[0], e);
                    }
                    firstTry = false;
                }

                for (int i = 0; i < nrOfConnections; i++) {
                    try {
                        if (connections[i].messageWaiting()) {
                            if (logger.isDebugEnabled()) {
                                logger.info("returning connection " + i);
                            }
                            return connections[i];
                        }
                    } catch (IOException e) {
                        errorOnRead(connections[i], e);
                        i--;
                    }
                }
            } // end of synchronized block

            if (deadline == -1) {
                if (logger.isDebugEnabled()) {
                    logger.info("doing a selectNow");
                }
                try {
                    selector.selectNow();
                } catch (IOException e) {
                    // IGNORE
                }
                deadlinePassed = true;
            } else if (deadline == 0) {
                if (logger.isDebugEnabled()) {
                    logger.info("doing a select() on " + selector.keys().size()
                            + " connections");
                }
                try {
                    selector.select();
                } catch (IOException e) {
                    logger.error("error on select: " + e);
                    // IGNORE
                }
            } else {
                time = System.currentTimeMillis();
                if (time >= deadline) {
                    deadlinePassed = true;
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.info("doing a select(timeout)");
                    }
                    try {
                        selector.select(deadline - time);
                    } catch (IOException e) {
                        logger.error("error on select: " + e);
                        // IGNORE
                    }
                }
            }

            if (logger.isDebugEnabled()) {
                logger.info("selected " + selector.selectedKeys().size()
                        + " connections");
            }

            keys = selector.selectedKeys().iterator();

            while (keys.hasNext()) {
                key = (SelectionKey) keys.next();
                dissipator = (NonBlockingChannelNioDissipator) key.attachment();

                try {
                    dissipator.readFromChannel();
                } catch (IOException e) {
                    errorOnRead(dissipator, e);
                }
            }
            selector.selectedKeys().clear();
        } // end of while(!deadlinePassed)
        if (logger.isDebugEnabled()) {
            logger.error("deadline passed");
        }
        throw new ReceiveTimedOutException("timeout while waiting"
                + " for dissipator");
    }

    synchronized public SendPortIdentifier[] connectedTo() {
        SendPortIdentifier[] result = new SendPortIdentifier[nrOfConnections];
        for (int i = 0; i < nrOfConnections; i++) {
            result[i] = connections[i].peer;
        }
        return result;
    }

    synchronized void closing() {
        closing = true;
    }

    synchronized void closeAllConnections() {
        for (int i = 0; i < nrOfConnections; i++) {
            try {
                connections[i].reallyClose();
            } catch (IOException e) {
                // IGNORE
            }
        }
    }
}
