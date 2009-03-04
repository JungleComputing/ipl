/* $Id$ */

package ibis.ipl.impl.nio;

import ibis.ipl.ConnectionClosedException;
import ibis.ipl.MessageUpcall;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.ReceiveTimedOutException;
import ibis.ipl.impl.Ibis;
import ibis.ipl.impl.SendPortIdentifier;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BlockingChannelNioReceivePort extends NioReceivePort {
    static final int INITIAL_DISSIPATOR_SIZE = 8;

    private static Logger logger
            = LoggerFactory.getLogger(BlockingChannelNioReceivePort.class);

    private boolean closing = false;

    private BlockingChannelNioDissipator[] connections
        = new BlockingChannelNioDissipator[INITIAL_DISSIPATOR_SIZE];

    private int nrOfConnections = 0;

    BlockingChannelNioReceivePort(Ibis ibis, PortType type, String name,
            MessageUpcall upcall, ReceivePortConnectUpcall connUpcall,
            Properties properties) throws IOException {
        super(ibis, type, name, upcall, connUpcall, properties);
    }

    synchronized void newConnection(SendPortIdentifier spi, Channel channel)
            throws IOException {

        if (!(channel instanceof ReadableByteChannel)) {
            throw new IOException("wrong channel type  on creating connection");
        }

        BlockingChannelNioDissipator dissipator
            = new BlockingChannelNioDissipator((ReadableByteChannel) channel);

        addConnection(spi, dissipator);

        if (nrOfConnections == connections.length) {
            BlockingChannelNioDissipator[] newConnections;
            newConnections = new BlockingChannelNioDissipator[connections.length * 2];
            for (int i = 0; i < connections.length; i++) {
                newConnections[i] = connections[i];
            }
            connections = newConnections;
        }
        connections[nrOfConnections] = dissipator;
        nrOfConnections++;

        if (nrOfConnections > 1) {
            logger.warn("" + nrOfConnections + " connections to a "
                    + "blocking receiveport, added connection from " + spi
                    + " to " + ident);
        }
    }

    synchronized void errorOnRead(NioDissipator dissipator, Exception cause) {
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
    }

    NioDissipator getReadyDissipator(long deadline) throws IOException {

        Selector selector;
        long time;
        boolean deadlinePassed = false;
        BlockingChannelNioDissipator dissipator = null;
        SelectionKey[] keys = new SelectionKey[0];

        synchronized (this) {
            if (nrOfConnections == 0 && closing) {
                throw new ConnectionClosedException();
            }
            for (int i = 0; i < nrOfConnections; i++) {
                BlockingChannelNioDissipator conn = connections[i];
                try {
                    if (conn.messageWaiting()) {
                        return conn;
                    }
                } catch (IOException e) {
                    errorOnRead(conn, e);
                    i--;
                }
            }
            if (nrOfConnections == 1
                    && type.hasCapability(PortType.CONNECTION_ONE_TO_ONE)
                    && deadline == 0) {
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
                    }
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

                selector = Selector.open();
                for (int i = 0; i < nrOfConnections; i++) {
                    SelectableChannel sh;
                    sh = (SelectableChannel) connections[i].channel;
                    sh.configureBlocking(false);
                    sh.register(selector, SelectionKey.OP_READ,
                            connections[i]);
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
                logger.debug("Got IOException", e);
            }

            keys = selector.selectedKeys().toArray(keys);

            selector.close();

            synchronized (this) {
                for (int i = 0; i < nrOfConnections; i++) {
                    SelectableChannel sh;
                    sh = (SelectableChannel)connections[i].channel;
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

    synchronized void closing() {
        closing = true;
    }
}
