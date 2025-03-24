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
import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ibis.ipl.ConnectionClosedException;
import ibis.ipl.MessageUpcall;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.ReceiveTimedOutException;
import ibis.ipl.impl.Ibis;
import ibis.ipl.impl.SendPortIdentifier;

final class NonBlockingChannelNioReceivePort extends NioReceivePort {
    static final int INITIAL_ARRAY_SIZE = 8;

    private static Logger logger = LoggerFactory.getLogger(NonBlockingChannelNioReceivePort.class);

    private NonBlockingChannelNioDissipator[] connections = new NonBlockingChannelNioDissipator[INITIAL_ARRAY_SIZE];

    private int nrOfConnections = 0;

    private NonBlockingChannelNioDissipator[] pendingConnections = new NonBlockingChannelNioDissipator[INITIAL_ARRAY_SIZE];

    private int nrOfPendingConnections = 0;

    private boolean closing = false;

    Selector selector;

    NonBlockingChannelNioReceivePort(Ibis ibis, PortType type, String name, MessageUpcall upcall, ReceivePortConnectUpcall connUpcall,
            Properties props) throws IOException {
        super(ibis, type, name, upcall, connUpcall, props);

        selector = Selector.open();
    }

    @Override
    synchronized void newConnection(SendPortIdentifier spi, Channel channel) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("registering new connection");
        }

        if (!(channel instanceof ReadableByteChannel)) {
            logger.error("wrong channel type");
            throw new IOException("wrong channel type on creating connection");
        }

        NonBlockingChannelNioDissipator dissipator = new NonBlockingChannelNioDissipator((ReadableByteChannel) channel);

        addConnection(spi, dissipator);

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
            logger.debug("waking up selector");
        }

        // wake up selector if needed
        selector.wakeup();

        if (logger.isDebugEnabled()) {
            logger.debug("registerred new connection");
        }
    }

    @Override
    synchronized void errorOnRead(NioDissipator dissipator, Exception cause) {

        logger.debug("lost connection", cause);

        for (int i = 0; i < nrOfPendingConnections; i++) {
            if (dissipator == pendingConnections[i]) {
                nrOfPendingConnections--;
                pendingConnections[i] = pendingConnections[nrOfPendingConnections];
                pendingConnections[nrOfPendingConnections] = null;
                logger.debug("lost connection removed from pending list");
                break;
            }
        }

        dissipator.info.close(cause);

        for (int i = 0; i < nrOfConnections; i++) {
            if (dissipator == connections[i]) {
                nrOfConnections--;
                connections[i] = connections[nrOfConnections];
                connections[nrOfConnections] = null;
                logger.debug("removed connection");
                break;
            }
        }

        if (nrOfConnections == 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("no more connections, waking up selector");
            }
            selector.wakeup();
        }

        if (logger.isDebugEnabled()) {
            logger.debug("removed connection");
        }
    }

    synchronized void registerPendingConnections() throws IOException {
        SelectableChannel sh;

        if (logger.isDebugEnabled() && nrOfPendingConnections > 0) {
            logger.debug("registerring " + nrOfPendingConnections + " connections");
        }

        for (int i = 0; i < nrOfPendingConnections; i++) {
            sh = (SelectableChannel) pendingConnections[i].channel;

            sh.register(selector, SelectionKey.OP_READ, pendingConnections[i]);
            pendingConnections[i] = null;
        }
        nrOfPendingConnections = 0;
    }

    @Override
    NioDissipator getReadyDissipator(long deadline) throws IOException {
        boolean deadlinePassed = false;
        boolean firstTry = true;
        long time;
        NonBlockingChannelNioDissipator dissipator = null;

        if (logger.isDebugEnabled()) {
            logger.debug("trying to find a dissipator" + " with a message waiting");
        }

        while (!deadlinePassed) {
            registerPendingConnections();

            synchronized (this) {
                if (nrOfConnections == 0) {
                    if (closing) {
                        if (logger.isInfoEnabled()) {
                            logger.info("exiting because we have no " + "connections (as requested)");
                        }
                        throw new ConnectionClosedException();
                    }
                    if (deadline == -1) {
                        deadlinePassed = true;
                        continue;
                    } else if (deadline == 0) {
                        try {
                            if (logger.isDebugEnabled()) {
                                logger.debug("wait()ing for a connection");
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
                                    logger.debug("wait()ing for a connection");
                                }
                                wait();
                            } catch (InterruptedException e) {
                                // ignored
                            }
                            continue;
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
                                logger.debug("returning connection " + i);
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
                    logger.debug("doing a selectNow");
                }
                try {
                    selector.selectNow();
                } catch (IOException e) {
                    // IGNORE
                }
                deadlinePassed = true;
            } else if (deadline == 0) {
                if (logger.isDebugEnabled()) {
                    logger.debug("doing a select() on " + selector.keys().size() + " connections");
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
                        logger.debug("doing a select(timeout)");
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
                logger.debug("selected " + selector.selectedKeys().size() + " connections");
            }

            for (SelectionKey key : selector.selectedKeys()) {
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
            logger.debug("deadline passed");
        }
        throw new ReceiveTimedOutException("timeout while waiting" + " for dissipator");
    }

    @Override
    synchronized void closing() {
        closing = true;
    }
}
