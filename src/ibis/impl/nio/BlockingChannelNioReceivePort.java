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

import org.apache.log4j.Logger;

final class BlockingChannelNioReceivePort extends NioReceivePort implements
        Config {
    static final int INITIAL_DISSIPATOR_SIZE = 8;

    private static Logger logger = Logger
            .getLogger(BlockingChannelNioReceivePort.class);

    private BlockingChannelNioDissipator[] connections;

    private int nrOfConnections = 0;

    private boolean closing = false;

    BlockingChannelNioReceivePort(NioIbis ibis, NioPortType type, String name,
            Upcall upcall, boolean connectionAdministration,
            ReceivePortConnectUpcall connUpcall) throws IOException {
        super(ibis, type, name, upcall, connectionAdministration, connUpcall);

        connections = new BlockingChannelNioDissipator[INITIAL_DISSIPATOR_SIZE];
    }

    synchronized void newConnection(NioSendPortIdentifier spi, Channel channel)
            throws IOException {

        if (!((channel instanceof ReadableByteChannel) && (channel instanceof SelectableChannel))) {
            throw new IOException("wrong type of channel on"
                    + " creating connection");
        }

        if (nrOfConnections == connections.length) {
            BlockingChannelNioDissipator[] newConnections;
            newConnections = new BlockingChannelNioDissipator[connections.length * 2];
            for (int i = 0; i < connections.length; i++) {
                newConnections[i] = connections[i];
            }
            connections = newConnections;
        }
        connections[nrOfConnections] = new BlockingChannelNioDissipator(spi,
                ident, (ReadableByteChannel) channel, type);
        nrOfConnections++;

        if (nrOfConnections > 1) {
            logger.warn("" + nrOfConnections + " connections to a `"
                    + type.name()
                    + "` blocking receiveport, added connection from " + spi
                    + " to " + ident);
        }

        if (nrOfConnections == 1) {
            notifyAll();
        }

    }

    synchronized void errorOnRead(NioDissipator dissipator, Exception cause) {
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
                return;
            }
        }
    }

    NioDissipator getReadyDissipator(long deadline) throws IOException {

        Selector selector;
        long time;
        boolean deadlinePassed = false;
        BlockingChannelNioDissipator dissipator = null;
        SelectionKey key;
        SelectionKey[] keys = new SelectionKey[0];

        synchronized (this) {
            if (nrOfConnections == 0 && closing) {
                throw new ConnectionClosedException();
            }
            for (int i = 0; i < nrOfConnections; i++) {
                try {
                    if (connections[i].messageWaiting()) {
                        return connections[i];
                    }
                } catch (IOException e) {
                    errorOnRead(connections[i], e);
                    i--;
                }
            }
            if (nrOfConnections == 1 && !type.manyToOne && deadline == 0) {
                dissipator = connections[0];
            }
        }

        // since we have only one connection, and no more are allowed, and
        // we can wait for ever for data we just do a blocking
        // receive here on the one channel
        try {
            while (dissipator != null && !dissipator.messageWaiting()) {
                if (dissipator.available() == 0) {
                    try {
                        dissipator.receive();
                    } catch (IOException e) {
                        errorOnRead(dissipator, e);
                        dissipator = null;
                        break;
                    }
                }
            }
        } catch (IOException e) {
            errorOnRead(dissipator, e);
            dissipator = null;
        }
        if (dissipator != null) {
            // message waiting now
            return dissipator;
        }

        while (!deadlinePassed) {
            synchronized (this) {
                if (nrOfConnections == 0) {
                    if (closing) {
                        throw new ConnectionClosedException();
                    } else {
                        if (deadline == -1) {
                            deadlinePassed = true;
                            continue;
                        } else if (deadline == 0) {
                            try {
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
                                    wait();
                                } catch (InterruptedException e) {
                                    // IGNORE
                                }
                                continue;
                            }
                        }
                    }
                }

                selector = Selector.open();
                for (int i = 0; i < nrOfConnections; i++) {
                    SelectableChannel sh;
                    sh = (SelectableChannel) connections[i].channel;
                    sh.configureBlocking(false);
                    sh.register(selector, SelectionKey.OP_READ, connections[i]);
                }
            }

            try {
                if (deadline == -1) {
                    selector.selectNow();
                    deadlinePassed = true;
                } else if (deadline == 0) {
                    selector.select();
                } else {
                    time = System.currentTimeMillis();
                    if (time >= deadline) {
                        deadlinePassed = true;
                    } else {
                        selector.select(deadline - time);
                    }
                }
            } catch (IOException e) {
                // FIXME: is this a good idea?
                // IGNORE
            }

            keys = (SelectionKey[]) selector.selectedKeys().toArray(keys);

            selector.close();

            synchronized (this) {
                for (int i = 0; i < nrOfConnections; i++) {
                    SelectableChannel sh;
                    sh = (SelectableChannel) connections[i].channel;
                    sh.configureBlocking(true);
                }
            }

            for (int i = 0; i < keys.length; i++) {
                dissipator = (BlockingChannelNioDissipator) keys[i]
                        .attachment();
                SelectableChannel sh;
                sh = (SelectableChannel) dissipator.channel;
                sh.configureBlocking(true);
                try {
                    dissipator.readFromChannel();
                } catch (IOException e) {
                    errorOnRead(dissipator, e);
                }
            }

            synchronized (this) {
                if (nrOfConnections == 0 && closing) {
                    throw new ConnectionClosedException();
                }
                for (int i = 0; i < nrOfConnections; i++) {
                    try {
                        if (connections[i].messageWaiting()) {
                            return connections[i];
                        }
                    } catch (IOException e) {
                        errorOnRead(connections[i], e);
                        i--;
                    }
                }
            }
        }
        throw new ReceiveTimedOutException("timeout while selecting"
                + " dissipator");
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
